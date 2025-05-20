package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar usuários ativos pela organização (orgId é obrigatório)
    @Query("SELECT u FROM Usuario u WHERE u.ativo = true AND u.org.id = :orgId")
    List<Usuario> findByAtivoTrueAndOrgId(@Param("orgId") Long orgId);

    // Atualiza o estado do campo "ativo" de um usuário pela organização
    @Modifying
    @Query("UPDATE Usuario u SET u.ativo = :ativo WHERE u.id = :id AND u.org.id = :orgId")
    void atualizarAtivo(@Param("id") Long id, @Param("ativo") Boolean ativo, @Param("orgId") Long orgId);

    // Buscar usuário por nome de usuário e organização
    @Query("SELECT u FROM Usuario u WHERE u.username = :username AND u.org.id = :orgId")
    Optional<Usuario> findByUsernameAndOrgId(@Param("username") String username, @Param("orgId") Long orgId);

    // Buscar usuários pelo e-mail e organização
    @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.org.id = :orgId")
    Optional<Usuario> findByEmailAndOrgId(@Param("email") String email, @Param("orgId") Long orgId);
}
