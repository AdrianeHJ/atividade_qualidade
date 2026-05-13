package com.example.biblioteca.service;

import com.example.biblioteca.entity.Livro;
import com.example.biblioteca.repository.LivroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LivroServiceTest {

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
        Optional<Livro> livro = livroService.getLivroPorId(livroBase.getId());
        assertThat(livro).isPresent();
        assertThat(livro.get().getTitulo()).isEqualTo("Dom Casmurro");
    }

    @Test
    void deveRetornarOptionalVazioQuandoIdNaoExistir() {
        Optional<Livro> livro = livroService.getLivroPorId("id-inexistente");
        assertThat(livro).isEmpty();
    }

    @Test
    void deveSalvarLivro() {
        Livro novoLivro = new Livro("1984", "George Orwell", "9780000000001",
                                     "Distopia", 1949, "QUERO_LER", "user-1");
        Livro salvo = livroService.salvarLivro(novoLivro);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getTitulo()).isEqualTo("1984");
    }

    @Test
    void deveAtualizarLivro() {
        livroBase.setTitulo("Dom Casmurro - Edição Comentada");
        Livro atualizado = livroService.salvarLivro(livroBase);

        assertThat(atualizado.getTitulo()).isEqualTo("Dom Casmurro - Edição Comentada");
    }

    @Test
    void deveDeletarLivro() {
        livroService.deletarLivro(livroBase.getId());
        Optional<Livro> livro = livroService.getLivroPorId(livroBase.getId());
        assertThat(livro).isEmpty();
    }

    @Test
    void deveVerificarExistenciaPorIsbn() {
        assertThat(livroService.existePorIsbn("9780000000000")).isTrue();
        assertThat(livroService.existePorIsbn("9780000009999")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"9780618260300", "0618260307", "978-0-13-468599-1", "9780321125217"})
    void deveValidarIsbnValido(String isbnValido) {
        assertThat(livroService.isIsbnValido(isbnValido)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "invalid", "short", "tooshorttobevalid"})
    void deveInvalidarIsbnInvalido(String isbnInvalido) {
        assertThat(livroService.isIsbnValido(isbnInvalido)).isFalse();
    }
}