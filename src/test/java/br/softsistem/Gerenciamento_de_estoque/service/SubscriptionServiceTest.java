package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.PlanType;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.PlanRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.SubscriptionRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para SubscriptionService
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {
    
    @Mock
    private SubscriptionRepository subscriptionRepository;
    
    @Mock
    private PlanRepository planRepository;
    
    @Mock
    private UsuarioRepository usuarioRepository;
    
    @InjectMocks
    private SubscriptionService subscriptionService;
    
    private Usuario testUser;
    private Plan testPlan;
    private Subscription testSubscription;
    
    @BeforeEach
    @SuppressWarnings("unused") // Método é usado pelo JUnit framework
    void setUp() {
        // Configurar usuário de teste
        testUser = new Usuario();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        
        // Configurar plano de teste
        testPlan = new Plan();
        testPlan.setId(1L);
        testPlan.setName("Plano Básico");
        testPlan.setType(PlanType.BASIC);
        testPlan.setPrice(new BigDecimal("30.00"));
        testPlan.setMaxUsers(5);
        testPlan.setHasReports(true);
        
        // Configurar assinatura de teste
        testSubscription = new Subscription();
        testSubscription.setId(1L);
        testSubscription.setUser(testUser);
        testSubscription.setPlan(testPlan);
        testSubscription.setStatus(SubscriptionStatus.TRIAL);
        testSubscription.setTrialStart(LocalDateTime.now());
        testSubscription.setTrialEnd(LocalDateTime.now().plusDays(14));
    }
    
    @Test
    void testGetCurrentSubscription_Success() {
        // Arrange
        when(subscriptionRepository.findByUserIdAndStatusIn(anyLong(), any()))
            .thenReturn(Optional.of(testSubscription));
        
        // Act
        Optional<Subscription> result = subscriptionService.getCurrentSubscription(1L);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(testSubscription.getId(), result.get().getId());
        assertEquals(SubscriptionStatus.TRIAL, result.get().getStatus());
        
        verify(subscriptionRepository).findByUserIdAndStatusIn(1L, 
            List.of(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE));
    }
    
    @Test
    void testGetCurrentSubscription_NotFound() {
        // Arrange
        when(subscriptionRepository.findByUserIdAndStatusIn(anyLong(), any()))
            .thenReturn(Optional.empty());
        
        // Act
        Optional<Subscription> result = subscriptionService.getCurrentSubscription(1L);
        
        // Assert
        assertFalse(result.isPresent());
    }
    
    @Test
    void testCreateTrialSubscription_Success() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(subscriptionRepository.findByUserIdAndStatusIn(anyLong(), any()))
            .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenReturn(testSubscription);
        
        // Act
        Subscription result = subscriptionService.createTrialSubscription(1L, 1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(testPlan, result.getPlan());
        assertEquals(SubscriptionStatus.TRIAL, result.getStatus());
        
        verify(subscriptionRepository).save(any(Subscription.class));
    }
    
    @Test
    void testCreateTrialSubscription_UserNotFound() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> subscriptionService.createTrialSubscription(1L, 1L)
        );
        
        assertEquals("Usuário não encontrado", exception.getMessage());
    }
    
    @Test
    void testCreateTrialSubscription_PlanNotFound() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(planRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> subscriptionService.createTrialSubscription(1L, 1L)
        );
        
        assertEquals("Plano não encontrado", exception.getMessage());
    }
    
    @Test
    void testCreateTrialSubscription_UserAlreadyHasSubscription() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(subscriptionRepository.findByUserIdAndStatusIn(anyLong(), any()))
            .thenReturn(Optional.of(testSubscription));
        
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> subscriptionService.createTrialSubscription(1L, 1L)
        );
        
        assertEquals("Usuário já possui uma assinatura ativa", exception.getMessage());
    }
    
    @Test
    void testCanUserAccess_WithReports() {
        // Arrange
        when(subscriptionRepository.findByUserIdAndStatusIn(anyLong(), any()))
            .thenReturn(Optional.of(testSubscription));
        
        // Act
        boolean result = subscriptionService.canUserAccess(1L, "reports");
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void testCanUserAccess_WithoutSubscription() {
        // Arrange
        when(subscriptionRepository.findByUserIdAndStatusIn(anyLong(), any()))
            .thenReturn(Optional.empty());
        
        // Act
        boolean result = subscriptionService.canUserAccess(1L, "reports");
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void testIsWithinLimits_Users() {
        // Arrange
        when(subscriptionRepository.findByUserIdAndStatusIn(anyLong(), any()))
            .thenReturn(Optional.of(testSubscription));
        
        // Act
        boolean withinLimits = subscriptionService.isWithinLimits(1L, "users", 3);
        boolean exceedsLimits = subscriptionService.isWithinLimits(1L, "users", 6);
        
        // Assert
        assertTrue(withinLimits);
        assertFalse(exceedsLimits);
    }
    
    @Test
    void testIsWithinLimits_UnlimitedPlan() {
        // Arrange
        testPlan.setMaxUsers(null); // Plano ilimitado
        when(subscriptionRepository.findByUserIdAndStatusIn(anyLong(), any()))
            .thenReturn(Optional.of(testSubscription));
        
        // Act
        boolean result = subscriptionService.isWithinLimits(1L, "users", 1000);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void testUpdateSubscriptionFromStripe() {
        // Arrange
        String stripeSubscriptionId = "sub_test123";
        LocalDateTime now = LocalDateTime.now();
        when(subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId))
            .thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenReturn(testSubscription);
        
        // Act
        subscriptionService.updateSubscriptionFromStripe(
            stripeSubscriptionId, 
            SubscriptionStatus.ACTIVE, 
            now, 
            now.plusMonths(1)
        );
        
        // Assert
        verify(subscriptionRepository).save(any(Subscription.class));
    }
    
    @Test
    void testGetUserSubscriptions() {
        // Arrange
        List<Subscription> subscriptions = List.of(testSubscription);
        when(subscriptionRepository.findByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(subscriptions);
        
        // Act
        List<Subscription> result = subscriptionService.getUserSubscriptions(1L);
        
        // Assert
        assertEquals(1, result.size());
        assertEquals(testSubscription.getId(), result.get(0).getId());
    }
}