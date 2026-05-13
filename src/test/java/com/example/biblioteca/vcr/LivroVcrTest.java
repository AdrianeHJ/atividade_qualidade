package com.example.biblioteca.vcr;

import com.example.biblioteca.entity.Livro;
import com.example.biblioteca.repository.LivroRepository;
import com.example.biblioteca.service.LivroService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LivroVcrTest {

    @Autowired
    private LivroService livroService;

    @Autowired
    private LivroRepository livroRepository;

    private MockWebServer mockWebServer;
    private OkHttpClient httpClient;

    @BeforeEach
    void setUp() throws IOException {
        livroRepository.deleteAll();
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        httpClient = new OkHttpClient();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void vcrSimulaRespostaGravadaDaApiDeLivros() throws Exception {
        String casseteGravado = """
            [
              {"titulo": "1984", "autor": "George Orwell", "isbn": "9780000000000"},
              {"titulo": "Admirável Mundo Novo", "autor": "Aldous Huxley", "isbn": "9780000000001"}
            ]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setBody(casseteGravado)
            .addHeader("Content-Type", "application/json")
            .setResponseCode(200));

        Request request = new Request.Builder()
            .url(mockWebServer.url("/api/external/livros").toString())
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
            String body = response.body().string();
            assertThat(body).contains("1984");
            assertThat(body).contains("George Orwell");
            assertThat(body).contains("Admirável Mundo Novo");
        }

        RecordedRequest requisicaoRecebida = mockWebServer.takeRequest();
        assertThat(requisicaoRecebida.getPath()).isEqualTo("/api/external/livros");
    }

    @Test
    void vcrPlaybackComDadosReaisNoMongoDB() {
        livroRepository.save(new Livro("1984", "George Orwell", "9780000000000",
                                        "Distopia", 1949, "LIDO", "u1"));
        livroRepository.save(new Livro("Brave New World", "Aldous Huxley", "9780000000001",
                                        "Distopia", 1932, "QUERO_LER", "u1"));

        List<Livro> livros = livroService.getTodosLivros();

        assertThat(livros).hasSize(2);
        assertThat(livros).extracting(Livro::getTitulo)
            .containsExactlyInAnyOrder("1984", "Brave New World");
    }

    @Test
    void vcrSimulaErro404DaApiExterna() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        Request request = new Request.Builder()
            .url(mockWebServer.url("/api/external/livros/isbn/inexistente").toString())
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(404);
        }
    }

    @Test
    void vcrSimulaBuscaIsbnComSucesso() throws Exception {
        String casseteIsbn = """
            {
              "titulo": "O Hobbit",
              "autor": "J.R.R. Tolkien",
              "isbn": "9780618260300",
              "anoPublicacao": 1937
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setBody(casseteIsbn)
            .addHeader("Content-Type", "application/json")
            .setResponseCode(200));

        Request request = new Request.Builder()
            .url(mockWebServer.url("/api/external/isbn/9780618260300").toString())
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
            String body = response.body().string();
            assertThat(body).contains("O Hobbit");
            assertThat(body).contains("Tolkien");
            assertThat(body).contains("9780618260300");
        }

        RecordedRequest requisicaoRecebida = mockWebServer.takeRequest();
        assertThat(requisicaoRecebida.getPath()).isEqualTo("/api/external/isbn/9780618260300");
    }

    @Test
    void vcrSimulaRespostaComCorpoVazio() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("[]"));

        Request request = new Request.Builder()
            .url(mockWebServer.url("/api/external/livros/busca?q=xyz").toString())
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
            assertThat(response.body().string()).isEqualTo("[]");
        }
    }

    @Test
    void vcrBuscaApiExternaEPersisteDadosNoBanco() throws Exception {
        String cassete = """
            {"titulo": "Cem Anos de Solidão", "autor": "Gabriel García Márquez",
             "isbn": "9780000000099", "genero": "Realismo Mágico", "anoPublicacao": 1967}
            """;

        mockWebServer.enqueue(new MockResponse()
            .setBody(cassete)
            .addHeader("Content-Type", "application/json")
            .setResponseCode(200));

        Request request = new Request.Builder()
            .url(mockWebServer.url("/api/external/isbn/9780000000099").toString())
            .build();

        String dadosExternos;
        try (Response response = httpClient.newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
            dadosExternos = response.body().string();
        }

        Livro livroExterno = new Livro("Cem Anos de Solidão", "Gabriel García Márquez",
                                       "9780000000099", "Realismo Mágico", 1967, "QUERO_LER", "u1");
        Livro salvo = livroService.salvarLivro(livroExterno);

        assertThat(dadosExternos).contains("Cem Anos de Solidão");
        assertThat(salvo.getId()).isNotNull();
        assertThat(livroService.existePorIsbn("9780000000099")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"9780618260300", "0618260307", "978-0-13-468599-1"})
    void servicoDeveValidarIsbnValido(String isbnValido) {
        assertThat(livroService.isIsbnValido(isbnValido)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123", "invalid", "978"})
    void servicoDeveInvalidarIsbnInvalido(String isbnInvalido) {
        assertThat(livroService.isIsbnValido(isbnInvalido)).isFalse();
    }
}