package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.CardHistory;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para operações com histórico de cartões
 */
@Repository
public interface CardHistoryRepository extends JpaRepository<CardHistory, Long> {
    
    /**
     * Busca histórico de cartões por assinatura
     */
    List<CardHistory> findBySubscriptionOrderByUpdatedAtDesc(Subscription subscription);
    
    /**
     * Busca histórico de cartões por card_id
     */
    List<CardHistory> findByNewCardIdOrderByUpdatedAtDesc(String cardId);
}







