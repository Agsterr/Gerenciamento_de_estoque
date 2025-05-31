package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.MovimentacaoProduto;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.repository.MovimentacaoProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto.MovimentacaoProdutoDto;
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
    public List<MovimentacaoProdutoDto> listarDetalhadoPorMes(int ano, int mes) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicio = LocalDateTime.of(ano, mes, 1, 0, 0);
        LocalDateTime fim    = inicio.plusMonths(1);

        return movimentacaoRepository
                .findMovimentacoesPorIntervalo(inicio, fim, orgId)
                .stream()
                .map(MovimentacaoProdutoDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Busca todas as movimentações detalhadas de um ano inteiro.
     */
    public List<MovimentacaoProdutoDto> listarDetalhadoPorAno(int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicio = LocalDateTime.of(ano, 1, 1, 0, 0);
        LocalDateTime fim    = inicio.plusYears(1);

        return movimentacaoRepository
                .findMovimentacoesPorIntervalo(inicio, fim, orgId)
                .stream()
                .map(MovimentacaoProdutoDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Busca todas as movimentações de um tipo (ENTRADA/SAIDA) em um dia específico.
     */
    public List<MovimentacaoProdutoDto> buscarPorData(TipoMovimentacao tipo, LocalDate data) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim    = data.atTime(23, 59, 59);

        return movimentacaoRepository
                .findByTipoAndDataHoraBetweenAndOrgId(tipo, inicio, fim, orgId)
                .stream()
                .map(MovimentacaoProdutoDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Busca todas as movimentações de um tipo (ENTRADA/SAIDA) em um intervalo de data/hora.
     */
    public List<MovimentacaoProdutoDto> buscarPorPeriodo(TipoMovimentacao tipo, LocalDateTime inicio, LocalDateTime fim) {
        Long orgId = SecurityUtils.getCurrentOrgId();

        return movimentacaoRepository
                .findByTipoAndDataHoraBetweenAndOrgId(tipo, inicio, fim, orgId)
                .stream()
                .map(MovimentacaoProdutoDto::new)
                .collect(Collectors.toList());
    }

    // Se ainda quiser manter os métodos de total, mas transformá-los em detalhamento (lista),
    // você pode simplesmente chamar os métodos detalhados acima e, no cliente, calcular soma ou filtrar.
    // Por enquanto, como foi pedido “resposta detalhada”, removemos/ignora-se endpoints que retornavam Integer.

}
