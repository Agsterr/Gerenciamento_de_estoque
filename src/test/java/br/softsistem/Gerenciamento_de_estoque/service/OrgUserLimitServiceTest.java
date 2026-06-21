package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrgUserLimitServiceTest {

    @InjectMocks
    private OrgUserLimitService service;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SubscriptionService subscriptionService;

    private Org org;

    @BeforeEach
    void setup() {
        org = new Org("Empresa");
        org.setId(1L);
    }

    @Test
    void deveBloquearQuandoLimiteDaOrgAtingido() {
        org.setMaxDispositivos(3);
        when(usuarioRepository.countAtivosByOrgId(1L)).thenReturn(3L);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.assertCanAddUser(org, 10L));
        assertTrue(ex.getMessage().contains("3"));
    }

    @Test
    void deveUsarMenorEntreDispositivosEPlano() {
        org.setMaxDispositivos(5);
        Plan plan = new Plan();
        plan.setMaxUsers(2);
        Subscription subscription = new Subscription();
        subscription.setPlan(plan);

        when(subscriptionService.hasSubscriptionBypass(10L)).thenReturn(false);
        when(subscriptionService.getCurrentSubscription(10L)).thenReturn(Optional.of(subscription));
        when(usuarioRepository.countAtivosByOrgId(1L)).thenReturn(1L);

        var limite = service.consultarLimite(org, 10L);
        assertEquals(2, limite.maximo());
        assertEquals("DISPOSITIVOS_E_PLANO", limite.origemLimite());
    }
}
