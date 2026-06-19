package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgRequestDto;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrgService {

    private final OrgRepository orgRepository;

    public OrgService(OrgRepository orgRepository) {
        this.orgRepository = orgRepository;
    }

    @Transactional
    @CacheEvict(value = "organizacoes", allEntries = true)
    public Optional<OrgDto> createOrg(OrgRequestDto orgRequestDto) {
        Optional<Org> existingOrg = orgRepository.findByNome(orgRequestDto.nome());
        if (existingOrg.isPresent()) {
            return Optional.empty();
        }
        Org org = new Org(orgRequestDto.nome());
        org = orgRepository.save(org);
        return Optional.of(new OrgDto(org));
    }

    @Cacheable(value = "organizacoes", key = "'todas'")
    public List<OrgDto> getAllOrgs() {
        return orgRepository.findAll().stream()
                .map(OrgDto::new)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "organizacoes", key = "'org-' + #id")
    public Optional<OrgDto> getOrgById(Long id) {
        return orgRepository.findById(id).map(OrgDto::new);
    }

    @Transactional
    @CacheEvict(value = "organizacoes", allEntries = true)
    public OrgDto updateOrg(Long id, OrgRequestDto orgRequestDto) {
        Org existingOrg = orgRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada"));

        Optional<Org> orgComMesmoNome = orgRepository.findByNome(orgRequestDto.nome());
        if (orgComMesmoNome.isPresent() && !orgComMesmoNome.get().getId().equals(id)) {
            throw new IllegalStateException("Já existe outra organização com este nome.");
        }

        existingOrg.setNome(orgRequestDto.nome());
        Org updatedOrg = orgRepository.save(existingOrg);
        return new OrgDto(updatedOrg);
    }

    @Transactional
    @CacheEvict(value = "organizacoes", allEntries = true)
    public boolean desativarOrg(Long id) {
        return orgRepository.findById(id).map(org -> {
            org.setAtivo(false);
            orgRepository.save(org);
            return true;
        }).orElse(false);
    }

    @Transactional
    @CacheEvict(value = "organizacoes", allEntries = true)
    public boolean ativarOrg(Long id) {
        return orgRepository.findById(id).map(org -> {
            org.setAtivo(true);
            orgRepository.save(org);
            return true;
        }).orElse(false);
    }
}
