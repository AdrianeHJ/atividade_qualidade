package com.example.biblioteca.controller.api;

import com.example.biblioteca.entity.Livro;
import com.example.biblioteca.service.LivroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/livros")
@CrossOrigin(origins = "*")
public class LivroApiController {

    @Autowired
    private LivroService livroService;

    @GetMapping
    public ResponseEntity<List<Livro>> getTodos() {
        return ResponseEntity.ok(livroService.getTodosLivros());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Livro> getPorId(@PathVariable String id) {
        Optional<Livro> livro = livroService.getLivroPorId(id);
        return livro.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Livro> criar(@RequestBody Livro livro) {
        if (livro.getIsbn() != null && !livro.getIsbn().isBlank()
                && livroService.existePorIsbn(livro.getIsbn())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Livro salvo = livroService.salvarLivro(livro);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Livro> atualizar(@PathVariable String id, @RequestBody Livro livro) {
        if (livroService.getLivroPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        livro.setId(id);
        return ResponseEntity.ok(livroService.salvarLivro(livro));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        if (livroService.getLivroPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        livroService.deletarLivro(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Livro>> getPorStatus(@PathVariable String status) {
        return ResponseEntity.ok(livroService.getLivrosPorStatus(status));
    }
}
