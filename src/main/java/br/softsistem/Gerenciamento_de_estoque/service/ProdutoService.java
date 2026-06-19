package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoRequest;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.AcaoAuditoria;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.model.MovimentacaoProduto;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.repository.CategoriaRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.MovimentacaoProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;
    private final CategoriaRepository categoriaRepository;
    private final OrgRepository orgRepository;
    private final MovimentacaoProdutoRepository movimentacaoProdutoRepository;
    private final AuditoriaService auditoriaService;
    private final EstoqueDepositoService estoqueDepositoService;

    public ProdutoService(ProdutoRepository repository, MovimentacaoProdutoRepository movimentacaoProdutoRepository, CategoriaRepository categoriaRepository, OrgRepository orgRepository, AuditoriaService auditoriaService, EstoqueDepositoService estoqueDepositoService) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.orgRepository = orgRepository;
        this.movimentacaoProdutoRepository = movimentacaoProdutoRepository;
        this.auditoriaService = auditoriaService;
        this.estoqueDepositoService = estoqueDepositoService;
    }

    @Cacheable(value = "produtos", key = "'estoque-baixo-' + #orgId")
    public List<Produto> listarProdutosComEstoqueBaixo(Long orgId) {
        return repository.findByAtivoTrueAndOrgId(orgId).stream()
                .filter(Produto::isEstoqueBaixo)
                .toList();
    }



    @Transactional
    @CacheEvict(value = "produtos", allEntries = true)
    public Produto salvar(ProdutoRequest produtoRequest, Long orgId) {
        Categoria categoria = categoriaRepository.findById(produtoRequest.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));

        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada."));

        Produto produtoExistente = repository.findByNomeAndOrgId(produtoRequest.getNome(), orgId);

        Produto produto = (produtoExistente != null) ? produtoExistente : new Produto();
        boolean isNovo = produto.getId() == null;

        // Setando os valores do produto
        produto.setNome(produtoRequest.getNome());
        produto.setDescricao(produtoRequest.getDescricao());
        produto.setPreco(produtoRequest.getPreco());

        // Estoque inicial só na criação; edição usa movimentações/pedidos/entregas
        int estoqueInicial = isNovo
                ? (produtoRequest.getQuantidade() != null ? produtoRequest.getQuantidade() : 0)
                : (produto.getQuantidade() != null ? produto.getQuantidade() : 0);

        if (isNovo) {
            produto.setQuantidade(estoqueInicial);
        }

        // Se o produto estava inativo, reativa ele
        if (produto.getAtivo() != null && !produto.getAtivo()) {
            produto.setAtivo(true);
        }

        produto.setQuantidadeMinima(produtoRequest.getQuantidadeMinima());
        produto.setCategoria(categoria);
        produto.setOrg(org);
        aplicarCamposAvancados(produto, produtoRequest, orgId, isNovo);

        Produto salvo = repository.save(produto);

        // Entrada inicial apenas para produto novo com estoque > 0
        if (isNovo && estoqueInicial > 0) {
            MovimentacaoProduto movimentacao = new MovimentacaoProduto();
            movimentacao.setProduto(salvo);
            movimentacao.setQuantidade(estoqueInicial);
            movimentacao.setDataHora(LocalDateTime.now());
            movimentacao.setTipo(TipoMovimentacao.ENTRADA);
            movimentacao.setOrg(org);
            movimentacaoProdutoRepository.save(movimentacao);
            estoqueDepositoService.ajustarNoDepositoPadrao(salvo, org, estoqueInicial);
        }

        auditoriaService.registrar("Produto", salvo.getId(), isNovo ? AcaoAuditoria.CREATE : AcaoAuditoria.UPDATE,
                "Produto: " + salvo.getNome() + " (SKU: " + salvo.getSku() + ")");
        return salvo;
    }






    @Cacheable(value = "produtos", key = "'lista-' + #orgId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Produto> listarTodos(Long orgId, Pageable pageable) {
        return repository.findByAtivoTrueAndOrgId(orgId, pageable);
    }

    @Transactional
    @CacheEvict(value = "produtos", allEntries = true)
    public void excluir(Long id, Long orgId) {
        Produto produto = repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID fornecido ou não pertence à organização."));
        produto.setAtivo(false);
        repository.save(produto);
    }

    @Cacheable(value = "produtos", key = "'produto-' + #id + '-' + #orgId")
    public Produto buscarPorId(Long id, Long orgId) {
        return repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID fornecido ou não pertence à organização."));
    }

    public boolean produtoExistente(Long id, Long orgId) {
        return repository.findByIdAndOrgId(id, orgId).isPresent();
    }

    @Transactional
    @CacheEvict(value = "produtos", allEntries = true)
    public Produto editar(Long id, ProdutoRequest produtoRequest, Long orgId) {
        Categoria categoria = categoriaRepository.findById(produtoRequest.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));

        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada."));

        Produto produtoExistente = repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID fornecido ou não pertence à organização."));

        // Atualiza dados cadastrais — estoque não é alterado pelo formulário
        produtoExistente.setNome(produtoRequest.getNome());
        produtoExistente.setDescricao(produtoRequest.getDescricao());
        produtoExistente.setPreco(produtoRequest.getPreco());
        produtoExistente.setQuantidadeMinima(produtoRequest.getQuantidadeMinima());
        produtoExistente.setCategoria(categoria);
        produtoExistente.setOrg(org);
        aplicarCamposAvancados(produtoExistente, produtoRequest, orgId, false);

        Produto salvo = repository.save(produtoExistente);

        auditoriaService.registrar("Produto", salvo.getId(), AcaoAuditoria.UPDATE,
                "Produto editado: " + salvo.getNome());
        return salvo;
    }

    public Produto buscarPorCodigoBarras(String codigoBarras, Long orgId) {
        return repository.findByCodigoBarrasAndOrgIdAndAtivoTrue(codigoBarras, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado para o código de barras informado"));
    }

    private void aplicarCamposAvancados(Produto produto, ProdutoRequest req, Long orgId, boolean isNovo) {
        if (req.getSku() != null && !req.getSku().isBlank()) {
            produto.setSku(req.getSku().trim());
        } else if (isNovo && produto.getSku() == null) {
            produto.setSku("SKU-" + orgId + "-" + (repository.countByAtivoTrueAndOrgId(orgId) + 1));
        }
        if (req.getCodigoBarras() != null && !req.getCodigoBarras().isBlank()) {
            produto.setCodigoBarras(req.getCodigoBarras().trim());
        }
        if (req.getCustoMedio() != null) {
            produto.setCustoMedio(req.getCustoMedio());
        } else if (isNovo && produto.getCustoMedio() == null) {
            produto.setCustoMedio(req.getPreco());
        }
    }
}
