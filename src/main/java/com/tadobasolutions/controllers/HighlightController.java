package com.tadobasolutions.controllers;

import com.tadobasolutions.dto.HighlightDTO;
import com.tadobasolutions.service.HighlightService;
import com.tadobasolutions.utilities.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/highlights")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://tadobasolutions.com",
        "https://tadobasolutions.com",
        "http://mis.tadobasolutions.com",
        "https://mis.tadobasolutions.com"
})
@RequiredArgsConstructor
public class HighlightController {

    private final HighlightService highlightService;

    @PostMapping
    public ResponseEntity<ApiResponse<HighlightDTO>> create(@RequestBody HighlightDTO dto) {
        HighlightDTO created = highlightService.createHighlight(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<HighlightDTO>builder()
                        .data(created)
                        .message("Highlight created successfully")
                        .statusCode(HttpStatus.CREATED.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HighlightDTO>> update(@PathVariable String id, @RequestBody HighlightDTO dto) {
        HighlightDTO updated = highlightService.updateHighlight(id, dto);
        return ResponseEntity.ok(
                ApiResponse.<HighlightDTO>builder()
                        .data(updated)
                        .message("Highlight updated successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        highlightService.deleteHighlight(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .data(null)
                        .message("Highlight deleted successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HighlightDTO>> getById(@PathVariable String id) {
        HighlightDTO highlight = highlightService.getHighlightById(id);
        return ResponseEntity.ok(
                ApiResponse.<HighlightDTO>builder()
                        .data(highlight)
                        .message("Highlight fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HighlightDTO>>> getAll() {
        List<HighlightDTO> highlights = highlightService.getAllHighlights();
        return ResponseEntity.ok(
                ApiResponse.<List<HighlightDTO>>builder()
                        .data(highlights)
                        .message("Highlights fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }
}
