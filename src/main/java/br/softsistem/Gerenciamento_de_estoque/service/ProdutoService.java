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
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Service
public class ProdutoService {

    private final ProdutoRepository repository;
    private final CategoriaRepository categoriaRepository;
    private final OrgRepository orgRepository;

    // Constructor Injection
    @Autowired
    public ProdutoService(ProdutoRepository repository, CategoriaRepository categoriaRepository, OrgRepository orgRepository) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.orgRepository = orgRepository;
    }

    // Listar todos os produtos ativos de uma organização
    public Page<Produto> listarTodos(Long orgId, Pageable pageable) {
        return repository.findByAtivoTrueAndOrgId(orgId, pageable);
    }

    // Criar ou atualizar um produto
    public Produto salvar(ProdutoRequest produtoRequest, Long orgId) {
        // Buscar a categoria usando o ID da categoria fornecido no DTO
        Categoria categoria = categoriaRepository.findById(produtoRequest.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));

        // Criar um novo objeto Produto a partir do DTO
        Produto produto = new Produto();
        produto.setNome(produtoRequest.getNome());
        produto.setDescricao(produtoRequest.getDescricao());
        produto.setPreco(produtoRequest.getPreco());
        produto.setQuantidade(produtoRequest.getQuantidade());
        produto.setQuantidadeMinima(produtoRequest.getQuantidadeMinima());
        produto.setCategoria(categoria);

        // Definir o orgId no produto, associando-o à organização
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada."));

        produto.setOrg(org);

        // Verificar se já existe um produto com o mesmo nome e organização
        Produto produtoExistente = repository.findByNomeAndOrgId(produto.getNome(), orgId);
        if (produtoExistente != null) {
            produtoExistente.setQuantidade(produtoExistente.getQuantidade() + produto.getQuantidade());
            return repository.save(produtoExistente);  // Atualiza o produto existente
        }
        return repository.save(produto);  // Salva como um novo produto
    }

    // Excluir um produto
    public void excluir(Long id, Long orgId) {
        Produto produto = repository.findByIdAndOrgId(id, orgId);

        if (produto == null) {
            throw new ResourceNotFoundException("Produto não encontrado com o ID fornecido ou não pertence à organização.");
        }

        produto.setAtivo(false);
        repository.save(produto);  // Marca o produto como inativo
    }

    // Total de entradas de produtos por ano e organização
    public BigDecimal getTotalEntradasPorAno(Long orgId, int ano) {
        return repository.totalEntradasPorAno(orgId, ano);
    }

    // Total de entradas de produtos por mês e organização
    public BigDecimal getTotalEntradasPorMes(Long orgId, int ano, int mes) {
        return repository.totalEntradasPorMes(orgId, ano, mes);
    }

    // Total de entradas de produtos por semana e organização
    public BigDecimal getTotalEntradasPorSemana(Long orgId, LocalDate inicioSemana, LocalDate fimSemana) {
        return repository.totalEntradasPorSemana(orgId, inicioSemana, fimSemana);
    }

    // Total de entradas de produtos por dia e organização
    public BigDecimal getTotalEntradasPorDia(Long orgId, LocalDate dia) {
        return repository.totalEntradasPorDia(orgId, dia);
    }

    // Total de saídas de produtos por ano e organização
    public BigDecimal getTotalSaidasPorAno(Long orgId, int ano) {
        return repository.totalSaidasPorAno(orgId, ano);
    }

    // Total de saídas de produtos por mês e organização
    public BigDecimal getTotalSaidasPorMes(Long orgId, int ano, int mes) {
        return repository.totalSaidasPorMes(orgId, ano, mes);
    }

    // Total de saídas de produtos por semana e organização
    public BigDecimal getTotalSaidasPorSemana(Long orgId, LocalDate inicioSemana, LocalDate fimSemana) {
        return repository.totalSaidasPorSemana(orgId, inicioSemana, fimSemana);
    }

    // Total de saídas de produtos por dia e organização
    public BigDecimal getTotalSaidasPorDia(Long orgId, LocalDate dia) {
        return repository.totalSaidasPorDia(orgId, dia);
    }
}
