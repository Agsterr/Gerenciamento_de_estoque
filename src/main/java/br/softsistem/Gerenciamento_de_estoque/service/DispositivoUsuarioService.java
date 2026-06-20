package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.admin.DispositivoUsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusDispositivo;
import br.softsistem.Gerenciamento_de_estoque.exception.LimiteDispositivosException;
import br.softsistem.Gerenciamento_de_estoque.model.DispositivoUsuario;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.DispositivoUsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DispositivoUsuarioService {

    private final DispositivoUsuarioRepository repository;

    public DispositivoUsuarioService(DispositivoUsuarioRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<DispositivoUsuarioDto> listarPendentes(Pageable pageable) {
        return repository.findByStatusOrderBySolicitadoEmDesc(StatusDispositivo.PENDING, pageable)
                .map(DispositivoUsuarioDto::new);
    }

    /**
     * Registra ou valida dispositivo no login. Org demo ignora limite.
     * Dispositivos existentes rejeitados bloqueiam o acesso.
     */
    @Transactional
    public void registrarOuValidarNoLogin(Usuario usuario, String fingerprint, HttpServletRequest request) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return;
        }
        Org org = usuario.getOrg();
        if (org == null) {
            return;
        }

        var existing = repository.findByUsuarioIdAndFingerprint(usuario.getId(), fingerprint.trim());
        if (existing.isPresent()) {
            DispositivoUsuario device = existing.get();
            if (device.getStatus() == StatusDispositivo.REJECTED) {
                throw new LimiteDispositivosException("Este dispositivo foi bloqueado para esta conta.");
            }
            if (device.getStatus() == StatusDispositivo.APPROVED) {
                return;
            }
            if (device.getStatus() == StatusDispositivo.PENDING) {
                device.setStatus(StatusDispositivo.APPROVED);
                device.setRevisadoEm(LocalDateTime.now());
                repository.save(device);
                return;
            }
        }

        if (!org.isEphemeralOrg() && !org.hasUnlimitedDevices()) {
            long approved = repository.countByUsuarioIdAndStatus(usuario.getId(), StatusDispositivo.APPROVED);
            if (approved >= org.getMaxDispositivos()) {
                throw new LimiteDispositivosException(
                        "Limite de " + org.getMaxDispositivos()
                                + " dispositivo(s) atingido para esta conta. Contate o administrador.");
            }
        }

        DispositivoUsuario novo = new DispositivoUsuario();
        novo.setUsuario(usuario);
        novo.setOrg(org);
        novo.setFingerprint(fingerprint.trim());
        novo.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
        novo.setNomeDispositivo(extractDeviceName(request));
        novo.setStatus(StatusDispositivo.APPROVED);
        novo.setRevisadoEm(LocalDateTime.now());
        repository.save(novo);
    }

    private String extractDeviceName(HttpServletRequest request) {
        if (request == null) {
            return "Dispositivo desconhecido";
        }
        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isBlank()) {
            return "Dispositivo desconhecido";
        }
        return ua.length() > 120 ? ua.substring(0, 120) : ua;
    }
}
