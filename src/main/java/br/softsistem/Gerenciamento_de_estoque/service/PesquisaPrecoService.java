package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.PesquisaPrecoStatsDto;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.PesquisaPrecoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.PesquisaPrecoRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.PesquisaPreco;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.PesquisaPrecoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class PesquisaPrecoService {

    private final PesquisaPrecoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final OrgRepository orgRepository;

    public PesquisaPrecoService(PesquisaPrecoRepository repository,
                                UsuarioRepository usuarioRepository,
                                OrgRepository orgRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.orgRepository = orgRepository;
    }

    @Transactional
    public PesquisaPrecoDto enviar(PesquisaPrecoRequest request) {
        validateRange(request);

        Long userId = requireUserId();
        Long orgId = requireOrgId();
        String username = SecurityUtils.getCurrentUsername();

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organização não encontrada."));

        PesquisaPreco pesquisa = repository.findByUsuarioId(userId).orElseGet(PesquisaPreco::new);
        pesquisa.setUsuario(usuario);
        pesquisa.setOrg(org);
        pesquisa.setUsername(username != null ? username : usuario.getUsername());
        pesquisa.setValorMin(request.valorMin().setScale(2, RoundingMode.HALF_UP));
        pesquisa.setValorMax(request.valorMax().setScale(2, RoundingMode.HALF_UP));
        pesquisa.setComentario(trimOrNull(request.comentario()));
        pesquisa.setAtualizadoEm(LocalDateTime.now());
        if (pesquisa.getCriadoEm() == null) {
            pesquisa.setCriadoEm(LocalDateTime.now());
        }

        return new PesquisaPrecoDto(repository.save(pesquisa));
    }

    @Transactional(readOnly = true)
    public PesquisaPrecoDto minhaResposta() {
        Long userId = requireUserId();
        return repository.findByUsuarioId(userId)
                .map(PesquisaPrecoDto::new)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PesquisaPrecoStatsDto estatisticasAdmin() {
        List<PesquisaPreco> todas = repository.findAllByOrderByCriadoEmDesc();
        List<PesquisaPrecoDto> dtos = todas.stream().map(PesquisaPrecoDto::new).toList();

        if (todas.isEmpty()) {
            return new PesquisaPrecoStatsDto(
                    0, null, null, null, null, null,
                    "Ainda não há respostas na pesquisa de preço.",
                    List.of()
            );
        }

        List<BigDecimal> mins = todas.stream().map(PesquisaPreco::getValorMin).sorted().toList();
        List<BigDecimal> maxs = todas.stream().map(PesquisaPreco::getValorMax).sorted().toList();
        List<BigDecimal> mediasIndividuais = new ArrayList<>();

        BigDecimal somaMin = BigDecimal.ZERO;
        BigDecimal somaMax = BigDecimal.ZERO;
        for (PesquisaPreco p : todas) {
            somaMin = somaMin.add(p.getValorMin());
            somaMax = somaMax.add(p.getValorMax());
            mediasIndividuais.add(p.getValorMin().add(p.getValorMax())
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP));
        }

        int n = todas.size();
        BigDecimal mediaMin = somaMin.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        BigDecimal mediaMax = somaMax.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        BigDecimal medianaMin = mediana(mins);
        BigDecimal medianaMax = mediana(maxs);
        BigDecimal precoSugerido = mediana(mediasIndividuais.stream().sorted().toList());

        String analise = String.format(
                "Com %d resposta(s), a faixa média declarada é R$ %s – R$ %s/mês. "
                        + "A mediana das faixas é R$ %s – R$ %s/mês. "
                        + "Com base no ponto médio de cada resposta, um preço mensal sugerido para avaliação é R$ %s.",
                n,
                formatBrl(mediaMin),
                formatBrl(mediaMax),
                formatBrl(medianaMin),
                formatBrl(medianaMax),
                formatBrl(precoSugerido)
        );

        return new PesquisaPrecoStatsDto(
                n,
                mediaMin,
                mediaMax,
                medianaMin,
                medianaMax,
                precoSugerido,
                analise,
                dtos
        );
    }

    private void validateRange(PesquisaPrecoRequest request) {
        if (request.valorMax().compareTo(request.valorMin()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "valorMax deve ser maior ou igual a valorMin.");
        }
    }

    private BigDecimal mediana(List<BigDecimal> sorted) {
        if (sorted == null || sorted.isEmpty()) {
            return null;
        }
        List<BigDecimal> copy = new ArrayList<>(sorted);
        Collections.sort(copy);
        int mid = copy.size() / 2;
        if (copy.size() % 2 == 1) {
            return copy.get(mid).setScale(2, RoundingMode.HALF_UP);
        }
        return copy.get(mid - 1).add(copy.get(mid))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private String formatBrl(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não identificado.");
        }
        return userId;
    }

    private Long requireOrgId() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organização não identificada.");
        }
        return orgId;
    }
}
