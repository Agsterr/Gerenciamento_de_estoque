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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
        entrega.calcularValor();

        Entrega entregaSalva = entregaRepository.save(entrega);

        // Registra movimentação
        MovimentacaoProduto movimentacao = new MovimentacaoProduto();
        movimentacao.setProduto(produto);
        movimentacao.setQuantidade(entregaSalva.getQuantidade());
        movimentacao.setDataHora(LocalDateTime.now());
        movimentacao.setTipo(TipoMovimentacao.SAIDA);
        movimentacao.setOrg(produto.getOrg());
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

        // Se a quantidade mudou, ajustar estoque se necessário:
        int quantidadeAnterior = entregaExistente.getQuantidade();
        int novaQuantidade = entregaRequest.getQuantidade();
        if (novaQuantidade != quantidadeAnterior) {
            int diff = novaQuantidade - quantidadeAnterior;
            // Se diff > 0, significa que vamos retirar mais do estoque
            if (diff > 0 && produto.getQuantidade() < diff) {
                throw new IllegalArgumentException("Estoque insuficiente para a alteração da quantidade.");
            }
            produto.setQuantidade(produto.getQuantidade() - diff);
            produtoRepository.save(produto);
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

        return entregaRepository.save(entregaExistente);
    }

    // ================================
    // DELETAR ENTREGA
    // ================================
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
    public List<EntregaResponseDto> listarEntregasPorDia(LocalDate dia) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicioDia = dia.atStartOfDay();
        LocalDateTime fimDia = dia.atTime(23, 59, 59);

        return entregaRepository
                .findByHorarioEntregaBetweenAndOrgId(inicioDia, fimDia, orgId)
                .stream()
                .map(EntregaResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<EntregaResponseDto> listarEntregasPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        Long orgId = SecurityUtils.getCurrentOrgId();

        return entregaRepository
                .findByHorarioEntregaBetweenAndOrgId(inicio, fim, orgId)
                .stream()
                .map(EntregaResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<EntregaResponseDto> listarEntregasPorMes(int mes, int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicioMes = LocalDate.of(ano, mes, 1).atStartOfDay();
        LocalDateTime fimMes = inicioMes.plusMonths(1).minusSeconds(1);

        return entregaRepository
                .findByHorarioEntregaBetweenAndOrgId(inicioMes, fimMes, orgId)
                .stream()
                .map(EntregaResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<EntregaResponseDto> listarEntregasPorAno(int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicioAno = LocalDate.of(ano, 1, 1).atStartOfDay();
        LocalDateTime fimAno = inicioAno.plusYears(1).minusSeconds(1);

        return entregaRepository
                .findByHorarioEntregaBetweenAndOrgId(inicioAno, fimAno, orgId)
                .stream()
                .map(EntregaResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<EntregaResponseDto> listarEntregasPorConsumidor(Long consumidorId) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository
                .findByConsumidorIdAndOrgId(consumidorId, orgId)
                .stream()
                .map(EntregaResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<EntregaResponseDto> listarEntregasPorConsumidorPorPeriodo(
            Long consumidorId, LocalDateTime inicio, LocalDateTime fim
    ) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository
                .findByConsumidorIdAndHorarioEntregaBetweenAndOrgId(consumidorId, inicio, fim, orgId)
                .stream()
                .map(EntregaResponseDto::fromEntity)
                .collect(Collectors.toList());
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
}
