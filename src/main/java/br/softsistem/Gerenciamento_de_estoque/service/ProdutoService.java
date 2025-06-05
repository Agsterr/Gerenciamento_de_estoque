package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoRequest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;
    private final CategoriaRepository categoriaRepository;
    private final OrgRepository orgRepository;
    private final MovimentacaoProdutoRepository movimentacaoProdutoRepository;

    @Autowired
    public ProdutoService(ProdutoRepository repository, MovimentacaoProdutoRepository movimentacaoProdutoRepository, CategoriaRepository categoriaRepository, OrgRepository orgRepository) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.orgRepository = orgRepository;
        this.movimentacaoProdutoRepository = movimentacaoProdutoRepository;
    }

    public List<Produto> listarProdutosComEstoqueBaixo(Long orgId) {
        return repository.findByAtivoTrueAndOrgId(orgId).stream()
                .filter(Produto::isEstoqueBaixo)
                .toList();
    }




    public Produto salvar(ProdutoRequest produtoRequest, Long orgId) {
        Categoria categoria = categoriaRepository.findById(produtoRequest.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));

        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada."));

        Produto produtoExistente = repository.findByNomeAndOrgId(produtoRequest.getNome(), orgId);

        Produto produto = (produtoExistente != null) ? produtoExistente : new Produto();
        boolean isNovo = produto.getId() == null;

        produto.setNome(produtoRequest.getNome());
        produto.setDescricao(produtoRequest.getDescricao());
        produto.setPreco(produtoRequest.getPreco());
        produto.setQuantidade(
                isNovo ? produtoRequest.getQuantidade() :
                        produto.getQuantidade() + produtoRequest.getQuantidade()
        );
        produto.setQuantidadeMinima(produtoRequest.getQuantidadeMinima());
        produto.setCategoria(categoria);
        produto.setOrg(org);

        Produto salvo = repository.save(produto);

        // Registrar movimentação de ENTRADA
        MovimentacaoProduto movimentacao = new MovimentacaoProduto();
        movimentacao.setProduto(salvo);
        movimentacao.setQuantidade(produtoRequest.getQuantidade());
        movimentacao.setDataHora(LocalDateTime.now());
        movimentacao.setTipo(TipoMovimentacao.ENTRADA);
        movimentacao.setOrg(org);

        movimentacaoProdutoRepository.save(movimentacao);

        return salvo;
    }


    public Page<Produto> listarTodos(Long orgId, Pageable pageable) {
        return repository.findByAtivoTrueAndOrgId(orgId, pageable);
    }

    public void excluir(Long id, Long orgId) {
        Produto produto = repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID fornecido ou não pertence à organização."));
        produto.setAtivo(false);
        repository.save(produto);
    }

    public Produto buscarPorId(Long id, Long orgId) {
        return repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID fornecido ou não pertence à organização."));
    }

    public boolean produtoExistente(Long id, Long orgId) {
        return repository.findByIdAndOrgId(id, orgId).isPresent();
    }
}
