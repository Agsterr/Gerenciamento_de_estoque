package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.FornecedorRequest;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.AcaoAuditoria;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Fornecedor;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.FornecedorRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FornecedorService {

    private final FornecedorRepository repository;
    private final OrgRepository orgRepository;
    private final AuditoriaService auditoriaService;

    public FornecedorService(FornecedorRepository repository, OrgRepository orgRepository,
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

    public Page<Fornecedor> listar(Pageable pageable) {
        return repository.findByOrgIdAndAtivoTrue(requireOrgId(), pageable);
    }

    public Fornecedor buscarPorId(Long id) {
        return repository.findByIdAndOrgId(id, requireOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado"));
    }

    @Transactional
    public Fornecedor criar(FornecedorRequest req) {
        Long orgId = requireOrgId();
        Org org = orgRepository.findById(orgId).orElseThrow();
        Fornecedor f = new Fornecedor();
        f.setNome(req.nome());
        f.setCnpj(req.cnpj());
        f.setEmail(req.email());
        f.setTelefone(req.telefone());
        f.setEndereco(req.endereco());
        f.setOrg(org);
        Fornecedor salvo = repository.save(f);
        auditoriaService.registrar("Fornecedor", salvo.getId(), AcaoAuditoria.CREATE, "Fornecedor criado: " + salvo.getNome());
        return salvo;
    }

    @Transactional
    public Fornecedor atualizar(Long id, FornecedorRequest req) {
        Fornecedor f = buscarPorId(id);
        f.setNome(req.nome());
        f.setCnpj(req.cnpj());
        f.setEmail(req.email());
        f.setTelefone(req.telefone());
        f.setEndereco(req.endereco());
        Fornecedor salvo = repository.save(f);
        auditoriaService.registrar("Fornecedor", salvo.getId(), AcaoAuditoria.UPDATE, "Fornecedor atualizado: " + salvo.getNome());
        return salvo;
    }

    @Transactional
    public void excluir(Long id) {
        Fornecedor f = buscarPorId(id);
        f.setAtivo(false);
        repository.save(f);
        auditoriaService.registrar("Fornecedor", id, AcaoAuditoria.DELETE, "Fornecedor desativado: " + f.getNome());
    }
}
