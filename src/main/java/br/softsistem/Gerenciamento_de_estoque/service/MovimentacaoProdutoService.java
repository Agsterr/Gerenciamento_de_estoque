package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.*;
import br.softsistem.Gerenciamento_de_estoque.repository.MovimentacaoProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto.MovimentacaoProdutoDto;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimentacaoProdutoService {

    private final MovimentacaoProdutoRepository movimentacaoRepository;
    private final ProdutoRepository produtoRepository;

    public MovimentacaoProdutoService(MovimentacaoProdutoRepository movimentacaoRepository,
                                      ProdutoRepository produtoRepository) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.produtoRepository = produtoRepository;
    }

    // Registrar movimentação
    public MovimentacaoProduto registrarMovimentacao(MovimentacaoProdutoDto dto) {
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
        return movimentacaoRepository.save(mov);
    }

    public List<MovimentacaoProduto> listarPorProduto(Long produtoId) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository.findByProdutoIdAndOrgId(produtoId, orgId);
    }

    public List<MovimentacaoProduto> listarPorDia(LocalDate dia) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository.findByDataHoraBetweenAndOrgId(
                dia.atStartOfDay(),
                dia.atTime(23, 59, 59),
                orgId);
    }

    public List<MovimentacaoProduto> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository.findByDataHoraBetweenAndOrgId(
                inicio.atStartOfDay(),
                fim.atTime(23, 59, 59),
                orgId);
    }

    public List<MovimentacaoProduto> listarPorAno(int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository.findByAnoAndOrgId(ano, orgId);
    }

    public List<MovimentacaoProduto> listarPorMes(int ano, int mes) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository.findByAnoAndMesAndOrgId(ano, mes, orgId);
    }

    public List<MovimentacaoProdutoDto> buscarPorData(TipoMovimentacao tipo, LocalDate data) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.atTime(23, 59, 59);
        return movimentacaoRepository.findByTipoAndDataHoraBetweenAndOrgId(tipo, inicio, fim, orgId)
                .stream()
                .map(MovimentacaoProdutoDto::new)
                .toList();
    }

    public List<MovimentacaoProdutoDto> buscarPorPeriodo(TipoMovimentacao tipo, LocalDateTime inicio, LocalDateTime fim) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository.findByTipoAndDataHoraBetweenAndOrgId(tipo, inicio, fim, orgId)
                .stream()
                .map(MovimentacaoProdutoDto::new)
                .toList();
    }

    public Integer totalPorAno(TipoMovimentacao tipo, int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository.totalPorAno(tipo, ano, orgId);
    }

    public Integer totalPorMes(TipoMovimentacao tipo, int ano, int mes) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return movimentacaoRepository.totalPorMes(tipo, ano, mes, orgId);
    }

}
