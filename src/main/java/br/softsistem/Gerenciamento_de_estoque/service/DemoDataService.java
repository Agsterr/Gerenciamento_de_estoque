package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.model.Deposito;
import br.softsistem.Gerenciamento_de_estoque.model.Fornecedor;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.repository.CategoriaRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ConsumidorRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.DepositoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.FornecedorRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class DemoDataService {

    private final OrgRepository orgRepository;
    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ConsumidorRepository consumidorRepository;
    private final FornecedorRepository fornecedorRepository;
    private final DepositoRepository depositoRepository;
    private final DemoOrgPurgeService demoOrgPurgeService;

    public DemoDataService(OrgRepository orgRepository,
                           ProdutoRepository produtoRepository,
                           CategoriaRepository categoriaRepository,
                           ConsumidorRepository consumidorRepository,
                           FornecedorRepository fornecedorRepository,
                           DepositoRepository depositoRepository,
                           DemoOrgPurgeService demoOrgPurgeService) {
        this.orgRepository = orgRepository;
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.consumidorRepository = consumidorRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.depositoRepository = depositoRepository;
        this.demoOrgPurgeService = demoOrgPurgeService;
    }

    @Transactional
    public void prepareDemoSession(Long orgId) {
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organização demo não encontrada"));
        if (!org.isEphemeralOrg()) {
            return;
        }
        org.setDemoLastAccess(LocalDateTime.now());
        orgRepository.save(org);

        demoOrgPurgeService.purgeOperationalData(orgId);
        seedSampleData(org);
    }

    private void seedSampleData(Org org) {
        Categoria eletronicos = categoriaRepository.save(buildCategoria("Eletrônicos", "Produtos eletrônicos", org));
        Categoria alimentos = categoriaRepository.save(buildCategoria("Alimentos", "Itens de mercearia", org));

        produtoRepository.save(buildProduto("Notebook Demo", "Notebook para testes", new BigDecimal("3499.90"), 12, 2, eletronicos, org));
        produtoRepository.save(buildProduto("Mouse USB", "Mouse óptico", new BigDecimal("49.90"), 45, 5, eletronicos, org));
        produtoRepository.save(buildProduto("Arroz 5kg", "Arroz tipo 1", new BigDecimal("24.50"), 80, 10, alimentos, org));

        Consumidor consumidor = new Consumidor();
        consumidor.setNome("Cliente Demo");
        consumidor.setCpf("00000000000");
        consumidor.setEndereco("Rua Exemplo, 100");
        consumidor.setOrg(org);
        consumidorRepository.save(consumidor);

        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setNome("Fornecedor Demo");
        fornecedor.setCnpj("00000000000000");
        fornecedor.setEmail("fornecedor@demo.local");
        fornecedor.setTelefone("(11) 99999-0000");
        fornecedor.setOrg(org);
        fornecedorRepository.save(fornecedor);

        Deposito deposito = new Deposito();
        deposito.setNome("Depósito Principal");
        deposito.setEndereco("Galpão Demo");
        deposito.setOrg(org);
        depositoRepository.save(deposito);
    }

    private Categoria buildCategoria(String nome, String descricao, Org org) {
        Categoria c = new Categoria();
        c.setNome(nome);
        c.setDescricao(descricao);
        c.setOrg(org);
        return c;
    }

    private Produto buildProduto(String nome, String descricao, BigDecimal preco, int qtd, int min,
                                 Categoria categoria, Org org) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setDescricao(descricao);
        p.setPreco(preco);
        p.setQuantidade(qtd);
        p.setQuantidadeMinima(min);
        p.setCategoria(categoria);
        p.setOrg(org);
        p.setAtivo(true);
        return p;
    }
}
