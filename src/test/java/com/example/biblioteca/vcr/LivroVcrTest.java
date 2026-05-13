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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes VCR — simulam respostas gravadas de APIs externas usando MockWebServer.
 * Nenhum Mock do Mockito é utilizado: o Testcontainers fornece o MongoDB real
 * e o MockWebServer intercepta as chamadas HTTP externas.
 */
@SpringBootTest
@Testcontainers
class LivroVcrTest {

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

    /**
     * VCR "Recording": simula a resposta gravada de uma API externa de livros.
     * O MockWebServer age como o "cassete" com a resposta pré-gravada.
     */
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

        // Verifica que a requisição chegou corretamente ao servidor
        RecordedRequest requisicaoRecebida = mockWebServer.takeRequest();
        assertThat(requisicaoRecebida.getPath()).isEqualTo("/api/external/livros");
    }

    /**
     * VCR "Playback": dados gravados são usados para popular o banco real
     * e o serviço os consulta normalmente via Testcontainers.
     */
    @Test
    void vcrPlaybackComDadosReaisNoMongoDB() {
        // "Reprodução" do cassete: persiste os dados gravados no MongoDB real
        livroRepository.save(new Livro("1984", "George Orwell", "9780000000000",
                                        "Distopia", 1949, "LIDO", "u1"));
        livroRepository.save(new Livro("Brave New World", "Aldous Huxley", "9780000000001",
                                        "Distopia", 1932, "QUERO_LER", "u1"));

        List<Livro> livros = livroService.getTodosLivros();

        assertThat(livros).hasSize(2);
        assertThat(livros).extracting(Livro::getTitulo)
            .containsExactlyInAnyOrder("1984", "Brave New World");
    }

    /**
     * VCR simula erro HTTP 404 de API externa (livro não encontrado pelo ISBN).
     */
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

    /**
     * VCR simula busca de ISBN retornando dados válidos de API externa.
     */
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

    /**
     * VCR simula timeout/erro de rede da API externa.
     */
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

    /**
     * VCR combinado: consulta API externa e persiste o resultado no MongoDB real.
     */
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

        // Simula chamada à API externa
        Request request = new Request.Builder()
            .url(mockWebServer.url("/api/external/isbn/9780000000099").toString())
            .build();

        String dadosExternos;
        try (Response response = httpClient.newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
            dadosExternos = response.body().string();
        }

        // Persiste os dados recebidos no banco real
        Livro livroExterno = new Livro("Cem Anos de Solidão", "Gabriel García Márquez",
                                       "9780000000099", "Realismo Mágico", 1967, "QUERO_LER", "u1");
        Livro salvo = livroService.salvarLivro(livroExterno);

        assertThat(dadosExternos).contains("Cem Anos de Solidão");
        assertThat(salvo.getId()).isNotNull();
        assertThat(livroService.existePorIsbn("9780000000099")).isTrue();
    }

    // --- Testes de lógica pura do serviço (Caixa Branca) ---

    @Test
    void servicoDeveValidarIsbnCorretamente() {
        assertThat(livroService.isIsbnValido("9780618260300")).isTrue();
        assertThat(livroService.isIsbnValido("0618260307")).isTrue();
        assertThat(livroService.isIsbnValido("abc")).isFalse();
        assertThat(livroService.isIsbnValido("123")).isFalse();
    }
}
