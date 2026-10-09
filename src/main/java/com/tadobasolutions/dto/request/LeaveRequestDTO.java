package com.tadobasolutions.dto.request;
import com.tadobasolutions.entity.enums.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaveRequestDTO {
//    private String employeeId;
    private LeaveType leaveType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
}
