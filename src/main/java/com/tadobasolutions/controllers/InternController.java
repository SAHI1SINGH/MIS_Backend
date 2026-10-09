package com.tadobasolutions.controllers;


import com.tadobasolutions.dto.InternDTO;
import com.tadobasolutions.service.InternService;
import com.tadobasolutions.utilities.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/interns")
@RequiredArgsConstructor
public class InternController {
    private final InternService internService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<InternDTO>> getMe(){
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            InternDTO dto = internService.getInternByEmail(email);
            return ResponseEntity.ok(
                    ApiResponse.<InternDTO>builder()
                            .data(dto)
                            .message("Intern Fetched")
                            .statusCode(HttpStatus.OK.value())
                            .timeStamp(LocalDateTime.now())
                            .success(true)
                            .build()
            );
    }
    @PostMapping
    public ResponseEntity<ApiResponse<InternDTO>> createIntern(@RequestBody InternDTO dto){
        InternDTO created = internService.createIntern(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<InternDTO>builder()
                        .data(created)
                        .message("Intern Created Successfully")
                        .statusCode(HttpStatus.CREATED.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InternDTO>>> getALlInterns(){
        List<InternDTO> interns =internService.getAllInterns();

        return ResponseEntity.ok(
                ApiResponse.<List<InternDTO>>builder()
                        .data(interns)
                        .message("Interns Fetched")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InternDTO>> getInternById (@PathVariable String id){
        InternDTO dto = internService.getInternById(id);
        return ResponseEntity.ok(
                ApiResponse.<InternDTO>builder()
                        .data(dto)
                        .message("Intern Fetched By Id")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InternDTO>> updateIntern (@RequestBody InternDTO dto , @PathVariable String id){
        InternDTO updated = internService.updateIntern(id, dto);

        return ResponseEntity.ok(
                ApiResponse.<InternDTO>builder()
                        .data(updated)
                        .message("Interns Updated Successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteIntern (@PathVariable String id){
         internService.deleteIntern(id);
         return ResponseEntity.ok(
                 ApiResponse.<Void>builder()
                         .message("Intern Deleted Successfully")
                         .statusCode(HttpStatus.OK.value())
                         .timeStamp(LocalDateTime.now())
                         .success(true)
                         .build()
         );
    }
}
