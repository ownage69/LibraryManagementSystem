package com.library.repository;

import com.library.entity.Book;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query(
            "select distinct b from Book b join b.authors a "
                    + "where lower(concat(concat(a.firstName, ' '), a.lastName)) "
                    + "like lower(concat('%', :author, '%'))"
    )
    List<Book> findByAuthorName(@Param("author") String author);

    @EntityGraph(attributePaths = {"publisher", "authors", "categories"})
    @Query("select distinct b from Book b")
    List<Book> findAllWithGraph();
}
