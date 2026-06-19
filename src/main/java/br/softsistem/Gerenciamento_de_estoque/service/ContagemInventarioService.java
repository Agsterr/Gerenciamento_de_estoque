package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.ContagemInventarioRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.ContagemItemUpdateRequest;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.AcaoAuditoria;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusContagemInventario;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.*;
import br.softsistem.Gerenciamento_de_estoque.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ContagemInventarioService {

    private final ContagemInventarioRepository contagemRepository;
    private final DepositoRepository depositoRepository;
    private final EstoqueDepositoRepository estoqueDepositoRepository;
    private final ProdutoRepository produtoRepository;
    private final MovimentacaoProdutoRepository movimentacaoRepository;
    private final OrgRepository orgRepository;
    private final AuditoriaService auditoriaService;
    private final EstoqueDepositoService estoqueDepositoService;

    public ContagemInventarioService(ContagemInventarioRepository contagemRepository,
                                     DepositoRepository depositoRepository,
                                     EstoqueDepositoRepository estoqueDepositoRepository,
                                     ProdutoRepository produtoRepository,
                                     MovimentacaoProdutoRepository movimentacaoRepository,
                                     OrgRepository orgRepository,
                                     AuditoriaService auditoriaService,
                                     EstoqueDepositoService estoqueDepositoService) {
        this.contagemRepository = contagemRepository;
        this.depositoRepository = depositoRepository;
        this.estoqueDepositoRepository = estoqueDepositoRepository;
        this.produtoRepository = produtoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.orgRepository = orgRepository;
        this.auditoriaService = auditoriaService;
        this.estoqueDepositoService = estoqueDepositoService;
    }

    private Long requireOrgId() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) throw new ResourceNotFoundException("Organização não encontrada");
        return orgId;
    }

    public Page<ContagemInventario> listar(Pageable pageable) {
        return contagemRepository.findByOrgIdOrderByCriadoEmDesc(requireOrgId(), pageable);
    }

    public ContagemInventario buscarPorId(Long id) {
        return contagemRepository.findByIdAndOrgId(id, requireOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Contagem não encontrada"));
    }

    @Transactional
    public ContagemInventario iniciar(ContagemInventarioRequest req) {
        Long orgId = requireOrgId();
        Org org = orgRepository.findById(orgId).orElseThrow();
        Deposito deposito = depositoRepository.findByIdAndOrgId(req.depositoId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Depósito não encontrado"));

        ContagemInventario contagem = new ContagemInventario();
        contagem.setDeposito(deposito);
        contagem.setObservacao(req.observacao());
        contagem.setOrg(org);
        contagem.setStatus(StatusContagemInventario.ABERTA);

        List<EstoqueDeposito> estoques = estoqueDepositoRepository.findByDepositoIdAndOrgId(deposito.getId(), orgId);
        List<ContagemInventarioItem> itens = new ArrayList<>();

        if (estoques.isEmpty()) {
            produtoRepository.findByAtivoTrueAndOrgId(orgId).forEach(produto -> {
                ContagemInventarioItem item = new ContagemInventarioItem();
                item.setContagem(contagem);
                item.setProduto(produto);
                item.setQuantidadeSistema(produto.getQuantidade());
                itens.add(item);
            });
        } else {
            for (EstoqueDeposito est : estoques) {
                ContagemInventarioItem item = new ContagemInventarioItem();
                item.setContagem(contagem);
                item.setProduto(est.getProduto());
                item.setQuantidadeSistema(est.getQuantidade());
                itens.add(item);
            }
        }

        contagem.setItens(itens);
        ContagemInventario salva = contagemRepository.save(contagem);
        auditoriaService.registrar("ContagemInventario", salva.getId(), AcaoAuditoria.CREATE,
                "Contagem iniciada no depósito " + deposito.getNome());
        return salva;
    }

    @Transactional
    public ContagemInventario registrarContagem(Long id, List<ContagemItemUpdateRequest> updates) {
        ContagemInventario contagem = buscarPorId(id);
        if (contagem.getStatus() != StatusContagemInventario.ABERTA) {
            throw new IllegalStateException("Contagem não está aberta");
        }
        for (ContagemItemUpdateRequest u : updates) {
            if (u.quantidadeContada() == null || u.quantidadeContada() < 0) {
                throw new IllegalArgumentException("Quantidade contada inválida para item " + u.itemId());
            }
            contagem.getItens().stream()
                    .filter(i -> i.getId().equals(u.itemId()))
                    .findFirst()
                    .ifPresent(item -> {
                        item.setQuantidadeContada(u.quantidadeContada());
                        item.setDiferenca(u.quantidadeContada() - item.getQuantidadeSistema());
                    });
        }
        return contagemRepository.save(contagem);
    }

    @Transactional
    public ContagemInventario finalizar(Long id) {
        ContagemInventario contagem = buscarPorId(id);
        if (contagem.getStatus() != StatusContagemInventario.ABERTA) {
            throw new IllegalStateException("Contagem não está aberta");
        }

        Deposito deposito = contagem.getDeposito();
        Long orgId = contagem.getOrg().getId();

        for (ContagemInventarioItem item : contagem.getItens()) {
            if (item.getQuantidadeContada() == null) continue;
            int diff = item.getQuantidadeContada() - item.getQuantidadeSistema();
            if (diff == 0) continue;

            Produto produto = item.getProduto();
            EstoqueDeposito estoque = estoqueDepositoRepository
                    .findByProdutoIdAndDepositoId(produto.getId(), deposito.getId())
                    .orElseGet(() -> {
                        EstoqueDeposito novo = new EstoqueDeposito();
                        novo.setProduto(produto);
                        novo.setDeposito(deposito);
                        novo.setOrg(contagem.getOrg());
                        novo.setQuantidade(0);
                        return novo;
                    });

            estoque.setQuantidade(item.getQuantidadeContada());
            estoqueDepositoRepository.save(estoque);
            estoqueDepositoService.sincronizarProdutoGlobal(produto, contagem.getOrg());

            int qtdMov = Math.abs(diff);
            TipoMovimentacao tipo = diff > 0 ? TipoMovimentacao.ENTRADA : TipoMovimentacao.SAIDA;

            MovimentacaoProduto mov = new MovimentacaoProduto();
            mov.setProduto(produto);
            mov.setQuantidade(qtdMov);
            mov.setTipo(tipo);
            mov.setDataHora(LocalDateTime.now());
            mov.setOrg(contagem.getOrg());
            movimentacaoRepository.save(mov);
        }

        contagem.setStatus(StatusContagemInventario.FINALIZADA);
        contagem.setFinalizadoEm(LocalDateTime.now());
        ContagemInventario salva = contagemRepository.save(contagem);
        auditoriaService.registrar("ContagemInventario", salva.getId(), AcaoAuditoria.FINALIZAR_CONTAGEM,
                "Contagem finalizada no depósito " + deposito.getNome());
        return salva;
    }

    @Transactional
    public ContagemInventario cancelar(Long id) {
        ContagemInventario contagem = buscarPorId(id);
        if (contagem.getStatus() != StatusContagemInventario.ABERTA) {
            throw new IllegalStateException("Somente contagens abertas podem ser canceladas");
        }
        contagem.setStatus(StatusContagemInventario.CANCELADA);
        contagem.setFinalizadoEm(LocalDateTime.now());
        ContagemInventario salva = contagemRepository.save(contagem);
        auditoriaService.registrar("ContagemInventario", salva.getId(), AcaoAuditoria.DELETE,
                "Contagem cancelada no depósito " + contagem.getDeposito().getNome());
        return salva;
    }
}
