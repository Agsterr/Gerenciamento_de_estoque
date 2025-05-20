package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.service.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService service;

    // Constructor Injection
    @Autowired
    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    // Listar todos os produtos ativos de uma organização com paginação
    @GetMapping
    public Page<ProdutoDto> listarTodos(@RequestParam Long orgId, Pageable pageable) {
        // Chama o serviço para obter os produtos e aplica a transformação para ProdutoDto
        return service.listarTodos(orgId, pageable).map(ProdutoDto::new);
    }

    // Criar ou atualizar um novo produto
    @PostMapping
    public ResponseEntity<Map<String, String>> criarProduto(@RequestBody ProdutoRequest produtoRequest) {
        // Delegando a criação ou atualização para o serviço
        service.salvar(produtoRequest, produtoRequest.getOrgId());

        return ResponseEntity.ok(Map.of("message", "Produto criado ou atualizado com sucesso!"));
    }

    // Excluir um produto (marcando como inativo)
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> excluir(@PathVariable Long id, @RequestParam Long orgId) {
        // Verifica se o produto existe e o exclui (marca como inativo)
        service.excluir(id, orgId);

        return ResponseEntity.ok(Map.of("message", "Produto excluído com sucesso."));
    }

    // Buscar produto por ID e organização
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDto> buscarPorId(@PathVariable Long id, @RequestParam Long orgId) {
        // Delegando a busca para o serviço
        Produto produto = service.buscarPorId(id, orgId);
        return ResponseEntity.ok(new ProdutoDto(produto));
    }

    // Consultar o total de entradas por ano de uma organização
    @GetMapping("/entradas/ano")
    public ResponseEntity<BigDecimal> getTotalEntradasPorAno(@RequestParam Long orgId, @RequestParam int ano) {
        BigDecimal totalEntradas = service.getTotalEntradasPorAno(orgId, ano);
        return ResponseEntity.ok(totalEntradas);
    }

    // Consultar o total de saídas por ano de uma organização
    @GetMapping("/saidas/ano")
    public ResponseEntity<BigDecimal> getTotalSaidasPorAno(@RequestParam Long orgId, @RequestParam int ano) {
        BigDecimal totalSaidas = service.getTotalSaidasPorAno(orgId, ano);
        return ResponseEntity.ok(totalSaidas);
    }

    // Consultar o total de entradas por mês de uma organização
    @GetMapping("/entradas/mes")
    public ResponseEntity<BigDecimal> getTotalEntradasPorMes(@RequestParam Long orgId, @RequestParam int ano, @RequestParam int mes) {
        BigDecimal totalEntradas = service.getTotalEntradasPorMes(orgId, ano, mes);
        return ResponseEntity.ok(totalEntradas);
    }

    // Consultar o total de saídas por mês de uma organização
    @GetMapping("/saidas/mes")
    public ResponseEntity<BigDecimal> getTotalSaidasPorMes(@RequestParam Long orgId, @RequestParam int ano, @RequestParam int mes) {
        BigDecimal totalSaidas = service.getTotalSaidasPorMes(orgId, ano, mes);
        return ResponseEntity.ok(totalSaidas);
    }
}
