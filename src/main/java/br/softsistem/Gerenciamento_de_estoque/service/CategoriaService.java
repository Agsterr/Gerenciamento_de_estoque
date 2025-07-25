package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.CategoriaDto.CategoriaRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.CategoriaRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    @Autowired
    CategoriaRepository categoriaRepository;

    @Autowired
    OrgRepository orgRepository;

    // Listar todas as categorias de uma organização com paginação
    public Page<Categoria> listarTodos(Long orgId, Pageable pageable) {
        return categoriaRepository.findByOrg_Id(orgId, pageable);  // Filtra por orgId e pagina
    }

    // Buscar uma categoria pelo nome e orgId
    public Optional<Categoria> buscarPorNomeEOrgId(String nome, Long orgId) {
        return categoriaRepository.findByNomeAndOrg_Id(nome, orgId);  // Busca por nome e orgId
    }

    // Buscar categorias cujo nome contenha uma parte do nome e que pertençam a uma organização específica
    public Page<Categoria> buscarPorParteDoNomeEOrgId(String parteDoNome, Long orgId, Pageable pageable) {
        return categoriaRepository.findByNomeContainingAndOrg_Id(parteDoNome, orgId, pageable);  // Busca por parte do nome e orgId com paginação
    }

    // Salvar uma nova categoria associada à organização
    public Categoria salvarCategoria(CategoriaRequest request, Long orgId) {
        Optional<Org> org = orgRepository.findById(orgId);
        if (org.isPresent()) {
            Categoria categoria = new Categoria();
            categoria.setNome(request.nome());
            categoria.setDescricao(request.descricao());
            categoria.setOrg(org.get());  // Associando a organização
            return categoriaRepository.save(categoria);
        }
        throw new RuntimeException("Organização não encontrada");  // Pode criar uma exceção personalizada
    }

    // Editar uma categoria existente
    public Categoria editarCategoria(Long id, CategoriaRequest request, Long orgId) {
        Optional<Categoria> categoriaExistente = categoriaRepository.findById(id);
        if (categoriaExistente.isPresent() && categoriaExistente.get().getOrg().getId().equals(orgId)) {
            Categoria categoria = categoriaExistente.get();
            categoria.setNome(request.nome());
            categoria.setDescricao(request.descricao());
            return categoriaRepository.save(categoria);
        }
        return null;  // Se a categoria não for encontrada ou não pertencer à organização
    }

    // Excluir uma categoria existente
    public boolean excluirCategoria(Long id, Long orgId) {
        Optional<Categoria> categoriaExistente = categoriaRepository.findById(id);
        if (categoriaExistente.isPresent() && categoriaExistente.get().getOrg().getId().equals(orgId)) {
            categoriaRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
