package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaRequestDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.exception.OrganizacaoNaoEncontradaException;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.*;
import br.softsistem.Gerenciamento_de_estoque.repository.*;
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

    public EntregaService(EntregaRepository entregaRepository, ConsumidorRepository consumidorRepository, ProdutoRepository produtoRepository, UsuarioRepository usuarioRepository, MovimentacaoProdutoRepository movimentacaoProdutoRepository) {
        this.entregaRepository = entregaRepository;
        this.consumidorRepository = consumidorRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimentacaoProdutoRepository = movimentacaoProdutoRepository;
    }

    public Entrega criarEntrega(EntregaRequestDto entregaRequest) {
        Long orgId = SecurityUtils.getCurrentOrgId();

        Consumidor consumidor = buscarConsumidorPorId(entregaRequest.getConsumidorId(), orgId);
        Produto produto = buscarProdutoPorId(entregaRequest.getProdutoId(), orgId);
        Usuario entregador = buscarEntregadorPorId(entregaRequest.getEntregadorId(), orgId);

        if (produto.getQuantidade() < entregaRequest.getQuantidade()) {
            throw new IllegalArgumentException("Estoque insuficiente para a entrega.");
        }

        produto.setQuantidade(produto.getQuantidade() - entregaRequest.getQuantidade());
        produtoRepository.save(produto);

        Entrega entrega = new Entrega();
        entrega.setConsumidor(consumidor);
        entrega.setProduto(produto);
        entrega.setEntregador(entregador);
        entrega.setQuantidade(entregaRequest.getQuantidade());
        entrega.setHorarioEntrega(entregaRequest.getHorarioEntrega() != null ? entregaRequest.getHorarioEntrega() : LocalDateTime.now());
        entrega.setOrg(produto.getOrg());
        entrega.calcularValor();
        Entrega entregaSalva = entregaRepository.save(entrega);

        MovimentacaoProduto movimentacao = new MovimentacaoProduto();
        movimentacao.setProduto(produto);
        movimentacao.setQuantidade(entrega.getQuantidade());
        movimentacao.setDataHora(LocalDateTime.now());
        movimentacao.setTipo(TipoMovimentacao.SAIDA);
        movimentacao.setOrg(produto.getOrg());
        movimentacaoProdutoRepository.save(movimentacao);

        return entregaSalva;
    }

    public Entrega editarEntrega(Long id, EntregaRequestDto entregaRequest) {
        Long orgId = SecurityUtils.getCurrentOrgId();

        Entrega entregaExistente = entregaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega não encontrada"));

        if (!entregaExistente.getOrg().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Entrega não pertence à organização.");
        }

        Consumidor consumidor = buscarConsumidorPorId(entregaRequest.getConsumidorId(), orgId);
        Produto produto = buscarProdutoPorId(entregaRequest.getProdutoId(), orgId);
        Usuario entregador = buscarEntregadorPorId(entregaRequest.getEntregadorId(), orgId);

        entregaExistente.setConsumidor(consumidor);
        entregaExistente.setProduto(produto);
        entregaExistente.setEntregador(entregador);
        entregaExistente.setQuantidade(entregaRequest.getQuantidade());
        entregaExistente.setHorarioEntrega(entregaRequest.getHorarioEntrega() != null ? entregaRequest.getHorarioEntrega() : LocalDateTime.now());

        entregaExistente.calcularValor();

        return entregaRepository.save(entregaExistente);
    }

    public void deletarEntrega(Long id) {
        Long orgId = SecurityUtils.getCurrentOrgId();

        Entrega entrega = entregaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega não encontrada"));

        if (!entrega.getOrg().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Você não pode excluir uma entrega de outra organização.");
        }

        entregaRepository.delete(entrega);
    }

    public BigDecimal getTotalPorDia(LocalDate dia) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalPorDia(dia, orgId);
    }

    public BigDecimal getTotalSemanal(LocalDate inicioSemana, LocalDate fimSemana) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalSemanal(inicioSemana, fimSemana, orgId);
    }

    public BigDecimal getTotalMensal(int mes, int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalMensal(mes, ano, orgId);
    }

    public BigDecimal getTotalPorConsumidor(Long consumidorId) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalPorConsumidor(consumidorId, orgId);
    }

    public BigDecimal getTotalDoMesAtual() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalDoMesAtual(orgId);
    }

    public BigDecimal getTotalSemanalPorConsumidor(Long consumidorId, LocalDate inicioSemana, LocalDate fimSemana) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalSemanalPorConsumidor(consumidorId, inicioSemana, fimSemana, orgId);
    }

    public BigDecimal getTotalAnual(int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalAnual(ano, orgId);
    }

    public BigDecimal getTotalAnualPorConsumidor(Long consumidorId, int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return entregaRepository.totalAnualPorConsumidor(consumidorId, ano, orgId);
    }

    private Consumidor buscarConsumidorPorId(Long consumidorId, Long orgId) {
        return consumidorRepository.findByIdAndOrgId(consumidorId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Consumidor não encontrado ou não pertence à organização."));
    }

    private Produto buscarProdutoPorId(Long produtoId, Long orgId) {
        return produtoRepository.findByIdAndOrgId(produtoId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado ou não pertence à organização."));
    }

    private Usuario buscarEntregadorPorId(Long entregadorId, Long orgId) {
        return usuarioRepository.findByIdAndOrgId(entregadorId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado ou não pertence à organização."));
    }

    public Page<Entrega> listarEntregas(Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }
        return entregaRepository.findByOrgId(orgId, pageable);
    }
}
