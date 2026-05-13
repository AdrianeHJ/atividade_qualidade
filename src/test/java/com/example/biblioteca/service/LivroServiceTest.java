package com.example.biblioteca.service;

import com.example.biblioteca.entity.Livro;
import com.example.biblioteca.repository.LivroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class LivroServiceTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void configurarPropriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private LivroService livroService;

    @Autowired
    private LivroRepository livroRepository;

    private Livro livroBase;

    @BeforeEach
    void setUp() {
        livroRepository.deleteAll();
        livroBase = livroRepository.save(
            new Livro("Dom Casmurro", "Machado de Assis", "9780000000000",
                      "Romance", 1899, "LIDO", "user-1")
        );
    }

    @Test
    void deveRetornarTodosLivros() {
        List<Livro> livros = livroService.getTodosLivros();

        assertThat(livros).hasSize(1);
        assertThat(livros.get(0).getTitulo()).isEqualTo("Dom Casmurro");
    }

    @Test
    void deveRetornarLivroPorId() {
        Optional<Livro> resultado = livroService.getLivroPorId(livroBase.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getAutor()).isEqualTo("Machado de Assis");
    }

    @Test
    void deveRetornarVazioQuandoIdNaoExiste() {
        Optional<Livro> resultado = livroService.getLivroPorId("id-inexistente");

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveSalvarLivro() {
        Livro novoLivro = new Livro("O Cortiço", "Aluísio Azevedo", "9780000000001",
                                    "Realismo", 1890, "QUERO_LER", "user-1");

        Livro salvo = livroService.salvarLivro(novoLivro);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getTitulo()).isEqualTo("O Cortiço");
        assertThat(livroRepository.findAll()).hasSize(2);
    }

    @Test
    void deveDeletarLivro() {
        livroService.deletarLivro(livroBase.getId());

        assertThat(livroRepository.findById(livroBase.getId())).isEmpty();
        assertThat(livroRepository.findAll()).isEmpty();
    }

    @Test
    void deveVerificarExistenciaPorIsbn() {
        assertThat(livroService.existePorIsbn("9780000000000")).isTrue();
        assertThat(livroService.existePorIsbn("0000000000000")).isFalse();
    }

    @Test
    void deveRetornarLivrosPorStatus() {
        livroRepository.save(
            new Livro("Memórias Póstumas", "Machado de Assis", "9780000000002",
                      "Romance", 1881, "LENDO", "user-1")
        );

        List<Livro> lidos = livroService.getLivrosPorStatus("LIDO");
        List<Livro> lendo = livroService.getLivrosPorStatus("LENDO");

        assertThat(lidos).hasSize(1);
        assertThat(lidos.get(0).getTitulo()).isEqualTo("Dom Casmurro");
        assertThat(lendo).hasSize(1);
        assertThat(lendo.get(0).getTitulo()).isEqualTo("Memórias Póstumas");
    }

    @Test
    void deveBuscarPorTitulo() {
        List<Livro> resultado = livroService.buscarPorTitulo("dom");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTitulo()).isEqualTo("Dom Casmurro");
    }

    @Test
    void deveBuscarPorAutor() {
        List<Livro> resultado = livroService.buscarPorAutor("machado");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getAutor()).isEqualTo("Machado de Assis");
    }

    @Test
    void deveRetornarLivrosPorProprietario() {
        livroRepository.save(
            new Livro("A Moreninha", "Joaquim Macedo", "9780000000003",
                      "Romance", 1844, "QUERO_LER", "user-2")
        );

        List<Livro> livrosUser1 = livroService.getLivrosPorProprietario("user-1");
        List<Livro> livrosUser2 = livroService.getLivrosPorProprietario("user-2");

        assertThat(livrosUser1).hasSize(1);
        assertThat(livrosUser2).hasSize(1);
        assertThat(livrosUser2.get(0).getTitulo()).isEqualTo("A Moreninha");
    }

    // --- Testes de lógica pura (Caixa Branca) — sem persistência ---

    @Test
    void deveValidarIsbnValido13Digitos() {
        assertThat(livroService.isIsbnValido("9780000000000")).isTrue();
    }

    @Test
    void deveValidarIsbnValido10Digitos() {
        assertThat(livroService.isIsbnValido("0000000000")).isTrue();
    }

    @Test
    void deveAceitarIsbnNuloOuVazio() {
        assertThat(livroService.isIsbnValido(null)).isTrue();
        assertThat(livroService.isIsbnValido("")).isTrue();
    }

    @Test
    void deveRejetarIsbnInvalido() {
        assertThat(livroService.isIsbnValido("123")).isFalse();
        assertThat(livroService.isIsbnValido("abc")).isFalse();
    }
}
