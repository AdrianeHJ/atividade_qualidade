package com.example.biblioteca.e2e;

import com.example.biblioteca.entity.Livro;
import com.example.biblioteca.entity.Usuario;
import com.example.biblioteca.repository.LivroRepository;
import com.example.biblioteca.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste E2E com Testcontainers - sobe MongoDB real em container Docker.
 * Execute localmente com Docker disponível.
 * No CI, apenas os testes VCR/Mock rodam (ver ci.yml).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@WithMockUser(username = "teste@email.com", roles = "USER")
class LivroApiE2ETest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        livroRepository.deleteAll();
    }

    @Test
    void deveCriarEBuscarLivro() throws Exception {
        Livro livro = new Livro("O Senhor dos Anéis", "Tolkien", "9780000000001",
                                "Fantasia", 1954, "LIDO", null);

        // Criar
        String response = mockMvc.perform(post("/api/livros")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(livro)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.titulo").value("O Senhor dos Anéis"))
            .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        // Buscar por ID
        mockMvc.perform(get("/api/livros/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.autor").value("Tolkien"));

        assertThat(livroRepository.findAll()).hasSize(1);
    }

    @Test
    void deveFiltrarLivrosPorStatus() throws Exception {
        Livro lido = new Livro("Livro A", "Autor A", null, "Drama", 2020, "LIDO", null);
        Livro lendo = new Livro("Livro B", "Autor B", null, "Drama", 2021, "LENDO", null);
        livroRepository.save(lido);
        livroRepository.save(lendo);

        mockMvc.perform(get("/api/livros/status/LIDO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].titulo").value("Livro A"));
    }

    @Test
    void deveImpedirIsbnDuplicado() throws Exception {
        Livro livro1 = new Livro("Livro Original", "Autor X", "9780000000099",
                                  "Drama", 2020, "LIDO", null);
        livroRepository.save(livro1);

        Livro livro2 = new Livro("Livro Duplicado", "Autor Y", "9780000000099",
                                  "Drama", 2021, "LENDO", null);

        mockMvc.perform(post("/api/livros")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(livro2)))
            .andExpect(status().isConflict());
    }

    @Test
    void deveCriarAtualizarEdeletarLivro() throws Exception {
        Livro livro = new Livro("Fundação", "Isaac Asimov", null, "Ficção Científica", 1951, "QUERO_LER", null);

        // Criar
        String criado = mockMvc.perform(post("/api/livros")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(livro)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(criado).get("id").asText();

        // Atualizar status
        livro.setStatus("LIDO");
        mockMvc.perform(put("/api/livros/" + id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(livro)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("LIDO"));

        // Deletar
        mockMvc.perform(delete("/api/livros/" + id).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(livroRepository.findAll()).isEmpty();
    }
}
