package com.example.biblioteca.service;

import com.example.biblioteca.entity.Livro;
import com.example.biblioteca.repository.LivroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LivroService {

    @Autowired
    private LivroRepository livroRepository;

    public List<Livro> getTodosLivros() {
        return livroRepository.findAll();
    }

    public List<Livro> getLivrosPorProprietario(String proprietarioId) {
        return livroRepository.findByProprietarioId(proprietarioId);
    }

    public List<Livro> getLivrosPorStatus(String status) {
        return livroRepository.findByStatus(status);
    }

    public Optional<Livro> getLivroPorId(String id) {
        return livroRepository.findById(id);
    }

    public Livro salvarLivro(Livro livro) {
        return livroRepository.save(livro);
    }

    public void deletarLivro(String id) {
        livroRepository.deleteById(id);
    }

    public boolean existePorIsbn(String isbn) {
        return livroRepository.existsByIsbn(isbn);
    }

    public boolean isIsbnValido(String isbn) {
        if (isbn == null || isbn.isEmpty()) return true; // ISBN opcional
        String limpo = isbn.replaceAll("[^0-9X]", "");
        return limpo.length() == 10 || limpo.length() == 13;
    }

    public List<Livro> buscarPorTitulo(String titulo) {
        return livroRepository.findByTituloContainingIgnoreCase(titulo);
    }

    public List<Livro> buscarPorAutor(String autor) {
        return livroRepository.findByAutorContainingIgnoreCase(autor);
    }
}
