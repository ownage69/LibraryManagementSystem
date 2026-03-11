package com.library.repository;

import com.library.entity.Author;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(
            String firstName,
            String lastName
    );
}
