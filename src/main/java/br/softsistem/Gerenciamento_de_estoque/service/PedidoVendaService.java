package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.PedidoVendaItemRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.PedidoVendaRequest;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.AcaoAuditoria;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusPedidoVenda;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoPedidoVenda;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.*;
import br.softsistem.Gerenciamento_de_estoque.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoVendaService {

    private final PedidoVendaRepository pedidoRepository;
    private final ConsumidorRepository consumidorRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;
    private final MovimentacaoProdutoRepository movimentacaoRepository;
    private final OrgRepository orgRepository;
    private final EstoqueDepositoService estoqueDepositoService;
    private final AuditoriaService auditoriaService;

    public PedidoVendaService(PedidoVendaRepository pedidoRepository,
                              ConsumidorRepository consumidorRepository,
                              UsuarioRepository usuarioRepository,
                              ProdutoRepository produtoRepository,
                              MovimentacaoProdutoRepository movimentacaoRepository,
                              OrgRepository orgRepository,
                              EstoqueDepositoService estoqueDepositoService,
                              AuditoriaService auditoriaService) {
        this.pedidoRepository = pedidoRepository;
        this.consumidorRepository = consumidorRepository;
        this.usuarioRepository = usuarioRepository;
        this.produtoRepository = produtoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.orgRepository = orgRepository;
        this.estoqueDepositoService = estoqueDepositoService;
        this.auditoriaService = auditoriaService;
    }

    private Long requireOrgId() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) throw new ResourceNotFoundException("Organização não encontrada");
        return orgId;
    }

    public Page<PedidoVenda> listar(Pageable pageable) {
        return pedidoRepository.findByOrgIdOrderByDataHoraDesc(requireOrgId(), pageable);
    }

    public PedidoVenda buscarPorId(Long id) {
        return pedidoRepository.findByIdAndOrgId(id, requireOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de venda não encontrado"));
    }

    @Transactional
    public PedidoVenda criar(PedidoVendaRequest req) {
        Long orgId = requireOrgId();
        Org org = orgRepository.findById(orgId).orElseThrow();
        TipoPedidoVenda tipo = req.tipoPedido() != null ? req.tipoPedido() : TipoPedidoVenda.VENDA;

        Consumidor consumidor = null;
        Usuario funcionario = null;
        if (tipo == TipoPedidoVenda.INTERNO) {
            if (req.funcionarioId() == null) {
                throw new IllegalArgumentException("Selecione o funcionário para retirada interna.");
            }
            funcionario = usuarioRepository.findByIdAndOrgId(req.funcionarioId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));
        } else {
            if (req.consumidorId() == null) {
                throw new IllegalArgumentException("Selecione o cliente.");
            }
            if (req.formaPagamento() == null || req.condicaoPagamento() == null) {
                throw new IllegalArgumentException("Informe forma e condição de pagamento.");
            }
            consumidor = consumidorRepository.findByIdAndOrgId(req.consumidorId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        }

        Long vendedorId = req.vendedorId() != null ? req.vendedorId() : SecurityUtils.getCurrentUserId();
        if (vendedorId == null) {
            throw new ResourceNotFoundException("Vendedor não identificado");
        }
        Usuario vendedor = usuarioRepository.findByIdAndOrgId(vendedorId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor não encontrado"));

        PedidoVenda pedido = new PedidoVenda();
        pedido.setNumero(gerarNumero(orgId, tipo));
        pedido.setTipoPedido(tipo);
        pedido.setConsumidor(consumidor);
        pedido.setFuncionario(funcionario);
        pedido.setVendedor(vendedor);
        pedido.setDataHora(req.dataHora() != null ? req.dataHora() : LocalDateTime.now());
        if (tipo == TipoPedidoVenda.VENDA) {
            pedido.setFormaPagamento(req.formaPagamento());
            pedido.setCondicaoPagamento(req.condicaoPagamento());
        }
        pedido.setObservacao(req.observacao());
        pedido.setOrg(org);
        pedido.setStatus(StatusPedidoVenda.RASCUNHO);

        List<PedidoVendaItem> itens = montarItens(pedido, req.itens(), orgId, tipo);
        pedido.setItens(itens);
        pedido.setValorTotal(calcularTotal(itens));

        PedidoVenda salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("PedidoVenda", salvo.getId(), AcaoAuditoria.CREATE,
                (tipo == TipoPedidoVenda.INTERNO ? "Retirada interna " : "Pedido ") + salvo.getNumero() + " criado");

        if (Boolean.TRUE.equals(req.confirmar())) {
            return confirmar(salvo.getId());
        }
        return salvo;
    }

    @Transactional
    public PedidoVenda confirmar(Long id) {
        PedidoVenda pedido = buscarPorId(id);
        if (pedido.getStatus() == StatusPedidoVenda.CONFIRMADO) {
            throw new IllegalStateException("Pedido já confirmado");
        }
        if (pedido.getStatus() == StatusPedidoVenda.CANCELADO) {
            throw new IllegalStateException("Pedido cancelado não pode ser confirmado");
        }

        for (PedidoVendaItem item : pedido.getItens()) {
            Produto produto = item.getProduto();
            int qtd = item.getQuantidade();
            if (produto.getQuantidade() < qtd) {
                throw new IllegalArgumentException(
                        "Estoque insuficiente para '" + produto.getNome() + "'. Disponível: " + produto.getQuantidade());
            }
            produto.setQuantidade(produto.getQuantidade() - qtd);
            produtoRepository.save(produto);
            estoqueDepositoService.ajustarNoDepositoPadrao(produto, pedido.getOrg(), -qtd);

            MovimentacaoProduto mov = new MovimentacaoProduto();
            mov.setProduto(produto);
            mov.setQuantidade(qtd);
            mov.setTipo(TipoMovimentacao.SAIDA);
            mov.setDataHora(pedido.getDataHora());
            mov.setOrg(pedido.getOrg());
            mov.setPedidoVenda(pedido);
            if (pedido.getConsumidor() != null) {
                mov.setConsumidor(pedido.getConsumidor());
            }
            mov.setUsuario(pedido.isInterno() && pedido.getFuncionario() != null
                    ? pedido.getFuncionario() : pedido.getVendedor());
            movimentacaoRepository.save(mov);
        }

        pedido.setStatus(StatusPedidoVenda.CONFIRMADO);
        pedido.setAtualizadoEm(LocalDateTime.now());
        PedidoVenda salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("PedidoVenda", salvo.getId(), AcaoAuditoria.UPDATE,
                "Pedido " + salvo.getNumero() + " confirmado — saída de estoque");
        return salvo;
    }

    @Transactional
    public PedidoVenda cancelar(Long id) {
        PedidoVenda pedido = buscarPorId(id);
        if (pedido.getStatus() == StatusPedidoVenda.CANCELADO) {
            throw new IllegalStateException("Pedido já cancelado");
        }

        if (pedido.getStatus() == StatusPedidoVenda.CONFIRMADO) {
            for (PedidoVendaItem item : pedido.getItens()) {
                Produto produto = item.getProduto();
                int qtd = item.getQuantidade();
                produto.setQuantidade(produto.getQuantidade() + qtd);
                produtoRepository.save(produto);
                estoqueDepositoService.ajustarNoDepositoPadrao(produto, pedido.getOrg(), qtd);
            }
            movimentacaoRepository.deleteByPedidoVendaId(pedido.getId());
        }

        pedido.setStatus(StatusPedidoVenda.CANCELADO);
        pedido.setAtualizadoEm(LocalDateTime.now());
        PedidoVenda salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("PedidoVenda", salvo.getId(), AcaoAuditoria.UPDATE,
                "Pedido " + salvo.getNumero() + " cancelado");
        return salvo;
    }

    private List<PedidoVendaItem> montarItens(PedidoVenda pedido, List<PedidoVendaItemRequest> reqs, Long orgId,
                                              TipoPedidoVenda tipo) {
        if (reqs == null || reqs.isEmpty()) {
            throw new IllegalArgumentException("Pedido deve ter ao menos um item (produto)");
        }
        List<PedidoVendaItem> itens = new ArrayList<>();
        for (PedidoVendaItemRequest r : reqs) {
            Produto produto = produtoRepository.findByIdAndOrgId(r.produtoId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + r.produtoId()));
            BigDecimal preco = tipo == TipoPedidoVenda.INTERNO
                    ? BigDecimal.ZERO
                    : (r.precoUnitario() != null ? r.precoUnitario() : produto.getPreco());
            PedidoVendaItem item = new PedidoVendaItem();
            item.setPedido(pedido);
            item.setProduto(produto);
            item.setQuantidade(r.quantidade());
            item.setPrecoUnitario(preco);
            item.setSubtotal(preco.multiply(BigDecimal.valueOf(r.quantidade())));
            itens.add(item);
        }
        return itens;
    }

    private BigDecimal calcularTotal(List<PedidoVendaItem> itens) {
        return itens.stream().map(PedidoVendaItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String gerarNumero(Long orgId, TipoPedidoVenda tipo) {
        String prefix = tipo == TipoPedidoVenda.INTERNO ? "PI" : "PV";
        long seq = pedidoRepository.countByOrgId(orgId) + 1;
        String numero = prefix + "-" + orgId + "-" + String.format("%05d", seq);
        while (pedidoRepository.existsByNumeroAndOrgId(numero, orgId)) {
            seq++;
            numero = prefix + "-" + orgId + "-" + String.format("%05d", seq);
        }
        return numero;
    }
}
