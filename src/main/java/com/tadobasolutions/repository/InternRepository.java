package com.tadobasolutions.repository;

import com.tadobasolutions.entity.Intern;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface InternRepository extends MongoRepository<Intern, String> {

    Optional<Intern> findByEmail(String email);
}
