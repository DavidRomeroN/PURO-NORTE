package com.anticucheria.repository;

import com.anticucheria.model.Usuario;
import com.anticucheria.model.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsuario(String usuario);

    boolean existsByUsuario(String usuario);

    List<Usuario> findByActivoTrueOrderByNombreAsc();

    List<Usuario> findAllByOrderByNombreAsc();

    long countByRolAndActivoTrue(Rol rol);
}
