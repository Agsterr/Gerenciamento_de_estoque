package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.ConsumidorRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.exception.ConsumidorNaoEncontradoException;
import br.softsistem.Gerenciamento_de_estoque.exception.OrganizacaoNaoEncontradaException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConsumidorService {

    private final ConsumidorRepository consumidorRepository;
    private final OrgRepository orgRepository;

    // Constructor Injection
    public ConsumidorService(ConsumidorRepository consumidorRepository, OrgRepository orgRepository) {
        this.consumidorRepository = consumidorRepository;
        this.orgRepository = orgRepository;
    }

    // Listar todos os consumidores de uma organização
    public List<Consumidor> listarTodos() {
        Long orgId = SecurityUtils.getCurrentOrgId();  // Obtém o org_id do contexto de segurança
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }
        return consumidorRepository.findByOrg_Id(orgId);  // Filtra consumidores pela organização
    }

    // Buscar um consumidor por nome e organização
    public Optional<Consumidor> buscarPorNome(String nome) {
        Long orgId = SecurityUtils.getCurrentOrgId();  // Obtém o org_id do contexto de segurança
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }
        return consumidorRepository.findByNomeAndOrg_Id(nome, orgId);  // Filtra pelo nome e org_id
    }

    // Salvar um consumidor associado à organização
    public Consumidor salvar(Consumidor consumidor) {
        Long orgId = SecurityUtils.getCurrentOrgId();  // Obtém o org_id do contexto de segurança
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }

        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrganizacaoNaoEncontradaException("Organização não encontrada"));

        consumidor.setOrg(org);  // Associa a organização ao consumidor
        return consumidorRepository.save(consumidor);  // Salva o consumidor associado à organização
    }

    // Editar um consumidor existente
    public Consumidor editar(Long id, Consumidor consumidorAtualizado) {
        // Busca o consumidor existente e atualiza seus dados
        Consumidor consumidorExistente = consumidorRepository.findById(id)
                .orElseThrow(() -> new ConsumidorNaoEncontradoException("Consumidor não encontrado"));

        // Atualiza os dados do consumidor
        consumidorExistente.setNome(consumidorAtualizado.getNome());
        consumidorExistente.setCpf(consumidorAtualizado.getCpf());
        consumidorExistente.setEndereco(consumidorAtualizado.getEndereco());
        // Atualize outros campos conforme necessário

        return consumidorRepository.save(consumidorExistente);  // Salva o consumidor atualizado
    }

    // Excluir um consumidor existente
    public void excluir(Long id) {
        // Busca o consumidor a ser excluído
        Consumidor consumidor = consumidorRepository.findById(id)
                .orElseThrow(() -> new ConsumidorNaoEncontradoException("Consumidor não encontrado"));

        consumidorRepository.delete(consumidor);  // Exclui o consumidor
    }
}
