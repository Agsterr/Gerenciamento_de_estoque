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

    // ----------------------------------------------------------------
    // Buscar usuários ativos, paginação e atualização de “ativo” por orgId
    // ----------------------------------------------------------------

    @Query("SELECT u FROM Usuario u WHERE u.ativo = true AND u.org.id = :orgId")
    Page<Usuario> findByAtivoTrueAndOrgId(@Param("orgId") Long orgId, Pageable pageable);

    @Modifying
    @Query("UPDATE Usuario u SET u.ativo = :ativo WHERE u.id = :id AND u.org.id = :orgId")
    void atualizarAtivo(@Param("id") Long id,
                        @Param("ativo") Boolean ativo,
                        @Param("orgId") Long orgId);

    // ----------------------------------------------------------------
    // Atenção: para multitenant, sempre buscar por username + orgId
    // ----------------------------------------------------------------

    @Query("SELECT u FROM Usuario u WHERE u.username = :username AND u.org.id = :orgId")
    Optional<Usuario> findByUsernameAndOrgId(@Param("username") String username,
                                             @Param("orgId") Long orgId);

    // ----------------------------------------------------------------
    // Buscas adicionais (por e-mail + orgId e por ID + orgId)
    // ----------------------------------------------------------------

    @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.org.id = :orgId")
    Optional<Usuario> findByEmailAndOrgId(@Param("email") String email,
                                          @Param("orgId") Long orgId);

    // Busca usuário por email (sem filtro de organização - para autenticação)
    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByIdAndOrgId(Long id, Long orgId);
}
