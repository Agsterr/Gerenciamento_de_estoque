package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.admin.AcessoLoginDto;
import br.softsistem.Gerenciamento_de_estoque.model.AcessoLogin;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.AcessoLoginRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LoginAuditoriaService {

    private final AcessoLoginRepository repository;

    public LoginAuditoriaService(AcessoLoginRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrarSucesso(Usuario usuario, HttpServletRequest request) {
        AcessoLogin log = baseLog(usuario.getUsername(), request);
        log.setUsuario(usuario);
        log.setOrg(usuario.getOrg());
        log.setSucesso(true);
        repository.save(log);
    }

    @Transactional
    public void registrarFalha(String username, HttpServletRequest request) {
        registrarFalha(username, request, "Credenciais inválidas");
    }

    @Transactional
    public void registrarFalha(String username, HttpServletRequest request, String detalhes) {
        AcessoLogin log = baseLog(username, request);
        log.setSucesso(false);
        log.setDetalhes(detalhes);
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AcessoLoginDto> listar(Pageable pageable) {
        return repository.findAllByOrderByDataHoraDesc(pageable).map(AcessoLoginDto::new);
    }

    private AcessoLogin baseLog(String username, HttpServletRequest request) {
        AcessoLogin log = new AcessoLogin();
        log.setUsername(username != null ? username.trim() : "desconhecido");
        log.setDataHora(LocalDateTime.now());
        if (request != null) {
            log.setIp(resolveClientIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
        }
        return log;
    }

    static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
