package com.example.biblioteca.service;

import com.example.biblioteca.entity.Usuario;
import com.example.biblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    void deveCriarUsuario() {
        Usuario usuario = new Usuario("John", "john@example.com", "password123");
        Usuario salvo = usuarioService.cadastrarUsuario(usuario);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getNome()).isEqualTo("John");
    }

    @Test
    void deveBuscarUsuarioPorEmail() {
        usuarioRepository.save(new Usuario("john", "john@example.com", "password123"));

        Optional<Usuario> usuario = usuarioService.getUsuarioPorEmail("john@example.com");
        assertThat(usuario).isPresent();
        assertThat(usuario.get().getNome()).isEqualTo("john");
    }

    @Test
    void deveRetornarVazioQuandoUsuarioNaoExistir() {
        Optional<Usuario> usuario = usuarioService.getUsuarioPorEmail("inexistente@example.com");
        assertThat(usuario).isEmpty();
    }

    @Test
    void deveSalvarUsuario() {
        Usuario usuario = new Usuario("john", "john@example.com", "password123");
        Usuario salvo = usuarioService.salvarUsuario(usuario);

        assertThat(salvo.getId()).isNotNull();
    }

    @Test
    void deveDeletarUsuario() {
        Usuario usuario = usuarioRepository.save(new Usuario("john", "john@example.com", "password123"));
        usuarioService.deletarUsuario(usuario.getId());

        Optional<Usuario> deletado = usuarioService.getUsuarioPorId(usuario.getId());
        assertThat(deletado).isEmpty();
    }

    @Test
    void deveVerificarExistenciaPorEmail() {
        usuarioRepository.save(new Usuario("john", "john@example.com", "password123"));

        assertThat(usuarioService.existePorEmail("john@example.com")).isTrue();
        assertThat(usuarioService.existePorEmail("inexistente@example.com")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"test@example.com", "user.name@domain.org", "user+tag@example.co.uk", "admin@sub.domain.com"})
    void deveValidarEmailValido(String emailValido) {
        assertThat(usuarioService.isEmailValido(emailValido)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "no-at-sign.com", "missing@domain", "spaces in@email.com"})
    void deveInvalidarEmailInvalido(String emailInvalido) {
        assertThat(usuarioService.isEmailValido(emailInvalido)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", "   "})
    void deveInvalidarEmailNuloOuVazio(String emailInvalido) {
        assertThat(usuarioService.isEmailValido(emailInvalido)).isFalse();
    }
}