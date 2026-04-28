package com.seguridad.Messenger.usuario.repository;

import com.seguridad.Messenger.usuario.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByIdAndActivoTrue(UUID id);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("""
            SELECT u FROM Usuario u
            JOIN FETCH u.persona p
            WHERE u.activo = true
              AND (
                LOWER(u.username)   LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.nombres)  LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Usuario> buscar(@Param("q") String q, Pageable pageable);

    /**
     * Devuelve solo el username de un usuario — usado por el registro de presencia
     * para construir el payload sin cargar la entidad completa.
     */
    @Query("SELECT u.username FROM Usuario u WHERE u.id = :id")
    String findUsernameById(@Param("id") UUID id);
}
