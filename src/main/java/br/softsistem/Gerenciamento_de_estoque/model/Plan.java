package br.softsistem.Gerenciamento_de_estoque.model;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.PlanType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidade que representa um plano de assinatura
 */
@Entity
@Table(name = "plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome do plano é obrigatório")
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @NotNull(message = "Preço é obrigatório")
    @Positive(message = "Preço deve ser positivo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @NotNull(message = "Tipo do plano é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType type;
    
    @Column(name = "stripe_price_id", unique = true)
    private String stripePriceId;
    
    @Column(name = "stripe_product_id", unique = true)
    private String stripeProductId;
    
    @Column(name = "max_users")
    private Integer maxUsers;
    
    @Column(name = "max_products")
    private Integer maxProducts;
    
    @Column(name = "max_organizations")
    private Integer maxOrganizations;
    
    @Column(name = "has_reports")
    private Boolean hasReports = false;
    
    @Column(name = "has_advanced_analytics")
    private Boolean hasAdvancedAnalytics = false;
    
    @Column(name = "has_api_access")
    private Boolean hasApiAccess = false;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Subscription> subscriptions;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters explícitos para garantir compatibilidade quando o processamento de anotações estiver desabilitado
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public PlanType getType() { return type; }
    public Integer getMaxUsers() { return maxUsers; }
    public Integer getMaxProducts() { return maxProducts; }
    public Integer getMaxOrganizations() { return maxOrganizations; }
    public Boolean getHasReports() { return hasReports; }
    public Boolean getHasAdvancedAnalytics() { return hasAdvancedAnalytics; }
    public Boolean getHasApiAccess() { return hasApiAccess; }
    public Boolean getIsActive() { return isActive; }
    
    // Getters e Setters explícitos para integrações Stripe
    public String getStripePriceId() { return stripePriceId; }
    public void setStripePriceId(String stripePriceId) { this.stripePriceId = stripePriceId; }
    
    public String getStripeProductId() { return stripeProductId; }
    public void setStripeProductId(String stripeProductId) { this.stripeProductId = stripeProductId; }
}