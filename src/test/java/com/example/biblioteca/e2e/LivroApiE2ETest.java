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
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WithMockUser(username = "teste@email.com", roles = "USER")
class LivroApiE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        livroRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveExecutarFluxoCompletoDeLivros() throws Exception {
        Usuario usuario = new Usuario("testuser", "teste@email.com", "password");
        usuarioRepository.save(usuario);

        Livro livro = new Livro("1984", "George Orwell", "9780000000000",
                                "Distopia", 1949, "QUERO_LER", usuario.getId());

        mockMvc.perform(post("/api/livros")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(livro)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.titulo").value("1984"));

        mockMvc.perform(get("/api/livros"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].titulo").value("1984"));

        String livroId = livroRepository.findAll().get(0).getId();

        mockMvc.perform(delete("/api/livros/" + livroId).with(csrf()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/livros/" + livroId))
            .andExpect(status().isNotFound());
    }

    @Test
    void deveValidarIsbnDuplicado() throws Exception {
        Livro livro1 = new Livro("1984", "George Orwell", "9780000000000",
                                 "Distopia", 1949, "LIDO", "user-1");
        livroRepository.save(livro1);

        Livro livro2 = new Livro("1984 Copy", "George Orwell", "9780000000000",
                                 "Distopia", 1949, "QUERO_LER", "user-1");

        mockMvc.perform(post("/api/livros")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(livro2)))
            .andExpect(status().isConflict());
    }
}