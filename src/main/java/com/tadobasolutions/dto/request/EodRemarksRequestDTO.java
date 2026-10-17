package com.tadobasolutions.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class EodRemarksRequestDTO {

    private String remarkedBy;
    private List<RowRemark> remarks;

    @Data
    public static class RowRemark {
        private Integer srNo;
        private Integer rating;
        private String remarkText;
    }
}