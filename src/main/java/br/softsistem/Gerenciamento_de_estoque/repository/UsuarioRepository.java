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

    // Busca usuários ativos
    List<Usuario> findByAtivoTrue();

    // Atualiza o estado do campo "ativo"
    @Modifying
    @Query("UPDATE Usuario u SET u.ativo = :ativo WHERE u.id = :id")
    void atualizarAtivo(@Param("id") Long id, @Param("ativo") Boolean ativo);


    // Buscar usuário por nome de usuário
    Optional<Usuario> findByUsername(String username);


    // Buscar usuários pelo e-mail
    Optional<Usuario> findByEmail(String email);
}