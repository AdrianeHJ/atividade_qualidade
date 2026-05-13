package com.example.biblioteca.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "livros")
public class Livro {

    @Id
    private String id;

    private String titulo;
    private String autor;
    private String isbn;
    private String genero;
    private Integer anoPublicacao;
    private String status; // "LIDO", "LENDO", "QUERO_LER"
    private String proprietarioId; // referência ao usuário dono
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Livro() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
        this.status = "QUERO_LER";
    }

    public Livro(String titulo, String autor, String isbn, String genero,
                 Integer anoPublicacao, String status, String proprietarioId) {
        this();
        this.titulo = titulo;
        this.autor = autor;
        this.isbn = isbn;
        this.genero = genero;
        this.anoPublicacao = anoPublicacao;
        this.status = status;
        this.proprietarioId = proprietarioId;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) {
        this.titulo = titulo;
        this.atualizadoEm = LocalDateTime.now();
    }

    public String getAutor() { return autor; }
    public void setAutor(String autor) {
        this.autor = autor;
        this.atualizadoEm = LocalDateTime.now();
    }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) {
        this.isbn = isbn;
        this.atualizadoEm = LocalDateTime.now();
    }

    public String getGenero() { return genero; }
    public void setGenero(String genero) {
        this.genero = genero;
        this.atualizadoEm = LocalDateTime.now();
    }

    public Integer getAnoPublicacao() { return anoPublicacao; }
    public void setAnoPublicacao(Integer anoPublicacao) {
        this.anoPublicacao = anoPublicacao;
        this.atualizadoEm = LocalDateTime.now();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.atualizadoEm = LocalDateTime.now();
    }

    public String getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(String proprietarioId) {
        this.proprietarioId = proprietarioId;
        this.atualizadoEm = LocalDateTime.now();
    }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
