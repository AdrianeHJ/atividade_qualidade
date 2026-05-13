package com.example.biblioteca.controller;

import com.example.biblioteca.entity.Livro;
import com.example.biblioteca.service.LivroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/livros")
public class LivroController {

    @Autowired
    private LivroService livroService;

    @GetMapping
    public String listarLivros(Model model,
                               @AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(required = false) String busca,
                               @RequestParam(required = false) String status) {
        List<Livro> livros;

        if (busca != null && !busca.isBlank()) {
            livros = livroService.buscarPorTitulo(busca);
            if (livros.isEmpty()) livros = livroService.buscarPorAutor(busca);
        } else if (status != null && !status.isBlank()) {
            livros = livroService.getLivrosPorStatus(status);
        } else {
            livros = livroService.getTodosLivros();
        }

        model.addAttribute("livros", livros);
        model.addAttribute("busca", busca);
        model.addAttribute("statusFiltro", status);
        return "livro/lista";
    }

    @GetMapping("/novo")
    public String exibirFormNovo(Model model) {
        model.addAttribute("livro", new Livro());
        return "livro/form";
    }

    @PostMapping
    public String criarLivro(@ModelAttribute Livro livro,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes ra) {
        // Poderia associar ao usuário logado: livro.setProprietarioId(...)
        livroService.salvarLivro(livro);
        ra.addFlashAttribute("sucesso", "Livro adicionado com sucesso!");
        return "redirect:/livros";
    }

    @GetMapping("/{id}/editar")
    public String exibirFormEditar(@PathVariable String id, Model model) {
        Livro livro = livroService.getLivroPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado: " + id));
        model.addAttribute("livro", livro);
        return "livro/form";
    }

    @PostMapping("/{id}")
    public String atualizarLivro(@PathVariable String id, @ModelAttribute Livro livro,
                                  RedirectAttributes ra) {
        livro.setId(id);
        livroService.salvarLivro(livro);
        ra.addFlashAttribute("sucesso", "Livro atualizado!");
        return "redirect:/livros";
    }

    @GetMapping("/{id}/deletar")
    public String deletarLivro(@PathVariable String id, RedirectAttributes ra) {
        livroService.deletarLivro(id);
        ra.addFlashAttribute("sucesso", "Livro removido!");
        return "redirect:/livros";
    }
}
