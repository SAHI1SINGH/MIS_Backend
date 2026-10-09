package com.tadobasolutions.repository;

import com.tadobasolutions.entity.Notice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NoticeRepository extends MongoRepository<Notice, String> {
    List<Notice> findAllByOrderByCreatedAtDesc();
}
