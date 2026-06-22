package com.pitso.repository;

import com.pitso.model.Book;
import com.pitso.model.EBook;
import com.pitso.model.PrintBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for all book types.
 * Spring Data JPA handles the JOINED inheritance automatically —
 * findAll() returns both EBooks and PrintBooks polymorphically.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbnNo(String isbnNo);

    boolean existsByIsbnNo(String isbnNo);

    // Search by title or author (case-insensitive)
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Book> searchBooks(@Param("query") String query, Pageable pageable);

    // Fetch only EBooks
    @Query("SELECT e FROM EBook e")
    List<EBook> findAllEBooks();

    // Fetch only PrintBooks
    @Query("SELECT p FROM PrintBook p")
    List<PrintBook> findAllPrintBooks();

    // Count by type
    @Query("SELECT COUNT(e) FROM EBook e")
    long countEBooks();

    @Query("SELECT COUNT(p) FROM PrintBook p")
    long countPrintBooks();

    // Search within a specific book type
    @Query("SELECT e FROM EBook e WHERE " +
           "LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.author) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<EBook> searchEBooks(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM PrintBook p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.author) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<PrintBook> searchPrintBooks(@Param("query") String query, Pageable pageable);
}
