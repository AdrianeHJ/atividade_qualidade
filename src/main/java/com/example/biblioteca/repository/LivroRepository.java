package com.example.biblioteca.repository;

import com.example.biblioteca.entity.Livro;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LivroRepository extends MongoRepository<Livro, String> {
    boolean existsByIsbn(String isbn);
    List<Livro> findByProprietarioId(String proprietarioId);
    List<Livro> findByStatus(String status);
    List<Livro> findByAutorContainingIgnoreCase(String autor);
    List<Livro> findByTituloContainingIgnoreCase(String titulo);
}
