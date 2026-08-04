package com.anticucheria.controller;

import com.anticucheria.dto.mapper.CatalogoMapper;
import com.anticucheria.dto.request.LoginRequest;
import com.anticucheria.dto.response.LoginResponse;
import com.anticucheria.dto.response.UsuarioResponse;
import com.anticucheria.exception.ResourceNotFoundException;
import com.anticucheria.model.Usuario;
import com.anticucheria.repository.UsuarioRepository;
import com.anticucheria.security.JwtUtil;
import com.anticucheria.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsuario(), request.getPassword()));

        Usuario usuario = usuarioRepository.findByUsuario(request.getUsuario())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return LoginResponse.builder()
                .token(jwtUtil.generateToken(usuario))
                .usuario(CatalogoMapper.toUsuarioResponse(usuario))
                .build();
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UserDetails user) {
        return usuarioService.obtenerPorUsername(user.getUsername());
    }
}
