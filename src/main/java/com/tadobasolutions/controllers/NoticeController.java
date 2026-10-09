package com.tadobasolutions.controllers;

import com.tadobasolutions.dto.NoticeDTO;
import com.tadobasolutions.service.NoticeService;
import com.tadobasolutions.utilities.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notices")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://tadobasolutions.com",
        "https://tadobasolutions.com",
        "http://mis.tadobasolutions.com",
        "https://mis.tadobasolutions.com"
})
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @PostMapping
    public ResponseEntity<ApiResponse<NoticeDTO>> create(@RequestBody NoticeDTO dto) {
        NoticeDTO created = noticeService.createNotice(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<NoticeDTO>builder()
                        .data(created)
                        .message("Notice created successfully")
                        .statusCode(HttpStatus.CREATED.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeDTO>> update(@PathVariable String id, @RequestBody NoticeDTO dto) {
        NoticeDTO updated = noticeService.updateNotice(id, dto);
        return ResponseEntity.ok(
                ApiResponse.<NoticeDTO>builder()
                        .data(updated)
                        .message("Notice updated successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .data(null)
                        .message("Notice deleted successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeDTO>> getById(@PathVariable String id) {
        NoticeDTO notice = noticeService.getNoticeById(id);
        return ResponseEntity.ok(
                ApiResponse.<NoticeDTO>builder()
                        .data(notice)
                        .message("Notice fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeDTO>>> getAll() {
        List<NoticeDTO> notices = noticeService.getAllNotices();
        return ResponseEntity.ok(
                ApiResponse.<List<NoticeDTO>>builder()
                        .data(notices)
                        .message("Notices fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }
}
