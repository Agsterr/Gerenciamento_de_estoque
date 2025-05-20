package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoRequest;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.repository.CategoriaRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;
    private final CategoriaRepository categoriaRepository;
    private final OrgRepository orgRepository;

    @Autowired
    public ProdutoService(ProdutoRepository repository, CategoriaRepository categoriaRepository, OrgRepository orgRepository) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.orgRepository = orgRepository;
    }

    // Criar ou atualizar um produto
    public Produto salvar(ProdutoRequest produtoRequest, Long orgId) {
        // Buscar a categoria
        Categoria categoria = categoriaRepository.findById(produtoRequest.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));

        // Criar novo produto
        Produto produto = new Produto();
        produto.setNome(produtoRequest.getNome());
        produto.setDescricao(produtoRequest.getDescricao());
        produto.setPreco(produtoRequest.getPreco());
        produto.setQuantidade(produtoRequest.getQuantidade());
        produto.setQuantidadeMinima(produtoRequest.getQuantidadeMinima());
        produto.setCategoria(categoria);

        // Buscar organização
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada."));
        produto.setOrg(org);

        // Verificar se o produto já existe
        Produto produtoExistente = repository.findByNomeAndOrgId(produto.getNome(), orgId);
        if (produtoExistente != null) {
            // Se já existir, atualiza a quantidade
            produtoExistente.setQuantidade(produtoExistente.getQuantidade() + produto.getQuantidade());
            return repository.save(produtoExistente);  // Atualiza
        }

        // Caso contrário, cria o novo produto
        return repository.save(produto);  // Cria
    }

    // Listar todos os produtos ativos de uma organização com paginação
    public Page<Produto> listarTodos(Long orgId, Pageable pageable) {
        return repository.findByAtivoTrueAndOrgId(orgId, pageable);
    }

    // Excluir um produto (marcando como inativo)
    public void excluir(Long id, Long orgId) {
        Produto produto = repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID fornecido ou não pertence à organização."));

        produto.setAtivo(false);  // Marca o produto como inativo
        repository.save(produto);
    }

    // Consultas para total de entradas com paginação
    public BigDecimal getTotalEntradasPorAno(Long orgId, int ano) {
        return repository.totalEntradasPorAno(orgId, ano);
    }

    public BigDecimal getTotalEntradasPorMes(Long orgId, int ano, int mes) {
        return repository.totalEntradasPorMes(orgId, ano, mes);
    }

    public BigDecimal getTotalEntradasPorSemana(Long orgId, LocalDate inicioSemana, LocalDate fimSemana) {
        return repository.totalEntradasPorSemana(orgId, inicioSemana, fimSemana);
    }

    public BigDecimal getTotalEntradasPorDia(Long orgId, LocalDate dia) {
        return repository.totalEntradasPorDia(orgId, dia);
    }

    // Consultas para total de saídas com paginação
    public BigDecimal getTotalSaidasPorAno(Long orgId, int ano) {
        return repository.totalSaidasPorAno(orgId, ano);
    }

    public BigDecimal getTotalSaidasPorMes(Long orgId, int ano, int mes) {
        return repository.totalSaidasPorMes(orgId, ano, mes);
    }

    public BigDecimal getTotalSaidasPorSemana(Long orgId, LocalDate inicioSemana, LocalDate fimSemana) {
        return repository.totalSaidasPorSemana(orgId, inicioSemana, fimSemana);
    }

    public BigDecimal getTotalSaidasPorDia(Long orgId, LocalDate dia) {
        return repository.totalSaidasPorDia(orgId, dia);
    }

    // Método para buscar produto por ID e orgId
    public Produto buscarPorId(Long id, Long orgId) {
        return repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID fornecido ou não pertence à organização."));
    }

    // Método para verificar se o produto existe
    public boolean produtoExistente(Long id, Long orgId) {
        return repository.findByIdAndOrgId(id, orgId).isPresent();
    }
}
