package com.pitso.controller;

import com.pitso.model.BookDtos.BookResponse;
import com.pitso.model.BookDtos.CreateBookRequest;
import com.pitso.model.BookDtos.UpdateBookRequest;
import com.pitso.service.BookService;
import com.pitso.service.CatalogIntelligenceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Server-rendered Thymeleaf frontend for the Library Intelligence application.
 * It reuses the existing Java service layer, so the UI and REST API share the
 * same validation and business rules.
 */
@Controller
public class LibraryWebController {

    private static final int PAGE_SIZE = 10;

    private final BookService bookService;
    private final CatalogIntelligenceService catalogIntelligenceService;

    public LibraryWebController(
            BookService bookService,
            CatalogIntelligenceService catalogIntelligenceService) {
        this.bookService = bookService;
        this.catalogIntelligenceService = catalogIntelligenceService;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", bookService.getInventoryStats());
        model.addAttribute("insights", catalogIntelligenceService.getInsights());
        model.addAttribute("recommendations",
            catalogIntelligenceService.recommend("", "ANY", 4));
        return "dashboard";
    }

    @GetMapping("/books")
    public String books(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        int safePage = Math.max(page, 0);
        PageRequest pageable = PageRequest.of(
            safePage,
            PAGE_SIZE,
            Sort.by("title").ascending()
        );

        Page<BookResponse> books = q == null || q.isBlank()
            ? bookService.getAllBooks(pageable)
            : bookService.searchBooks(q.trim(), pageable);

        model.addAttribute("books", books);
        model.addAttribute("q", q == null ? "" : q.trim());
        return "books";
    }

    @GetMapping("/books/new")
    public String newBook(Model model) {
        if (!model.containsAttribute("bookForm")) {
            model.addAttribute("bookForm", new CreateBookRequest());
        }
        model.addAttribute("mode", "create");
        return "book-form";
    }

    @PostMapping("/books")
    public String createBook(
            @Valid @ModelAttribute("bookForm") CreateBookRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            return "book-form";
        }

        try {
            BookResponse created = bookService.createBook(request);
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Added \"" + created.getTitle() + "\" to the library."
            );
            return "redirect:/books";
        } catch (RuntimeException ex) {
            bindingResult.reject("book.create", ex.getMessage());
            model.addAttribute("mode", "create");
            return "book-form";
        }
    }

    @GetMapping("/books/{id}/edit")
    public String editBook(@PathVariable Long id, Model model) {
        BookResponse book = bookService.getBookById(id);
        UpdateBookRequest form = new UpdateBookRequest();
        form.setTitle(book.getTitle());
        form.setAuthor(book.getAuthor());
        form.setFileSizeKb(book.getFileSizeKb());
        form.setNoOfPages(book.getNoOfPages());
        form.setWeightGrams(book.getWeightGrams());

        model.addAttribute("book", book);
        model.addAttribute("bookForm", form);
        model.addAttribute("mode", "edit");
        return "book-form";
    }

    @PostMapping("/books/{id}")
    public String updateBook(
            @PathVariable Long id,
            @Valid @ModelAttribute("bookForm") UpdateBookRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        BookResponse current = bookService.getBookById(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("book", current);
            model.addAttribute("mode", "edit");
            return "book-form";
        }

        try {
            BookResponse updated = bookService.updateBook(id, request);
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Updated \"" + updated.getTitle() + "\"."
            );
            return "redirect:/books";
        } catch (RuntimeException ex) {
            bindingResult.reject("book.update", ex.getMessage());
            model.addAttribute("book", current);
            model.addAttribute("mode", "edit");
            return "book-form";
        }
    }

    @PostMapping("/books/{id}/delete")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        BookResponse book = bookService.getBookById(id);
        bookService.deleteBook(id);
        redirectAttributes.addFlashAttribute(
            "successMessage",
            "Deleted \"" + book.getTitle() + "\" from the library."
        );
        return "redirect:/books";
    }

    @GetMapping("/catalog")
    public String catalog(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "ANY") String preferredType,
            Model model) {

        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("preferredType", preferredType);
        model.addAttribute("insights", catalogIntelligenceService.getInsights());
        model.addAttribute(
            "recommendations",
            catalogIntelligenceService.recommend(q, preferredType, 8)
        );
        return "catalog";
    }
}
