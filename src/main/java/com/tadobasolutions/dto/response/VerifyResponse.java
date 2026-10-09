package com.tadobasolutions.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyResponse {
    private String previewUrl;
    private String downloadUrl;
    private String fileName;
}
