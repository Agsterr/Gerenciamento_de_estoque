package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.CategoriaDto.CategoriaRequest;
import br.softsistem.Gerenciamento_de_estoque.exception.OrganizacaoNaoEncontradaException;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.CategoriaRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final OrgRepository orgRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, OrgRepository orgRepository) {
        this.categoriaRepository = categoriaRepository;
        this.orgRepository = orgRepository;
    }

    public Page<Categoria> listarTodos(Long orgId, Pageable pageable) {
        return categoriaRepository.findByOrg_Id(orgId, pageable);
    }

    public Optional<Categoria> buscarPorNomeEOrgId(String nome, Long orgId) {
        return categoriaRepository.findByNomeAndOrg_Id(nome, orgId);
    }

    public Page<Categoria> buscarPorParteDoNomeEOrgId(String parteDoNome, Long orgId, Pageable pageable) {
        return categoriaRepository.findByNomeContainingAndOrg_Id(parteDoNome, orgId, pageable);
    }

    @Transactional
    public Categoria salvarCategoria(CategoriaRequest request, Long orgId) {
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrganizacaoNaoEncontradaException("Organização não encontrada"));

        if (categoriaRepository.findByNomeAndOrg_Id(request.nome(), orgId).isPresent()) {
            throw new IllegalStateException("Já existe uma categoria com este nome nesta organização.");
        }

        Categoria categoria = new Categoria();
        categoria.setNome(request.nome());
        categoria.setDescricao(request.descricao());
        categoria.setOrg(org);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria editarCategoria(Long id, CategoriaRequest request, Long orgId) {
        Categoria categoria = categoriaRepository.findById(id)
                .filter(c -> c.getOrg().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada ou não pertence à organização"));

        categoria.setNome(request.nome());
        categoria.setDescricao(request.descricao());
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void excluirCategoria(Long id, Long orgId) {
        Categoria categoria = categoriaRepository.findById(id)
                .filter(c -> c.getOrg().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada ou não pertence à organização"));

        categoriaRepository.delete(categoria);
    }
}
