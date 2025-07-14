package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.MovimentacaoProduto;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.repository.MovimentacaoProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
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

    public MovimentacaoProdutoService(MovimentacaoProdutoRepository movimentacaoRepository,
                                      ProdutoRepository produtoRepository) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.produtoRepository = produtoRepository;
    }

    /**
     * Registra uma movimentação (entrada ou saída). Retorna o DTO correspondente.
     */
    public MovimentacaoProdutoDto registrarMovimentacao(MovimentacaoProdutoDto dto) {
        Long orgId = SecurityUtils.getCurrentOrgId();

        Produto produto = produtoRepository.findByIdAndOrgId(dto.getProdutoId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado ou não pertence à organização"));

        MovimentacaoProduto mov = new MovimentacaoProduto();
        mov.setProduto(produto);
        mov.setQuantidade(dto.getQuantidade());
        mov.setTipo(dto.getTipo());
        mov.setDataHora(LocalDateTime.now());
        mov.setOrg(produto.getOrg());

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

}
