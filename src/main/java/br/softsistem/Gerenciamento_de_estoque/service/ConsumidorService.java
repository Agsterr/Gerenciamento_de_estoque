package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.ConsumidorRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.exception.ConsumidorNaoEncontradoException;
import br.softsistem.Gerenciamento_de_estoque.exception.OrganizacaoNaoEncontradaException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ConsumidorService {

    private final ConsumidorRepository consumidorRepository;
    private final OrgRepository orgRepository;

    public ConsumidorService(ConsumidorRepository consumidorRepository, OrgRepository orgRepository) {
        this.consumidorRepository = consumidorRepository;
        this.orgRepository = orgRepository;
    }

    private Long requireOrgId() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }
        return orgId;
    }

    @Cacheable(value = "consumidores", key = "'lista-' + #root.target.getCurrentOrgId() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Consumidor> listarTodos(Pageable pageable) {
        Long orgId = requireOrgId();
        return consumidorRepository.findByOrg_Id(orgId, pageable);
    }

    @Cacheable(value = "consumidores", key = "'nome-' + #nome + '-' + #root.target.getCurrentOrgId()")
    public Optional<Consumidor> buscarPorNome(String nome) {
        Long orgId = requireOrgId();
        return consumidorRepository.findByNomeAndOrg_Id(nome, orgId);
    }

    public Consumidor buscarPorId(Long id) {
        Long orgId = requireOrgId();
        return consumidorRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ConsumidorNaoEncontradoException("Consumidor não encontrado"));
    }

    @Transactional
    @CacheEvict(value = "consumidores", allEntries = true)
    public Consumidor salvar(Consumidor consumidor) {
        Long orgId = requireOrgId();
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrganizacaoNaoEncontradaException("Organização não encontrada"));

        consumidor.setOrg(org);
        return consumidorRepository.save(consumidor);
    }

    @Transactional
    @CacheEvict(value = "consumidores", allEntries = true)
    public Consumidor editar(Long id, Consumidor consumidorAtualizado) {
        Long orgId = requireOrgId();
        Consumidor consumidorExistente = consumidorRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ConsumidorNaoEncontradoException("Consumidor não encontrado ou não pertence à organização"));

        consumidorExistente.setNome(consumidorAtualizado.getNome());
        consumidorExistente.setCpf(consumidorAtualizado.getCpf());
        consumidorExistente.setEndereco(consumidorAtualizado.getEndereco());

        return consumidorRepository.save(consumidorExistente);
    }

    @Transactional
    @CacheEvict(value = "consumidores", allEntries = true)
    public void excluir(Long id) {
        Long orgId = requireOrgId();
        Consumidor consumidor = consumidorRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ConsumidorNaoEncontradoException("Consumidor não encontrado ou não pertence à organização"));

        consumidorRepository.delete(consumidor);
    }

    public Long getCurrentOrgId() {
        return SecurityUtils.getCurrentOrgId();
    }
}
