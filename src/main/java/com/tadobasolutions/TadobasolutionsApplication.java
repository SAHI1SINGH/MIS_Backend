package com.tadobasolutions;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@RequiredArgsConstructor
@SpringBootApplication
@EnableScheduling
@EnableMongoAuditing
public class TadobasolutionsApplication {

    private static final Logger log = LoggerFactory.getLogger(TadobasolutionsApplication.class);
    private final Environment env;

    @Value("${cert.storage.path:NOT-SET}")
    private String certPath;

    public static void main(String[] args) {
        SpringApplication.run(TadobasolutionsApplication.class, args);
    }

    @PostConstruct
    public void logStartup() {
        log.info("Active profiles: {}", String.join(",", env.getActiveProfiles()));
        log.info("Certificate storage path = {}", certPath);
    }
}
