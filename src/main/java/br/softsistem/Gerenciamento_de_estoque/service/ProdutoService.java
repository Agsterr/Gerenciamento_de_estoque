package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto.MovimentacaoProdutoDto;
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
    private final MovimentacaoProdutoService movimentacaoProdutoService;

    @Autowired
    public ProdutoService(ProdutoRepository repository, MovimentacaoProdutoRepository movimentacaoProdutoRepository, CategoriaRepository categoriaRepository, OrgRepository orgRepository, MovimentacaoProdutoService movimentacaoProdutoService) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.orgRepository = orgRepository;
        this.movimentacaoProdutoRepository = movimentacaoProdutoRepository;
        this.movimentacaoProdutoService = movimentacaoProdutoService;
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

        // Setando os valores do produto
        produto.setNome(produtoRequest.getNome());
        produto.setDescricao(produtoRequest.getDescricao());
        produto.setPreco(produtoRequest.getPreco());

        // Garantir que a quantidade não seja null antes de atribuí-la
        if (produtoRequest.getQuantidade() == null) {
            produtoRequest.setQuantidade(0);  // Se não for passado, atribui 0
        }

        // Definindo a quantidade no objeto produto
        produto.setQuantidade(produtoRequest.getQuantidade());  // Atribui a quantidade

        // Se o produto estava inativo, reativa ele e ajusta a quantidade
        if (produto.getAtivo() != null && !produto.getAtivo()) {
            produto.setAtivo(true);
        }

        produto.setQuantidadeMinima(produtoRequest.getQuantidadeMinima());
        produto.setCategoria(categoria);
        produto.setOrg(org);

        // Salvando o produto no banco
        Produto salvo = repository.save(produto);

        // Registrar movimentação de ENTRADA
        MovimentacaoProduto movimentacao = new MovimentacaoProduto();
        movimentacao.setProduto(salvo);
        movimentacao.setQuantidade(produtoRequest.getQuantidade());
        movimentacao.setDataHora(LocalDateTime.now());
        movimentacao.setTipo(TipoMovimentacao.ENTRADA);
        movimentacao.setOrg(org);

        // Salvando a movimentação de produto
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

    public Produto editar(Long id, ProdutoRequest produtoRequest, Long orgId) {
        Categoria categoria = categoriaRepository.findById(produtoRequest.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));

        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada."));

        Produto produtoExistente = repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID fornecido ou não pertence à organização."));

        // Capturar quantidade anterior para calcular diferença
        Integer quantidadeAnterior = produtoExistente.getQuantidade();
        Integer novaQuantidade = produtoRequest.getQuantidade();

        // Atualiza os campos com os valores do request
        produtoExistente.setNome(produtoRequest.getNome());
        produtoExistente.setDescricao(produtoRequest.getDescricao());
        produtoExistente.setPreco(produtoRequest.getPreco());
        produtoExistente.setQuantidade(novaQuantidade);
        produtoExistente.setQuantidadeMinima(produtoRequest.getQuantidadeMinima());
        produtoExistente.setCategoria(categoria);
        produtoExistente.setOrg(org);

        Produto salvo = repository.save(produtoExistente);

        // Criar ou atualizar movimentação baseada na diferença de quantidade
        Integer diferenca = novaQuantidade - quantidadeAnterior;

        if (diferenca != 0) {
            // Obter todas as movimentações do produto
            List<MovimentacaoProdutoDto> movimentacoes = movimentacaoProdutoService.buscarPorIdProduto(salvo.getId());
            // Se houver movimentações, atualizar a quantidade da primeira movimentacao de ENTRADA
            // (assumindo que é a movimentação inicial que registrou o estoque)
            boolean movimentacaoEncontrada = false;

            for (MovimentacaoProdutoDto movimentacao : movimentacoes) {
                if (movimentacao.getTipo() == TipoMovimentacao.ENTRADA) {
                    // Atualiza quantidade total diretamente na movimentação existente
                    movimentacaoProdutoService.editarMovimentacaoSemAjustarEstoque(movimentacao.getId(), novaQuantidade);
                    movimentacaoEncontrada = true;
                    break; // Atualiza apenas a primeira movimentação de ENTRADA
                }
            }

            // Se não encontrou movimentação de ENTRADA, cria uma nova
            if (!movimentacaoEncontrada) {
                TipoMovimentacao tipo = diferenca > 0 ? TipoMovimentacao.ENTRADA : TipoMovimentacao.SAIDA;
                Integer quantidadeMovimentacao = Math.abs(diferenca);

                // Usar o método que não afeta o estoque, pois já foi atualizado diretamente
                movimentacaoProdutoService.criarMovimentacaoSemAfetarEstoque(
                        salvo.getId(),
                        quantidadeMovimentacao,
                        tipo,
                        LocalDateTime.now()
                );
            }
        }

        return salvo;
    }

}
