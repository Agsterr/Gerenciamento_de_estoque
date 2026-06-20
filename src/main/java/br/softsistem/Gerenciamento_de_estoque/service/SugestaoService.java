package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.SugestaoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.SugestaoRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Sugestao;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.SugestaoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SugestaoService {

    private final SugestaoRepository repository;
    private final OrgRepository orgRepository;
    private final UsuarioRepository usuarioRepository;

    public SugestaoService(SugestaoRepository repository,
                           OrgRepository orgRepository,
                           UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.orgRepository = orgRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public SugestaoDto criar(SugestaoRequest request) {
        Long orgId = requireCurrentOrgId();
        Long userId = SecurityUtils.getCurrentUserId();
        String username = SecurityUtils.getCurrentUsername();

        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organização não encontrada."));

        Sugestao sugestao = new Sugestao();
        sugestao.setOrg(org);
        sugestao.setUsername(username);
        sugestao.setTexto(request.texto().trim());

        if (userId != null) {
            usuarioRepository.findById(userId).ifPresent(sugestao::setUsuario);
        }

        return new SugestaoDto(repository.save(sugestao));
    }

    @Transactional(readOnly = true)
    public Page<SugestaoDto> listarPorOrg(Pageable pageable) {
        Long orgId = requireCurrentOrgId();
        return repository.findByOrgIdOrderByCriadoEmDesc(orgId, pageable).map(SugestaoDto::new);
    }

    @Transactional(readOnly = true)
    public Page<SugestaoDto> listarGlobal(Pageable pageable) {
        return repository.findAllByOrderByCriadoEmDesc(pageable).map(SugestaoDto::new);
    }

    @Transactional
    public SugestaoDto atualizarStatus(Long id, String status) {
        Sugestao sugestao = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sugestão não encontrada."));
        if (!isStatusValido(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido.");
        }
        sugestao.setStatus(status);
        return new SugestaoDto(repository.save(sugestao));
    }

    private boolean isStatusValido(String status) {
        return "NOVA".equals(status) || "LIDA".equals(status) || "ARQUIVADA".equals(status);
    }

    private Long requireCurrentOrgId() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organização não identificada.");
        }
        return orgId;
    }
}
