package com.anticucheria.service.impl;

import com.anticucheria.dto.mapper.CatalogoMapper;
import com.anticucheria.dto.request.ActualizarUsuarioRequest;
import com.anticucheria.dto.request.CambiarPasswordRequest;
import com.anticucheria.dto.request.CrearUsuarioRequest;
import com.anticucheria.dto.response.UsuarioResponse;
import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.exception.ResourceNotFoundException;
import com.anticucheria.model.Usuario;
import com.anticucheria.model.enums.Rol;
import com.anticucheria.repository.UsuarioRepository;
import com.anticucheria.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(boolean incluirInactivos) {
        List<Usuario> usuarios = incluirInactivos
                ? usuarioRepository.findAllByOrderByNombreAsc()
                : usuarioRepository.findByActivoTrueOrderByNombreAsc();
        return usuarios.stream().map(CatalogoMapper::toUsuarioResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return CatalogoMapper.toUsuarioResponse(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
        return CatalogoMapper.toUsuarioResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        if (usuarioRepository.existsByUsuario(request.getUsuario())) {
            throw new ReglaNegocioException("Ese nombre de usuario ya está en uso.");
        }
        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .usuario(request.getUsuario())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(request.getRol())
                .activo(true)
                .build();
        return CatalogoMapper.toUsuarioResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = buscar(id);

        boolean degradaUltimoAdmin = usuario.getRol() == Rol.ADMIN
                && request.getRol() != Rol.ADMIN
                && Boolean.TRUE.equals(usuario.getActivo())
                && usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) <= 1;
        if (degradaUltimoAdmin) {
            throw new ReglaNegocioException("Debe haber al menos un administrador.");
        }

        usuario.setNombre(request.getNombre());
        usuario.setRol(request.getRol());
        return CatalogoMapper.toUsuarioResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void cambiarPassword(Long id, CambiarPasswordRequest request) {
        Usuario usuario = buscar(id);
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo, String usernameActual) {
        Usuario usuario = buscar(id);

        if (!activo) {
            if (usuario.getUsuario().equals(usernameActual)) {
                throw new ReglaNegocioException("No puedes quitarte el acceso a ti mismo.");
            }
            if (usuario.getRol() == Rol.ADMIN
                    && Boolean.TRUE.equals(usuario.getActivo())
                    && usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) <= 1) {
                throw new ReglaNegocioException("Debe haber al menos un administrador.");
            }
        }

        usuario.setActivo(activo);
        return CatalogoMapper.toUsuarioResponse(usuarioRepository.save(usuario));
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }
}
