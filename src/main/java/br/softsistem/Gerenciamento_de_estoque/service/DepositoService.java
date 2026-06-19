package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.DepositoRequest;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.AcaoAuditoria;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Deposito;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.DepositoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DepositoService {

    private final DepositoRepository repository;
    private final OrgRepository orgRepository;
    private final AuditoriaService auditoriaService;

    public DepositoService(DepositoRepository repository, OrgRepository orgRepository,
                           AuditoriaService auditoriaService) {
        this.repository = repository;
        this.orgRepository = orgRepository;
        this.auditoriaService = auditoriaService;
    }

    private Long requireOrgId() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) throw new ResourceNotFoundException("Organização não encontrada");
        return orgId;
    }

    public List<Deposito> listar() {
        return repository.findByOrgIdAndAtivoTrue(requireOrgId());
    }

    public Deposito buscarPorId(Long id) {
        return repository.findByIdAndOrgId(id, requireOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Depósito não encontrado"));
    }

    public Deposito getDepositoPadrao(Long orgId) {
        return repository.findByOrgIdAndPadraoTrue(orgId)
                .orElseGet(() -> {
                    List<Deposito> depositos = repository.findByOrgIdAndAtivoTrue(orgId);
                    return depositos.isEmpty() ? null : depositos.get(0);
                });
    }

    /** Garante um depósito padrão para a org (cria "Depósito Principal" se necessário). */
    @Transactional
    public Deposito ensureDepositoPadrao(Long orgId) {
        Deposito existente = getDepositoPadrao(orgId);
        if (existente != null) {
            return existente;
        }
        Org org = orgRepository.findById(orgId).orElseThrow();
        Deposito d = new Deposito();
        d.setNome("Depósito Principal");
        d.setPadrao(true);
        d.setOrg(org);
        d.setAtivo(true);
        Deposito salvo = repository.save(d);
        auditoriaService.registrar("Deposito", salvo.getId(), AcaoAuditoria.CREATE,
                "Depósito padrão criado automaticamente: " + salvo.getNome());
        return salvo;
    }

    @Transactional
    public Deposito criar(DepositoRequest req) {
        Long orgId = requireOrgId();
        Org org = orgRepository.findById(orgId).orElseThrow();
        Deposito d = new Deposito();
        d.setNome(req.nome());
        d.setEndereco(req.endereco());
        d.setOrg(org);

        boolean primeiro = repository.countByOrgId(orgId) == 0;
        boolean padrao = Boolean.TRUE.equals(req.padrao()) || primeiro;
        d.setPadrao(padrao);

        if (padrao) {
            repository.findByOrgIdAndPadraoTrue(orgId).ifPresent(existente -> {
                existente.setPadrao(false);
                repository.save(existente);
            });
        }

        Deposito salvo = repository.save(d);
        auditoriaService.registrar("Deposito", salvo.getId(), AcaoAuditoria.CREATE, "Depósito criado: " + salvo.getNome());
        return salvo;
    }

    @Transactional
    public Deposito atualizar(Long id, DepositoRequest req) {
        Deposito d = buscarPorId(id);
        d.setNome(req.nome());
        d.setEndereco(req.endereco());
        if (Boolean.TRUE.equals(req.padrao())) {
            repository.findByOrgIdAndPadraoTrue(d.getOrg().getId()).ifPresent(ex -> {
                if (!ex.getId().equals(id)) {
                    ex.setPadrao(false);
                    repository.save(ex);
                }
            });
            d.setPadrao(true);
        }
        Deposito salvo = repository.save(d);
        auditoriaService.registrar("Deposito", salvo.getId(), AcaoAuditoria.UPDATE, "Depósito atualizado: " + salvo.getNome());
        return salvo;
    }

    @Transactional
    public void excluir(Long id) {
        Deposito d = buscarPorId(id);
        d.setAtivo(false);
        repository.save(d);
        auditoriaService.registrar("Deposito", id, AcaoAuditoria.DELETE, "Depósito desativado: " + d.getNome());
    }
}
