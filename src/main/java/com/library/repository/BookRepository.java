package com.library.repository;

import com.library.entity.Book;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    @Query(
            "select distinct b from Book b join b.authors a "
                    + "where lower(concat(concat(a.firstName, ' '), a.lastName)) "
                    + "like lower(concat('%', :author, '%'))"
    )
    List<Book> findByAuthorName(@Param("author") String author);

    @EntityGraph(attributePaths = {"publisher", "authors", "categories"})
    @Query("select distinct b from Book b")
    List<Book> findAllWithGraph();

    @EntityGraph(attributePaths = {"publisher", "authors", "categories"})
    @Query(
            value = "select distinct b from Book b "
                    + "join b.publisher p "
                    + "join b.authors a "
                    + "join b.categories c "
                    + "where (:authorLastName = '' "
                    + "or lower(a.lastName) like concat('%', :authorLastName, '%')) "
                    + "and (:categoryName = '' "
                    + "or lower(c.name) like concat('%', :categoryName, '%')) "
                    + "and (:publisherCountry = '' "
                    + "or lower(p.country) like concat('%', :publisherCountry, '%'))",
            countQuery = "select count(distinct b) from Book b "
                    + "join b.publisher p "
                    + "join b.authors a "
                    + "join b.categories c "
                    + "where (:authorLastName = '' "
                    + "or lower(a.lastName) like concat('%', :authorLastName, '%')) "
                    + "and (:categoryName = '' "
                    + "or lower(c.name) like concat('%', :categoryName, '%')) "
                    + "and (:publisherCountry = '' "
                    + "or lower(p.country) like concat('%', :publisherCountry, '%'))"
    )
    Page<Book> findByFiltersJpql(
            @Param("authorLastName") String authorLastName,
            @Param("categoryName") String categoryName,
            @Param("publisherCountry") String publisherCountry,
            Pageable pageable
    );

    @Query(
            value = "select distinct b.* "
                    + "from books b "
                    + "join publishers p on p.id = b.publisher_id "
                    + "join book_authors ba on ba.book_id = b.id "
                    + "join authors a on a.id = ba.author_id "
                    + "join book_categories bc on bc.book_id = b.id "
                    + "join categories c on c.id = bc.category_id "
                    + "where (:authorLastName = '' "
                    + "or lower(a.last_name) like concat('%', :authorLastName, '%')) "
                    + "and (:categoryName = '' "
                    + "or lower(c.name) like concat('%', :categoryName, '%')) "
                    + "and (:publisherCountry = '' "
                    + "or lower(p.country) like concat('%', :publisherCountry, '%'))",
            countQuery = "select count(distinct b.id) "
                    + "from books b "
                    + "join publishers p on p.id = b.publisher_id "
                    + "join book_authors ba on ba.book_id = b.id "
                    + "join authors a on a.id = ba.author_id "
                    + "join book_categories bc on bc.book_id = b.id "
                    + "join categories c on c.id = bc.category_id "
                    + "where (:authorLastName = '' "
                    + "or lower(a.last_name) like concat('%', :authorLastName, '%')) "
                    + "and (:categoryName = '' "
                    + "or lower(c.name) like concat('%', :categoryName, '%')) "
                    + "and (:publisherCountry = '' "
                    + "or lower(p.country) like concat('%', :publisherCountry, '%'))",
            nativeQuery = true
    )
    Page<Book> findByFiltersNative(
            @Param("authorLastName") String authorLastName,
            @Param("categoryName") String categoryName,
            @Param("publisherCountry") String publisherCountry,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"publisher", "authors", "categories"})
    @Query("select distinct b from Book b where b.id in :ids")
    List<Book> findAllByIdInWithGraph(@Param("ids") List<Long> ids);
}
