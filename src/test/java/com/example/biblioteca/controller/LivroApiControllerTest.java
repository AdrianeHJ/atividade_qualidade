package com.example.biblioteca.controller;

import com.example.biblioteca.entity.Livro;
import com.example.biblioteca.repository.LivroRepository;
import com.example.biblioteca.repository.UsuarioRepository;
import com.example.biblioteca.service.LivroService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@WithMockUser
class LivroApiControllerTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void configurarPropriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LivroService livroService;

    @Autowired
    private LivroRepository livroRepository;

    private Livro livro;

    @BeforeEach
    void setUp() {
        livroRepository.deleteAll();
        livro = new Livro("1984", "George Orwell", "9780000000000", "Distopia", 1949, "LIDO", "user-1");
        livro = livroRepository.save(livro);
    }

    @Test
    void deveRetornarListaDeLivros() throws Exception {
        mockMvc.perform(get("/api/livros"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].titulo").value("1984"))
            .andExpect(jsonPath("$[0].autor").value("George Orwell"));
    }

    @Test
    void deveRetornarLivroPorId() throws Exception {
        mockMvc.perform(get("/api/livros/" + livro.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titulo").value("1984"));
    }

    @Test
    void deveRetornar404QuandoLivroNaoEncontrado() throws Exception {
        mockMvc.perform(get("/api/livros/inexistente"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deveCriarLivro() throws Exception {
        Livro novoLivro = new Livro("Dom Casmurro", "Machado de Assis", "9780000000001", "Romance", 1899, "LIDO", "user-1");

        mockMvc.perform(post("/api/livros")
                .with(csrf())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(novoLivro)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.titulo").value("Dom Casmurro"));
    }

    @Test
    void deveRetornar409QuandoIsbnDuplicado() throws Exception {
        Livro livroDuplicado = new Livro("1984", "George Orwell", "9780000000000", "Distopia", 1949, "LIDO", "user-1");

        mockMvc.perform(post("/api/livros")
                .with(csrf())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(livroDuplicado)))
            .andExpect(status().isConflict());
    }

    @Test
    void deveAtualizarLivro() throws Exception {
        livro.setTitulo("1984 - Edicao Atualizada");

        mockMvc.perform(put("/api/livros/" + livro.getId())
                .with(csrf())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(livro)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titulo").value("1984 - Edicao Atualizada"));
    }

    @Test
    void deveDeletarLivro() throws Exception {
        mockMvc.perform(delete("/api/livros/" + livro.getId()).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(livroRepository.findById(livro.getId())).isEmpty();
    }

    @Test
    void deveRetornar404AoDeletarLivroInexistente() throws Exception {
        mockMvc.perform(delete("/api/livros/inexistente").with(csrf()))
            .andExpect(status().isNotFound());
    }

    }