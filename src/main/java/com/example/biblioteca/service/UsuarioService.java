package com.example.biblioteca.service;

import com.example.biblioteca.entity.Usuario;
import com.example.biblioteca.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Usuario> getTodosUsuarios() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> getUsuarioPorId(String id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> getUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public Usuario cadastrarUsuario(Usuario usuario) {
        if (!isEmailValido(usuario.getEmail())) {
            throw new IllegalArgumentException("E-mail inválido");
        }
        if (existePorEmail(usuario.getEmail())) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        // Hash da senha antes de salvar
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return usuarioRepository.save(usuario);
    }

    public Usuario salvarUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    public void deletarUsuario(String id) {
        usuarioRepository.deleteById(id);
    }

    public boolean existePorEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    public boolean isEmailValido(String email) {
        if (email == null || email.isEmpty()) return false;
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
