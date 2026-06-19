package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Deposito;
import br.softsistem.Gerenciamento_de_estoque.model.EstoqueDeposito;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.repository.DepositoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.EstoqueDepositoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstoqueDepositoService {

    private final EstoqueDepositoRepository repository;
    private final DepositoService depositoService;
    private final DepositoRepository depositoRepository;
    private final ProdutoRepository produtoRepository;

    public EstoqueDepositoService(EstoqueDepositoRepository repository,
                                  DepositoService depositoService,
                                  DepositoRepository depositoRepository,
                                  ProdutoRepository produtoRepository) {
        this.repository = repository;
        this.depositoService = depositoService;
        this.depositoRepository = depositoRepository;
        this.produtoRepository = produtoRepository;
    }

    public List<EstoqueDeposito> listarPorDeposito(Long depositoId) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) throw new ResourceNotFoundException("Organização não encontrada");
        return repository.findByDepositoIdAndOrgId(depositoId, orgId);
    }

    public List<EstoqueDeposito> listarPorProduto(Long produtoId) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) throw new ResourceNotFoundException("Organização não encontrada");
        return repository.findByProdutoIdAndOrgId(produtoId, orgId);
    }

    @Transactional
    public void ajustarNoDepositoPadrao(Produto produto, Org org, int delta) {
        Deposito deposito = depositoService.ensureDepositoPadrao(org.getId());
        ajustarNoDeposito(deposito, produto, org, delta);
    }

    @Transactional
    public void ajustarNoDeposito(Deposito deposito, Produto produto, Org org, int delta) {
        if (delta == 0 || produto.getId() == null) return;
        EstoqueDeposito estoque = obterOuCriar(deposito, produto, org);
        int novaQtd = estoque.getQuantidade() + delta;
        if (novaQtd < 0) {
            throw new IllegalArgumentException(
                    "Estoque insuficiente no depósito '" + deposito.getNome() + "' para '" + produto.getNome() + "'.");
        }
        estoque.setQuantidade(novaQtd);
        repository.save(estoque);
        sincronizarProdutoGlobal(produto, org);
    }

    /** Soma estoque de todos os depósitos e atualiza produto.quantidade */
    @Transactional
    public void sincronizarProdutoGlobal(Produto produto, Org org) {
        if (produto.getId() == null) return;
        int total = repository.findByProdutoIdAndOrgId(produto.getId(), org.getId()).stream()
                .mapToInt(EstoqueDeposito::getQuantidade).sum();
        produto.setQuantidade(total);
        produtoRepository.save(produto);
    }

    @Transactional
    public void transferir(Long depositoOrigemId, Long depositoDestinoId, Long produtoId, int quantidade) {
        if (quantidade <= 0) throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        if (depositoOrigemId.equals(depositoDestinoId)) {
            throw new IllegalArgumentException("Depósito de origem e destino devem ser diferentes");
        }
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) throw new ResourceNotFoundException("Organização não encontrada");

        Deposito origem = depositoRepository.findByIdAndOrgId(depositoOrigemId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Depósito de origem não encontrado"));
        Deposito destino = depositoRepository.findByIdAndOrgId(depositoDestinoId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Depósito de destino não encontrado"));
        Produto produto = produtoRepository.findByIdAndOrgId(produtoId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        Org org = produto.getOrg();

        ajustarNoDeposito(origem, produto, org, -quantidade);
        ajustarNoDeposito(destino, produto, org, quantidade);
    }

    private EstoqueDeposito obterOuCriar(Deposito deposito, Produto produto, Org org) {
        return repository.findByProdutoIdAndDepositoId(produto.getId(), deposito.getId())
                .orElseGet(() -> {
                    EstoqueDeposito novo = new EstoqueDeposito();
                    novo.setProduto(produto);
                    novo.setDeposito(deposito);
                    novo.setOrg(org);
                    novo.setQuantidade(0);
                    return novo;
                });
    }
}
