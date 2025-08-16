package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.MovimentacaoProduto;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import br.softsistem.Gerenciamento_de_estoque.repository.MovimentacaoProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.EntregaRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ConsumidorRepository;
import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto.MovimentacaoProdutoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovimentacaoProdutoService {

    private final MovimentacaoProdutoRepository movimentacaoRepository;
    private final ProdutoRepository produtoRepository;
    private final EntregaRepository entregaRepository;
    private final ConsumidorRepository consumidorRepository;

    public MovimentacaoProdutoService(MovimentacaoProdutoRepository movimentacaoRepository,
                                      ProdutoRepository produtoRepository,
                                      EntregaRepository entregaRepository,
                                      ConsumidorRepository consumidorRepository) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.produtoRepository = produtoRepository;
        this.entregaRepository = entregaRepository;
        this.consumidorRepository = consumidorRepository;
    }

    /**
     * Registra uma movimentação (entrada ou saída). Retorna o DTO correspondente.
     */
    public MovimentacaoProdutoDto registrarMovimentacao(MovimentacaoProdutoDto dto) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        // Validação: SAIDA precisa de consumidor ou entrega
        if (dto.getTipo() == TipoMovimentacao.SAIDA && dto.getConsumidorId() == null && dto.getEntregaId() == null) {
            throw new IllegalArgumentException("Para movimentação de SAIDA informe consumidorId ou entregaId.");
        }
        Produto produto = produtoRepository.findByIdAndOrgId(dto.getProdutoId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado ou não pertence à organização"));
        MovimentacaoProduto mov = new MovimentacaoProduto();
        mov.setProduto(produto);
        mov.setQuantidade(dto.getQuantidade());
        mov.setTipo(dto.getTipo());
        mov.setDataHora(LocalDateTime.now());
        mov.setOrg(produto.getOrg());
        if (dto.getEntregaId() != null) {
            entregaRepository.findById(dto.getEntregaId())
                    .filter(e -> e.getOrg().getId().equals(orgId))
                    .ifPresent(mov::setEntrega);
        }
        if (dto.getConsumidorId() != null) {
            Consumidor cons = consumidorRepository.findByIdAndOrgId(dto.getConsumidorId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Consumidor não encontrado ou não pertence à organização"));
            mov.setConsumidor(cons);
        }
        // Atualizar estoque
        if (dto.getTipo() == TipoMovimentacao.ENTRADA) {
            produto.setQuantidade(produto.getQuantidade() + dto.getQuantidade());
        } else if (dto.getTipo() == TipoMovimentacao.SAIDA) {
            if (produto.getQuantidade() < dto.getQuantidade()) {
                throw new IllegalArgumentException("Quantidade de saída maior que o estoque atual");
            }
            produto.setQuantidade(produto.getQuantidade() - dto.getQuantidade());
        }

        produtoRepository.save(produto);
        MovimentacaoProduto salvo = movimentacaoRepository.save(mov);
        return new MovimentacaoProdutoDto(salvo);
    }

    /**
     * Edita uma movimentação existente e ajusta o estoque conforme necessário.
     * @param movimentacaoId ID da movimentação a ser editada
     * @param dto dados atualizados da movimentação
     * @return DTO da movimentação atualizada
     */
    public MovimentacaoProdutoDto editarMovimentacao(Long movimentacaoId, MovimentacaoProdutoDto dto) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (dto.getTipo() == TipoMovimentacao.SAIDA && dto.getConsumidorId() == null && dto.getEntregaId() == null) {
            throw new IllegalArgumentException("Para movimentação de SAIDA informe consumidorId ou entregaId.");
        }
        MovimentacaoProduto movimentacaoExistente = movimentacaoRepository.findById(movimentacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentação não encontrada"));
        if (!movimentacaoExistente.getOrg().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Movimentação não pertence à organização");
        }
        Produto produtoNovo = produtoRepository.findByIdAndOrgId(dto.getProdutoId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado ou não pertence à organização"));
        Produto produtoAntigo = movimentacaoExistente.getProduto();
        if (movimentacaoExistente.getTipo() == TipoMovimentacao.ENTRADA) {
            produtoAntigo.setQuantidade(produtoAntigo.getQuantidade() - movimentacaoExistente.getQuantidade());
        } else if (movimentacaoExistente.getTipo() == TipoMovimentacao.SAIDA) {
            produtoAntigo.setQuantidade(produtoAntigo.getQuantidade() + movimentacaoExistente.getQuantidade());
        }
        if (dto.getTipo() == TipoMovimentacao.ENTRADA) {
            produtoNovo.setQuantidade(produtoNovo.getQuantidade() + dto.getQuantidade());
        } else if (dto.getTipo() == TipoMovimentacao.SAIDA) {
            if (produtoNovo.getQuantidade() < dto.getQuantidade()) {
                throw new IllegalArgumentException("Quantidade de saída maior que o estoque atual");
            }
            produtoNovo.setQuantidade(produtoNovo.getQuantidade() - dto.getQuantidade());
        }
        movimentacaoExistente.setProduto(produtoNovo);
        movimentacaoExistente.setQuantidade(dto.getQuantidade());
        movimentacaoExistente.setTipo(dto.getTipo());
        movimentacaoExistente.setDataHora(dto.getDataHora() != null ? dto.getDataHora() : LocalDateTime.now());
        movimentacaoExistente.setOrg(produtoNovo.getOrg());
        if (dto.getConsumidorId() != null) {
            Consumidor cons = consumidorRepository.findByIdAndOrgId(dto.getConsumidorId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Consumidor não encontrado ou não pertence à organização"));
            movimentacaoExistente.setConsumidor(cons);
        }
        if (!produtoAntigo.getId().equals(produtoNovo.getId())) {
            produtoRepository.save(produtoAntigo);
        }
        produtoRepository.save(produtoNovo);
        MovimentacaoProduto salvo = movimentacaoRepository.save(movimentacaoExistente);
        if (salvo.getEntrega() != null) {
            Entrega entregaVinculada = salvo.getEntrega();
            entregaVinculada.setProduto(produtoNovo);
            entregaVinculada.setQuantidade(dto.getQuantidade());
            entregaVinculada.calcularValor();
            entregaRepository.save(entregaVinculada);
        }
        return new MovimentacaoProdutoDto(salvo);
    }

    /**
     * Edita uma movimentação existente sem alterar o estoque dos produtos.
     * @param movimentacaoId ID da movimentação a ser editada
     * @param novaQuantidade nova quantidade da movimentação
     * @return DTO da movimentação atualizada
     */
    public MovimentacaoProdutoDto editarMovimentacaoSemAjustarEstoque(Long movimentacaoId, Integer novaQuantidade) {
        Long orgId = SecurityUtils.getCurrentOrgId();

        // Buscar movimentação existente
        MovimentacaoProduto mov = movimentacaoRepository.findById(movimentacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentação não encontrada"));

        // Verificar se pertence à organização
        if (!mov.getOrg().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Movimentação não pertence à organização");
        }

        // Atualizar apenas os campos necessários
        mov.setQuantidade(novaQuantidade);
        mov.setDataHora(LocalDateTime.now());

        MovimentacaoProduto salvo = movimentacaoRepository.save(mov);
        return new MovimentacaoProdutoDto(salvo);
    }

    /**
     * Cria uma nova movimentação sem afetar o estoque (para casos específicos como edição de produtos).
     * @param produtoId ID do produto
     * @param quantidade quantidade da movimentação
     * @param tipo tipo da movimentação (ENTRADA/SAIDA)
     * @param dataHora data/hora da movimentação (opcional, usa atual se null)
     * @return DTO da movimentação criada
     */
    public MovimentacaoProdutoDto criarMovimentacaoSemAfetarEstoque(Long produtoId, Integer quantidade, TipoMovimentacao tipo, LocalDateTime dataHora) {
        Long orgId = SecurityUtils.getCurrentOrgId();

        Produto produto = produtoRepository.findByIdAndOrgId(produtoId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado ou não pertence à organização"));

        MovimentacaoProduto mov = new MovimentacaoProduto();
        mov.setProduto(produto);
        mov.setQuantidade(quantidade);
        mov.setTipo(tipo);
        mov.setDataHora(dataHora != null ? dataHora : LocalDateTime.now());
        mov.setOrg(produto.getOrg());

        MovimentacaoProduto salvo = movimentacaoRepository.save(mov);
        return new MovimentacaoProdutoDto(salvo);
    }

    /**
     * Busca todas as movimentações detalhadas de um mês e ano.
     */
    public Page<MovimentacaoProdutoDto> listarDetalhadoPorMes(int ano, int mes, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicio = LocalDateTime.of(ano, mes, 1, 0, 0);
        LocalDateTime fim = inicio.plusMonths(1);
        return movimentacaoRepository
                .findMovimentacoesPorIntervalo(inicio, fim, orgId, pageable)
                .map(MovimentacaoProdutoDto::new);
    }

    public Page<MovimentacaoProdutoDto> listarDetalhadoPorAno(int ano, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicio = LocalDateTime.of(ano, 1, 1, 0, 0);
        LocalDateTime fim = inicio.plusYears(1);
        return movimentacaoRepository
                .findMovimentacoesPorIntervalo(inicio, fim, orgId, pageable)
                .map(MovimentacaoProdutoDto::new);
    }

    public Page<MovimentacaoProdutoDto> buscarPorData(TipoMovimentacao tipo, LocalDate data, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.atTime(23, 59, 59);
        return movimentacaoRepository
                .findByTipoAndDataHoraBetweenAndOrgId(tipo, inicio, fim, orgId, pageable)
                .map(MovimentacaoProdutoDto::new);
    }

    public Page<MovimentacaoProdutoDto> buscarPorPeriodo(TipoMovimentacao tipo, LocalDateTime inicio, LocalDateTime fim, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository
                .findByTipoAndDataHoraBetweenAndOrgId(tipo, inicio, fim, orgId, pageable)
                .map(MovimentacaoProdutoDto::new);
    }

    public Page<MovimentacaoProdutoDto> buscarPorNomeProduto(String nomeProduto, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository
                .findByProdutoNomeAndOrgId(nomeProduto, orgId, pageable)
                .map(MovimentacaoProdutoDto::new);
    }

    public Page<MovimentacaoProdutoDto> buscarPorCategoriaProduto(String categoriaProduto, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository
                .findByProdutoCategoriaAndOrgId(categoriaProduto, orgId, pageable)
                .map(MovimentacaoProdutoDto::new);
    }

    public Page<MovimentacaoProdutoDto> buscarPorIdProduto(Long produtoId, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository
                .findByProdutoIdAndOrgId(produtoId, orgId, pageable)
                .map(MovimentacaoProdutoDto::new);
    }

    public Page<MovimentacaoProdutoDto> buscarPorProdutoNomeCategoriaIdAndIntervalo(String nome, String categoria, Long produtoId, LocalDateTime inicio, LocalDateTime fim, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository
                .findByProdutoNomeCategoriaIdAndIntervalo(nome, categoria, produtoId, inicio, fim, orgId, pageable)
                .map(MovimentacaoProdutoDto::new);
    }

    /**
     * Busca todas as movimentações de um produto pelo ID e pela organização.
     */
    public List<MovimentacaoProdutoDto> buscarPorIdProduto(Long produtoId) {
        Long orgId = SecurityUtils.getCurrentOrgId();

        // Usar Pageable.unpaged() para obter todos os resultados sem paginação
        return movimentacaoRepository
                .findByProdutoIdAndOrgId(produtoId, orgId, Pageable.unpaged())
                .stream()
                .map(MovimentacaoProdutoDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Busca todas as movimentações de uma organização, por produto, nome ou categoria, e por intervalo de datas.
     */
    public List<MovimentacaoProdutoDto> buscarPorProdutoNomeCategoriaIdAndIntervalo(String nome, String categoria, Long produtoId, LocalDateTime inicio, LocalDateTime fim) {
        Long orgId = SecurityUtils.getCurrentOrgId();

        // Usar Pageable.unpaged() para obter todos os resultados sem paginação
        return movimentacaoRepository
                .findByProdutoNomeCategoriaIdAndIntervalo(nome, categoria, produtoId, inicio, fim, orgId, Pageable.unpaged())
                .stream()
                .map(MovimentacaoProdutoDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Busca todas as movimentações de tipos (ENTRADA e SAIDA) para uma organização, com suporte à paginação.
     */
    public Page<MovimentacaoProdutoDto> buscarPorTipos(List<TipoMovimentacao> tipos, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository
                .findByTipoInAndOrgId(tipos, orgId, pageable)
                .map(MovimentacaoProdutoDto::new);
    }

    /**
     * Busca todas as movimentações por nome de consumidor na organização atual.
     * @param nomeConsumidor nome do consumidor (pode ser parcial)
     * @param pageable informações de paginação
     * @return página de movimentações do consumidor
     */
    public Page<MovimentacaoProdutoDto> buscarPorNomeConsumidor(String nomeConsumidor, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();

        // Primeiro buscamos todas as movimentações da organização
        List<MovimentacaoProdutoDto> todasMovimentacoes = movimentacaoRepository
                .findByOrgId(orgId, Pageable.unpaged()) // Buscamos todas sem paginação
                .stream()
                .map(MovimentacaoProdutoDto::new)
                .filter(dto -> dto.getNomeConsumidor() != null &&
                        dto.getNomeConsumidor().toLowerCase().contains(nomeConsumidor.toLowerCase()))
                .collect(Collectors.toList());

        // Implementamos a paginação manualmente
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), todasMovimentacoes.size());

        // Verificamos se o índice inicial é válido
        if (start > todasMovimentacoes.size()) {
            return Page.empty(pageable);
        }

        // Criamos uma sublista com os itens da página atual
        List<MovimentacaoProdutoDto> itensDaPagina = todasMovimentacoes.subList(start, end);

        // Retornamos um Page com os itens filtrados e paginados
        return new org.springframework.data.domain.PageImpl<>(
                itensDaPagina,
                pageable,
                todasMovimentacoes.size()
        );
    }

    /**
     * Atualiza apenas o consumidor vinculado à movimentação.
     * @param movimentacaoId ID da movimentação a ser atualizada
     * @param consumidorId ID do novo consumidor
     * @return DTO da movimentação atualizada
     */
    public MovimentacaoProdutoDto atualizarConsumidor(Long movimentacaoId, Long consumidorId) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        MovimentacaoProduto mov = movimentacaoRepository.findById(movimentacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentação não encontrada"));
        if (!mov.getOrg().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Movimentação não pertence à organização");
        }
        Consumidor cons = consumidorRepository.findByIdAndOrgId(consumidorId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Consumidor não encontrado ou não pertence à organização"));
        mov.setConsumidor(cons);
        mov.setDataHora(LocalDateTime.now());
        MovimentacaoProduto salvo = movimentacaoRepository.save(mov);
        return new MovimentacaoProdutoDto(salvo);
    }

}
