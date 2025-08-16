package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

@Service
public class EntregaPeriodoService {

    private final EntregaService entregaService;

    // timezone padrão caso a organização não tenha uma definida
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");

    public EntregaPeriodoService(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    /**
     * Aceita datas com timezone (ex.: 2025-08-10T23:59:59Z, 2025-08-10T23:59:59-03:00)
     * Normaliza para a timezone da organização e delega para o método já existente
     * que trabalha com LocalDateTime.
     */
    public Page<EntregaResponseDto> listarEntregasPorPeriodoNormalizado(
            Long orgId,
            OffsetDateTime inicioCli,
            OffsetDateTime fimCli,
            Pageable pageable
    ) {
        Objects.requireNonNull(orgId, "orgId é obrigatório");
        Objects.requireNonNull(inicioCli, "inicio é obrigatório");
        Objects.requireNonNull(fimCli, "fim é obrigatório");

        ZoneId zoneOrg = carregarTimezoneDaOrg(orgId).orElse(DEFAULT_ZONE);

        // Mantém o instante e converte para a zona da organização
        LocalDateTime inicio = inicioCli.atZoneSameInstant(zoneOrg).toLocalDateTime();
        LocalDateTime fim    = fimCli.atZoneSameInstant(zoneOrg).toLocalDateTime();

        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Período inválido: fim < início");
        }

        // Delegação para o método já existente no seu service
        return entregaService.listarEntregasPorPeriodo(inicio, fim, pageable);
    }

    /**
     * Listagem por dia. Mantemos a assinatura com orgId para padronizar (caso no futuro
     * você filtre por organização). Por enquanto, só delega.
     */
    public Page<EntregaResponseDto> listarEntregasPorDia(
            Long orgId,
            LocalDate dia,
            Pageable pageable
    ) {
        Objects.requireNonNull(orgId, "orgId é obrigatório");
        Objects.requireNonNull(dia, "dia é obrigatório");
        return entregaService.listarEntregasPorDia(dia, pageable);
    }

    public Page<EntregaResponseDto> listarEntregasPorMes(Long orgId, int mes, int ano, Pageable pageable) {
        Objects.requireNonNull(orgId, "orgId é obrigatório");
        return entregaService.listarEntregasPorMes(mes, ano, pageable);
    }

    public Page<EntregaResponseDto> listarEntregasPorAno(Long orgId, int ano, Pageable pageable) {
        Objects.requireNonNull(orgId, "orgId é obrigatório");
        return entregaService.listarEntregasPorAno(ano, pageable);
    }

    /**
     * TODO: buscar a timezone da organização no banco/config.
     * Mantido como Optional.empty() para cair no DEFAULT_ZONE.
     */
    private Optional<ZoneId> carregarTimezoneDaOrg(Long orgId) {
        // Exemplo de implementação futura:
        // return organizacaoRepository.findById(orgId).map(Organizacao::getTimezone).map(ZoneId::of);
        return Optional.empty();
    }
}
