package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.CategoriaDto.CategoriaRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.CategoriaDto.CategoriaResponse;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService service;

    // Listar todas as categorias
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listarTodos() {
        List<CategoriaResponse> categorias = service.listarTodos()
                .stream()
                .map(categoria -> new CategoriaResponse(categoria.getId(), categoria.getNome()))
                .toList();
        return ResponseEntity.ok(categorias);
    }

    // Salvar nova categoria
    @PostMapping
    public ResponseEntity<CategoriaResponse> salvar(@Valid @RequestBody CategoriaRequest request) {
        Categoria categoria = new Categoria();
        categoria.setNome(request.nome());

        Categoria salvo = service.salvar(categoria);
        CategoriaResponse response = new CategoriaResponse(salvo.getId(), salvo.getNome());
        return ResponseEntity.ok(response);
    }
}
