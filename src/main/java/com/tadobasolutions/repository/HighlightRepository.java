package com.tadobasolutions.repository;

import com.tadobasolutions.entity.Highlight;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface HighlightRepository extends MongoRepository<Highlight, String> {
    List<Highlight> findAllByOrderByCreatedAtDesc();
}
