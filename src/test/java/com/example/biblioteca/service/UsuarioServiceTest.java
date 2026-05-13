package com.example.biblioteca.service;

import com.example.biblioteca.entity.Usuario;
import com.example.biblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class UsuarioServiceTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void configurarPropriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    void deveCadastrarUsuarioComSucesso() {
        Usuario usuario = new Usuario("João Silva", "joao@email.com", "senha123");

        Usuario salvo = usuarioService.cadastrarUsuario(usuario);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getNome()).isEqualTo("João Silva");
        // Confirma que a senha foi armazenada com hash (não em texto puro)
        assertThat(salvo.getSenha()).isNotEqualTo("senha123");
        assertThat(salvo.getSenha()).startsWith("$2a$");
    }

    @Test
    void deveLancarExcecaoQuandoEmailJaCadastrado() {
        Usuario primeiro = new Usuario("João Silva", "joao@email.com", "senha123");
        usuarioService.cadastrarUsuario(primeiro);

        Usuario duplicado = new Usuario("João Outro", "joao@email.com", "outrasenha");

        assertThatThrownBy(() -> usuarioService.cadastrarUsuario(duplicado))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("E-mail já cadastrado");
    }

    @Test
    void deveLancarExcecaoQuandoEmailInvalido() {
        Usuario usuario = new Usuario("Maria", "nao-eh-email", "senha123");

        assertThatThrownBy(() -> usuarioService.cadastrarUsuario(usuario))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("E-mail inválido");
    }

    @Test
    void deveRetornarUsuarioPorEmail() {
        Usuario usuario = new Usuario("Maria Souza", "maria@email.com", "senha456");
        usuarioService.cadastrarUsuario(usuario);

        Optional<Usuario> resultado = usuarioService.getUsuarioPorEmail("maria@email.com");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Maria Souza");
    }

    @Test
    void deveRetornarVazioParaEmailNaoCadastrado() {
        Optional<Usuario> resultado = usuarioService.getUsuarioPorEmail("naoexiste@email.com");

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveVerificarExistenciaPorEmail() {
        Usuario usuario = new Usuario("Carlos", "carlos@email.com", "senha789");
        usuarioService.cadastrarUsuario(usuario);

        assertThat(usuarioService.existePorEmail("carlos@email.com")).isTrue();
        assertThat(usuarioService.existePorEmail("outro@email.com")).isFalse();
    }

    @Test
    void deveDeletarUsuario() {
        Usuario usuario = new Usuario("Ana", "ana@email.com", "senha000");
        Usuario salvo = usuarioService.cadastrarUsuario(usuario);

        usuarioService.deletarUsuario(salvo.getId());

        assertThat(usuarioRepository.findById(salvo.getId())).isEmpty();
    }

    @Test
    void deveRetornarUsuarioPorId() {
        Usuario usuario = new Usuario("Pedro", "pedro@email.com", "senha111");
        Usuario salvo = usuarioService.cadastrarUsuario(usuario);

        Optional<Usuario> resultado = usuarioService.getUsuarioPorId(salvo.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Pedro");
    }

    // --- Testes de lógica pura (Caixa Branca) — sem persistência ---

    @Test
    void deveValidarEmailsCorretos() {
        assertThat(usuarioService.isEmailValido("valido@email.com")).isTrue();
        assertThat(usuarioService.isEmailValido("nome.sobrenome@dominio.com.br")).isTrue();
        assertThat(usuarioService.isEmailValido("user+tag@exemplo.org")).isTrue();
    }

    @Test
    void deveRejetarEmailsInvalidos() {
        assertThat(usuarioService.isEmailValido("invalido")).isFalse();
        assertThat(usuarioService.isEmailValido("sem@dominio")).isFalse();
        assertThat(usuarioService.isEmailValido(null)).isFalse();
        assertThat(usuarioService.isEmailValido("")).isFalse();
    }
}
