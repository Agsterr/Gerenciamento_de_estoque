package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaComAvisoResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaResponseDto;
import br.softsistem.Gerenciamento_de_estoque.exception.OrganizacaoNaoEncontradaException;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.*;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.repository.ConsumidorRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.EntregaRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.MovimentacaoProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class EntregaService {

    private final EntregaRepository entregaRepository;
    private final ConsumidorRepository consumidorRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimentacaoProdutoRepository movimentacaoProdutoRepository;

    public EntregaService(
            EntregaRepository entregaRepository,
            ConsumidorRepository consumidorRepository,
            ProdutoRepository produtoRepository,
            UsuarioRepository usuarioRepository,
            MovimentacaoProdutoRepository movimentacaoProdutoRepository
    ) {
        this.entregaRepository = entregaRepository;
        this.consumidorRepository = consumidorRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimentacaoProdutoRepository = movimentacaoProdutoRepository;
    }

    // ================================
    // CRIAR ENTREGA (com entregador automático)
    // ================================

    @CacheEvict(value = {"entregas-relatorios", "produtos"}, allEntries = true)
    public EntregaComAvisoResponseDto criarEntrega(EntregaRequestDto entregaRequest) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }

        Consumidor consumidor = consumidorRepository
                .findByIdAndOrgId(entregaRequest.getConsumidorId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Consumidor não encontrado ou não pertence à organização"));

        Produto produto = produtoRepository
                .findByIdAndOrgId(entregaRequest.getProdutoId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado ou não pertence à organização"));

        if (produto.getQuantidade() < entregaRequest.getQuantidade()) {
            throw new IllegalArgumentException("Estoque insuficiente para a entrega.");
        }

        // Ajusta estoque
        produto.setQuantidade(produto.getQuantidade() - entregaRequest.getQuantidade());
        produtoRepository.save(produto);

        // Calcula flag e mensagem de estoque baixo
        boolean estoqueBaixo = produto.isEstoqueBaixo();
        String mensagemEstoqueBaixo = estoqueBaixo
                ? String.format(
                "⚠ Estoque do produto '%s' está abaixo do mínimo! Atual: %d | Mínimo: %d",
                produto.getNome(),
                produto.getQuantidade(),
                produto.getQuantidadeMinima()
        )
                : null; // PADRONIZA: null quando não há mensagem

        Long entregadorId = SecurityUtils.getCurrentUserId();
        if (entregadorId == null) {
            throw new ResourceNotFoundException("Usuário autenticação inválida ou não encontrado");
        }
        Usuario entregador = usuarioRepository
                .findByIdAndOrgId(entregadorId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado ou não pertence à organização"));

        // Monta e salva a entidade
        Entrega entrega = entregaRequest.toEntity(produto, consumidor);
        entrega.setEntregador(entregador);
        entrega.setOrg(produto.getOrg());
        // Definir data/hora atual se não informada no request
        if (entrega.getHorarioEntrega() == null) {
            entrega.setHorarioEntrega(LocalDateTime.now());
        }
        entrega.calcularValor();

        Entrega entregaSalva = entregaRepository.save(entrega);

        // Registra movimentação
        MovimentacaoProduto movimentacao = new MovimentacaoProduto();
        movimentacao.setProduto(produto);
        movimentacao.setQuantidade(entregaSalva.getQuantidade());
        // alinhar data/hora da movimentação ao horário da entrega
        movimentacao.setDataHora(entregaSalva.getHorarioEntrega());
        movimentacao.setTipo(TipoMovimentacao.SAIDA);
        movimentacao.setOrg(produto.getOrg());
        movimentacao.setEntrega(entregaSalva);
        // enriquecer com usuário/consumidor
        movimentacao.setUsuario(entregador);
        movimentacao.setConsumidor(consumidor);
        movimentacaoProdutoRepository.save(movimentacao);

        // Retorna DTO com flag e mensagem
        return new EntregaComAvisoResponseDto(
                EntregaResponseDto.fromEntity(entregaSalva),
                estoqueBaixo,
                mensagemEstoqueBaixo
        );
    }



    // ================================
    // EDITAR ENTREGA (mantém entregador antigo)
    // ================================
    @CacheEvict(value = {"entregas-relatorios", "produtos"}, allEntries = true)
    public Entrega editarEntrega(Long id, EntregaRequestDto entregaRequest) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }

        Entrega entregaExistente = entregaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega não encontrada"));

        if (!entregaExistente.getOrg().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Entrega não pertence à organização.");
        }

        // Buscar Consumidor atualizado (pode mudar)
        Consumidor consumidor = consumidorRepository.findByIdAndOrgId(entregaRequest.getConsumidorId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Consumidor não encontrado ou não pertence à organização"));

        // Buscar Produto atualizado (pode mudar)
        Produto produto = produtoRepository.findByIdAndOrgId(entregaRequest.getProdutoId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado ou não pertence à organização"));

        // Se a quantidade mudou ou o produto mudou, ajustar estoque de forma correta
        Produto produtoAntigo = entregaExistente.getProduto();
        int quantidadeAnterior = entregaExistente.getQuantidade();
        int novaQuantidade = entregaRequest.getQuantidade();

        if (!produtoAntigo.getId().equals(produto.getId())) {
            // Produto mudou: devolve ao estoque do antigo e subtrai do novo
            produtoAntigo.setQuantidade(produtoAntigo.getQuantidade() + quantidadeAnterior);
            if (produto.getQuantidade() < novaQuantidade) {
                throw new IllegalArgumentException("Estoque insuficiente para a alteração do produto/quantidade.");
            }
            produto.setQuantidade(produto.getQuantidade() - novaQuantidade);
            produtoRepository.save(produtoAntigo);
            produtoRepository.save(produto);
        } else {
            // Mesmo produto: ajustar pela diferença
            int diff = novaQuantidade - quantidadeAnterior;
            if (diff > 0) { // retirar mais do estoque
                if (produto.getQuantidade() < diff) {
                    throw new IllegalArgumentException("Estoque insuficiente para a alteração da quantidade.");
                }
                produto.setQuantidade(produto.getQuantidade() - diff);
            } else if (diff < 0) { // devolver ao estoque
                produto.setQuantidade(produto.getQuantidade() + Math.abs(diff));
            }
            if (diff != 0) {
                produtoRepository.save(produto);
            }
        }

        entregaExistente.setConsumidor(consumidor);
        entregaExistente.setProduto(produto);
        entregaExistente.setQuantidade(novaQuantidade);
        entregaExistente.setHorarioEntrega(
                entregaRequest.getHorarioEntrega() != null
                        ? entregaRequest.getHorarioEntrega()
                        : LocalDateTime.now()
        );
        entregaExistente.calcularValor();

        Entrega entregaAtualizada = entregaRepository.save(entregaExistente);

        // Atualizar movimentação associada à entrega (se existir)
        movimentacaoProdutoRepository.findByEntregaId(entregaAtualizada.getId()).ifPresentOrElse(mov -> {
            mov.setProduto(produto);
            mov.setQuantidade(novaQuantidade);
            // alinhar com a data/hora da entrega
            mov.setDataHora(entregaAtualizada.getHorarioEntrega());
            mov.setTipo(TipoMovimentacao.SAIDA);
            mov.setOrg(produto.getOrg());
            mov.setEntrega(entregaAtualizada);
            // manter usuario existente; atualizar consumidor caso tenha mudado
            mov.setConsumidor(entregaAtualizada.getConsumidor());
            movimentacaoProdutoRepository.save(mov);
        }, () -> {
            // Caso não exista movimentação (consistência), cria uma vinculada à entrega atualizada
            MovimentacaoProduto novoMov = new MovimentacaoProduto();
            novoMov.setProduto(produto);
            novoMov.setQuantidade(novaQuantidade);
            novoMov.setDataHora(entregaAtualizada.getHorarioEntrega());
            novoMov.setTipo(TipoMovimentacao.SAIDA);
            novoMov.setOrg(produto.getOrg());
            novoMov.setEntrega(entregaAtualizada);
            novoMov.setUsuario(entregaAtualizada.getEntregador());
            novoMov.setConsumidor(entregaAtualizada.getConsumidor());
            movimentacaoProdutoRepository.save(novoMov);
        });

        return entregaAtualizada;
    }

    // ================================
    // DELETAR ENTREGA
    // ================================
    @CacheEvict(value = {"entregas-relatorios", "produtos"}, allEntries = true)
    public void deletarEntrega(Long id) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }

        Entrega entrega = entregaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega não encontrada"));

        if (!entrega.getOrg().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Você não pode excluir uma entrega de outra organização.");
        }

        // Antes de deletar, se for necessário, você pode ajustar o estoque de volta:
        Produto produto = entrega.getProduto();
        produto.setQuantidade(produto.getQuantidade() + entrega.getQuantidade());
        produtoRepository.save(produto);

        // Remover movimentação associada, se houver
        movimentacaoProdutoRepository.deleteByEntregaId(entrega.getId());

        entregaRepository.delete(entrega);
    }

    // ================================
    // LISTAR ENTREGAS COM PAGINAÇÃO
    // ================================
    public Page<Entrega> listarEntregas(Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }
        return entregaRepository.findByOrgId(orgId, pageable);
    }

    // ================================
    // MÉTODOS PARA RESPOSTAS DETALHADAS
    // ================================
    public Page<EntregaResponseDto> listarEntregasPorDia(LocalDate dia, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }

        LocalDateTime inicioDia = dia.atStartOfDay();
        LocalDateTime fimDia = dia.atTime(23, 59, 59);

        // Usando o método do repositório com paginação
        Page<Entrega> entregasPage = entregaRepository.findByHorarioEntregaBetweenAndOrgId(inicioDia, fimDia, orgId, pageable);

        // Convertendo cada entidade para DTO
        return entregasPage.map(EntregaResponseDto::fromEntity);
    }

    public Page<EntregaResponseDto> listarEntregasPorPeriodo(LocalDateTime inicio, LocalDateTime fim, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }

        // Usando o método do repositório com paginação
        Page<Entrega> entregasPage = entregaRepository.findByHorarioEntregaBetweenAndOrgId(inicio, fim, orgId, pageable);

        // Convertendo cada entidade para DTO
        return entregasPage.map(EntregaResponseDto::fromEntity);
    }


    @Cacheable(value = "entregas-relatorios", key = "'mes-' + #mes + '-' + #ano + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<EntregaResponseDto> listarEntregasPorMes(int mes, int ano, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }

        // Definindo o intervalo do mês
        LocalDateTime inicioMes = LocalDate.of(ano, mes, 1).atStartOfDay();
        LocalDateTime fimMes = inicioMes.plusMonths(1).minusSeconds(1);

        // Usando o método do repositório com paginação
        Page<Entrega> entregasPage = entregaRepository.findByHorarioEntregaBetweenAndOrgId(inicioMes, fimMes, orgId, pageable);

        // Convertendo cada entidade para DTO
        return entregasPage.map(EntregaResponseDto::fromEntity);
    }


    @Cacheable(value = "entregas-relatorios", key = "'ano-' + #ano + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<EntregaResponseDto> listarEntregasPorAno(int ano, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }

        // Definindo o intervalo do ano
        LocalDateTime inicioAno = LocalDate.of(ano, 1, 1).atStartOfDay();
        LocalDateTime fimAno = inicioAno.plusYears(1).minusSeconds(1);

        // Usando o método do repositório com paginação
        Page<Entrega> entregasPage = entregaRepository.findByHorarioEntregaBetweenAndOrgId(inicioAno, fimAno, orgId, pageable);

        // Convertendo cada entidade para DTO
        return entregasPage.map(EntregaResponseDto::fromEntity);
    }

    public Page<EntregaResponseDto> listarEntregasPorConsumidor(Long consumidorId, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }

        // Usando o método do repositório com paginação
        Page<Entrega> entregasPage = entregaRepository.findByConsumidorIdAndOrgId(consumidorId, orgId, pageable);

        // Convertendo cada entidade para DTO
        return entregasPage.map(EntregaResponseDto::fromEntity);
    }

    // Método no serviço para listar entregas por produto
    public Page<EntregaResponseDto> listarEntregasPorProduto(Long produtoId, Long orgId, Pageable pageable) {
        Page<Entrega> entregas = entregaRepository.findByProdutoIdAndOrgId(produtoId, orgId, pageable);
        return entregas.map(EntregaResponseDto::fromEntity);  // Convertendo para DTO
    }



    public Page<EntregaResponseDto> listarEntregasPorConsumidorPorPeriodo(
            Long consumidorId, LocalDateTime inicio, LocalDateTime fim, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }

        // Usando o método do repositório com paginação
        Page<Entrega> entregasPage = entregaRepository.findByConsumidorIdAndHorarioEntregaBetweenAndOrgId(consumidorId, inicio, fim, orgId, pageable);

        // Convertendo cada entidade para DTO
        return entregasPage.map(EntregaResponseDto::fromEntity);
    }


    // ================================
    // (Métodos de soma mantidos, mas não expostos)
    // ================================
    public BigDecimal getTotalPorDia(LocalDate dia) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicioDia = dia.atStartOfDay();
        LocalDateTime fimDia = dia.atTime(23, 59, 59);
        return entregaRepository.totalPorIntervalo(inicioDia, fimDia, orgId);
    }

    public BigDecimal getTotalSemanal(LocalDate inicioSemana, LocalDate fimSemana) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inSemana = inicioSemana.atStartOfDay();
        LocalDateTime fimSemanaHora = fimSemana.atTime(23, 59, 59);
        return entregaRepository.totalPorIntervalo(inSemana, fimSemanaHora, orgId);
    }

    public BigDecimal getTotalMensal(int mes, int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicioMes = LocalDate.of(ano, mes, 1).atStartOfDay();
        LocalDateTime fimMes = inicioMes.plusMonths(1).minusSeconds(1);
        return entregaRepository.totalPorIntervalo(inicioMes, fimMes, orgId);
    }

    public BigDecimal getTotalPorConsumidor(Long consumidorId) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicio = LocalDate.of(1900, 1, 1).atStartOfDay();
        LocalDateTime fim = LocalDate.of(3000, 1, 1).atStartOfDay().minusSeconds(1);
        return entregaRepository.totalPorIntervaloPorConsumidor(consumidorId, inicio, fim, orgId);
    }

    @Cacheable(value = "entregas-relatorios", key = "'total-mes-atual-' + #root.target.getCurrentOrgId() + '-' + T(java.time.LocalDate).now().getMonthValue() + '-' + T(java.time.LocalDate).now().getYear()")
    public BigDecimal getTotalDoMesAtual() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioMesAtual = hoje.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMesAtual = inicioMesAtual.plusMonths(1).minusSeconds(1);
        return entregaRepository.totalPorIntervalo(inicioMesAtual, fimMesAtual, orgId);
    }

    public BigDecimal getTotalSemanalPorConsumidor(Long consumidorId, LocalDate inicioSemana, LocalDate fimSemana) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inSemana = inicioSemana.atStartOfDay();
        LocalDateTime fimSemanaHora = fimSemana.atTime(23, 59, 59);
        return entregaRepository.totalPorIntervaloPorConsumidor(consumidorId, inSemana, fimSemanaHora, orgId);
    }

    public BigDecimal getTotalAnual(int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicioAno = LocalDate.of(ano, 1, 1).atStartOfDay();
        LocalDateTime fimAno = inicioAno.plusYears(1).minusSeconds(1);
        return entregaRepository.totalPorIntervalo(inicioAno, fimAno, orgId);
    }

    public BigDecimal getTotalAnualPorConsumidor(Long consumidorId, int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicioAno = LocalDate.of(ano, 1, 1).atStartOfDay();
        LocalDateTime fimAno = inicioAno.plusYears(1).minusSeconds(1);
        return entregaRepository.totalPorIntervaloPorConsumidor(consumidorId, inicioAno, fimAno, orgId);
    }

    // ================================
    // MÉTODOS AUXILIARES
    // ================================
    private Consumidor buscarConsumidorPorId(Long consumidorId, Long orgId) {
        return consumidorRepository.findByIdAndOrgId(consumidorId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Consumidor não encontrado ou não pertence à organização."));
    }

    private Produto buscarProdutoPorId(Long produtoId, Long orgId) {
        return produtoRepository.findByIdAndOrgId(produtoId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado ou não pertence à organização."));
    }



    /**
     * Retorna o total de entregas realizadas pela organização.
     *
     * @return número total de entregas
     */
    public Integer getTotalEntregasRealizadas() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }
        return entregaRepository.totalEntregasRealizadas(orgId);
    }

    /**
     * Retorna o total de entregas realizadas por um consumidor na organização.
     *
     * @param consumidorId ID do consumidor
     * @return número de entregas do consumidor
     */
    public Integer getTotalEntregasPorConsumidor(Long consumidorId) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }
        return entregaRepository.totalEntregasPorConsumidor(consumidorId, orgId);
    }

    /**
     * Retorna o total de entregas realizadas com um produto específico na organização.
     *
     * @param produtoId ID do produto
     * @return número de entregas do produto
     */
    public Integer getTotalEntregasPorProduto(Long produtoId) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }
        return entregaRepository.totalEntregasPorProduto(produtoId, orgId);
    }
}
