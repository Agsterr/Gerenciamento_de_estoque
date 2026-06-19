package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.PedidoCompraItemRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.PedidoCompraRequest;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.AcaoAuditoria;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusPedidoCompra;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.*;
import br.softsistem.Gerenciamento_de_estoque.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoCompraService {

    private final PedidoCompraRepository pedidoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final DepositoRepository depositoRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueDepositoRepository estoqueDepositoRepository;
    private final MovimentacaoProdutoRepository movimentacaoRepository;
    private final OrgRepository orgRepository;
    private final AuditoriaService auditoriaService;

    public PedidoCompraService(PedidoCompraRepository pedidoRepository,
                               FornecedorRepository fornecedorRepository,
                               DepositoRepository depositoRepository,
                               ProdutoRepository produtoRepository,
                               EstoqueDepositoRepository estoqueDepositoRepository,
                               MovimentacaoProdutoRepository movimentacaoRepository,
                               OrgRepository orgRepository,
                               AuditoriaService auditoriaService) {
        this.pedidoRepository = pedidoRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.depositoRepository = depositoRepository;
        this.produtoRepository = produtoRepository;
        this.estoqueDepositoRepository = estoqueDepositoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.orgRepository = orgRepository;
        this.auditoriaService = auditoriaService;
    }

    private Long requireOrgId() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) throw new ResourceNotFoundException("Organização não encontrada");
        return orgId;
    }

    public Page<PedidoCompra> listar(Pageable pageable) {
        return pedidoRepository.findByOrgIdOrderByCriadoEmDesc(requireOrgId(), pageable);
    }

    public PedidoCompra buscarPorId(Long id) {
        return pedidoRepository.findByIdAndOrgId(id, requireOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de compra não encontrado"));
    }

    @Transactional
    public PedidoCompra criar(PedidoCompraRequest req) {
        Long orgId = requireOrgId();
        Org org = orgRepository.findById(orgId).orElseThrow();
        Fornecedor fornecedor = fornecedorRepository.findByIdAndOrgId(req.fornecedorId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado"));

        Deposito deposito = null;
        if (req.depositoId() != null) {
            deposito = depositoRepository.findByIdAndOrgId(req.depositoId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Depósito não encontrado"));
        }

        PedidoCompra pedido = new PedidoCompra();
        pedido.setNumero(gerarNumero(orgId));
        pedido.setFornecedor(fornecedor);
        pedido.setDeposito(deposito);
        pedido.setObservacao(req.observacao());
        pedido.setOrg(org);
        pedido.setStatus(StatusPedidoCompra.RASCUNHO);

        List<PedidoCompraItem> itens = montarItens(pedido, req.itens(), orgId);
        pedido.setItens(itens);
        pedido.setValorTotal(calcularTotal(itens));

        PedidoCompra salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("PedidoCompra", salvo.getId(), AcaoAuditoria.CREATE,
                "Pedido " + salvo.getNumero() + " criado");
        return salvo;
    }

    @Transactional
    public PedidoCompra receber(Long id) {
        PedidoCompra pedido = buscarPorId(id);
        if (pedido.getStatus() == StatusPedidoCompra.RECEBIDO) {
            throw new IllegalStateException("Pedido já foi recebido");
        }
        if (pedido.getStatus() == StatusPedidoCompra.CANCELADO) {
            throw new IllegalStateException("Pedido cancelado não pode ser recebido");
        }

        Long orgId = pedido.getOrg().getId();
        Deposito depositoDestino = pedido.getDeposito();
        if (depositoDestino == null) {
            depositoDestino = depositoRepository.findByOrgIdAndPadraoTrue(orgId)
                    .orElseThrow(() -> new IllegalStateException("Nenhum depósito padrão configurado"));
        }
        final Deposito deposito = depositoDestino;

        for (PedidoCompraItem item : pedido.getItens()) {
            Produto produto = item.getProduto();
            int qtd = item.getQuantidade();
            BigDecimal custoEntrada = item.getPrecoUnitario();

            atualizarCustoMedio(produto, qtd, custoEntrada);
            produto.setQuantidade(produto.getQuantidade() + qtd);
            produtoRepository.save(produto);

            EstoqueDeposito estoque = estoqueDepositoRepository
                    .findByProdutoIdAndDepositoId(produto.getId(), deposito.getId())
                    .orElseGet(() -> {
                        EstoqueDeposito novo = new EstoqueDeposito();
                        novo.setProduto(produto);
                        novo.setDeposito(deposito);
                        novo.setOrg(pedido.getOrg());
                        novo.setQuantidade(0);
                        return novo;
                    });
            estoque.setQuantidade(estoque.getQuantidade() + qtd);
            estoqueDepositoRepository.save(estoque);

            MovimentacaoProduto mov = new MovimentacaoProduto();
            mov.setProduto(produto);
            mov.setQuantidade(qtd);
            mov.setTipo(TipoMovimentacao.ENTRADA);
            mov.setDataHora(LocalDateTime.now());
            mov.setOrg(pedido.getOrg());
            movimentacaoRepository.save(mov);
        }

        pedido.setStatus(StatusPedidoCompra.RECEBIDO);
        pedido.setAtualizadoEm(LocalDateTime.now());
        PedidoCompra salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("PedidoCompra", salvo.getId(), AcaoAuditoria.RECEBER_PEDIDO,
                "Pedido " + salvo.getNumero() + " recebido no depósito " + deposito.getNome());
        return salvo;
    }

    @Transactional
    public PedidoCompra cancelar(Long id) {
        PedidoCompra pedido = buscarPorId(id);
        if (pedido.getStatus() == StatusPedidoCompra.RECEBIDO) {
            throw new IllegalStateException("Pedido recebido não pode ser cancelado");
        }
        pedido.setStatus(StatusPedidoCompra.CANCELADO);
        pedido.setAtualizadoEm(LocalDateTime.now());
        PedidoCompra salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("PedidoCompra", salvo.getId(), AcaoAuditoria.UPDATE,
                "Pedido " + salvo.getNumero() + " cancelado");
        return salvo;
    }

    private void atualizarCustoMedio(Produto produto, int qtdEntrada, BigDecimal custoEntrada) {
        int qtdAtual = produto.getQuantidade() != null ? produto.getQuantidade() : 0;
        BigDecimal custoAtual = produto.getCustoMedio() != null ? produto.getCustoMedio() : BigDecimal.ZERO;
        int qtdTotal = qtdAtual + qtdEntrada;
        if (qtdTotal == 0) {
            produto.setCustoMedio(custoEntrada);
            return;
        }
        BigDecimal valorAtual = custoAtual.multiply(BigDecimal.valueOf(qtdAtual));
        BigDecimal valorEntrada = custoEntrada.multiply(BigDecimal.valueOf(qtdEntrada));
        BigDecimal novoCusto = valorAtual.add(valorEntrada)
                .divide(BigDecimal.valueOf(qtdTotal), 2, RoundingMode.HALF_UP);
        produto.setCustoMedio(novoCusto);
    }

    private List<PedidoCompraItem> montarItens(PedidoCompra pedido, List<PedidoCompraItemRequest> reqs, Long orgId) {
        if (reqs == null || reqs.isEmpty()) {
            throw new IllegalArgumentException("Pedido deve ter ao menos um item");
        }
        List<PedidoCompraItem> itens = new ArrayList<>();
        for (PedidoCompraItemRequest r : reqs) {
            Produto produto = produtoRepository.findByIdAndOrgId(r.produtoId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + r.produtoId()));
            PedidoCompraItem item = new PedidoCompraItem();
            item.setPedido(pedido);
            item.setProduto(produto);
            item.setQuantidade(r.quantidade());
            item.setPrecoUnitario(r.precoUnitario());
            item.setSubtotal(r.precoUnitario().multiply(BigDecimal.valueOf(r.quantidade())));
            itens.add(item);
        }
        return itens;
    }

    private BigDecimal calcularTotal(List<PedidoCompraItem> itens) {
        return itens.stream().map(PedidoCompraItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String gerarNumero(Long orgId) {
        long seq = pedidoRepository.countByOrgId(orgId) + 1;
        String numero = "PC-" + orgId + "-" + String.format("%05d", seq);
        while (pedidoRepository.existsByNumeroAndOrgId(numero, orgId)) {
            seq++;
            numero = "PC-" + orgId + "-" + String.format("%05d", seq);
        }
        return numero;
    }
}
