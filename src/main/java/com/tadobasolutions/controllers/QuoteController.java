package com.tadobasolutions.controllers;

import com.tadobasolutions.entity.Quote;
import com.tadobasolutions.service.QuoteService;
import com.tadobasolutions.utilities.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/quotes")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://tadobasolutions.com",
        "https://tadobasolutions.com",
        "http://mis.tadobasolutions.com",
        "https://mis.tadobasolutions.com"
})
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    @GetMapping("/test")
    public String test() {
        return "Test done successfully";
    }


    @PostMapping("/save-quote")
    public ResponseEntity<ApiResponse<String>> saveQuote(@Valid @RequestBody Quote quote) {
        String data = quoteService.saveQuote(quote);
        ApiResponse<String> response = ApiResponse.<String>builder()
                .data(data)
                .message("Quote saved successfully!")
                .statusCode(HttpStatus.CREATED.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Quote>>> getAllQuoteList() {
        List<Quote> data = quoteService.getAllQuoteList();
        ApiResponse<List<Quote>> response = ApiResponse.<List<Quote>>builder()
                .data(data)
                .message("Quote list fetched successfully!")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/get-visit-count")
    public ResponseEntity<ApiResponse<Long>> getVisitCount() {
        Long data = quoteService.getVisitCount();
        ApiResponse<Long> response = ApiResponse.<Long>builder()
                .data(data)
                .message("Visit Count Fetched Successfully")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/update-visit-count")
    public ResponseEntity<ApiResponse<String>> updateVisitCount() {
        quoteService.updateVisitCount();
        ApiResponse<String> response = ApiResponse.<String>builder()
                .data("Visit Count Updated")
                .message("Visit Count Updated successfully.")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}