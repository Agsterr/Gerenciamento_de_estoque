package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar usuários ativos com paginação pela organização
    @Query("SELECT u FROM Usuario u WHERE u.ativo = true AND u.org.id = :orgId")
    Page<Usuario> findByAtivoTrueAndOrgId(@Param("orgId") Long orgId, Pageable pageable);

    // Atualizar estado do campo "ativo" de um usuário pela organização
    @Modifying
    @Query("UPDATE Usuario u SET u.ativo = :ativo WHERE u.id = :id AND u.org.id = :orgId")
    void atualizarAtivo(@Param("id") Long id, @Param("ativo") Boolean ativo, @Param("orgId") Long orgId);

    // Buscar usuário por nome de usuário e organização
    @Query("SELECT u FROM Usuario u WHERE u.username = :username AND u.org.id = :orgId")
    Optional<Usuario> findByUsernameAndOrgId(@Param("username") String username, @Param("orgId") Long orgId);

    // Buscar usuários por e-mail e organização
    @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.org.id = :orgId")
    Optional<Usuario> findByEmailAndOrgId(@Param("email") String email, @Param("orgId") Long orgId);
}
