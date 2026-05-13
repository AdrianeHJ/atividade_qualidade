package com.example.biblioteca.controller;

import com.example.biblioteca.entity.Livro;
import com.example.biblioteca.service.LivroService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = com.example.biblioteca.controller.api.LivroApiController.class)
@Import(com.example.biblioteca.SecurityConfig.class)
@WithMockUser
class LivroApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LivroService livroService;

    @MockBean
    private com.example.biblioteca.repository.UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Livro livro;

    @BeforeEach
    void setUp() {
        livro = new Livro("1984", "George Orwell", "9780000000000", "Distopia", 1949, "LIDO", "user-1");
        livro.setId("livro-1");
    }

    @Test
    void deveRetornarListaDeLivros() throws Exception {
        when(livroService.getTodosLivros()).thenReturn(List.of(livro));

        mockMvc.perform(get("/api/livros"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].titulo").value("1984"))
            .andExpect(jsonPath("$[0].autor").value("George Orwell"));
    }

    @Test
    void deveRetornarLivroPorId() throws Exception {
        when(livroService.getLivroPorId("livro-1")).thenReturn(Optional.of(livro));

        mockMvc.perform(get("/api/livros/livro-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titulo").value("1984"));
    }

    @Test
    void deveRetornar404QuandoLivroNaoEncontrado() throws Exception {
        when(livroService.getLivroPorId("inexistente")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/livros/inexistente"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deveCriarLivro() throws Exception {
        when(livroService.existePorIsbn(any())).thenReturn(false);
        when(livroService.salvarLivro(any(Livro.class))).thenReturn(livro);

        mockMvc.perform(post("/api/livros")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(livro)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.titulo").value("1984"));
    }

    @Test
    void deveRetornar409QuandoIsbnDuplicado() throws Exception {
        when(livroService.existePorIsbn("9780000000000")).thenReturn(true);

        mockMvc.perform(post("/api/livros")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(livro)))
            .andExpect(status().isConflict());
    }

    @Test
    void deveAtualizarLivro() throws Exception {
        when(livroService.getLivroPorId("livro-1")).thenReturn(Optional.of(livro));
        when(livroService.salvarLivro(any(Livro.class))).thenReturn(livro);

        mockMvc.perform(put("/api/livros/livro-1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(livro)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titulo").value("1984"));
    }

    @Test
    void deveDeletarLivro() throws Exception {
        when(livroService.getLivroPorId("livro-1")).thenReturn(Optional.of(livro));
        doNothing().when(livroService).deletarLivro("livro-1");

        mockMvc.perform(delete("/api/livros/livro-1").with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    void deveRetornar404AoDeletarLivroInexistente() throws Exception {
        when(livroService.getLivroPorId("inexistente")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/livros/inexistente").with(csrf()))
            .andExpect(status().isNotFound());
    }
}
