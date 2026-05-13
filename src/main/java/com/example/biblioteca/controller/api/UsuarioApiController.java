package com.example.biblioteca.controller.api;

import com.example.biblioteca.entity.Usuario;
import com.example.biblioteca.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioApiController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<Usuario>> getTodos() {
        return ResponseEntity.ok(usuarioService.getTodosUsuarios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> getPorId(@PathVariable String id) {
        Optional<Usuario> usuario = usuarioService.getUsuarioPorId(id);
        return usuario.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody Usuario usuario) {
        try {
            Usuario salvo = usuarioService.cadastrarUsuario(usuario);
            // Não retornar a senha no response
            salvo.setSenha(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        if (usuarioService.getUsuarioPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        usuarioService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
