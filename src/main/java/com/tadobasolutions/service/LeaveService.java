package com.tadobasolutions.service;

import com.tadobasolutions.dto.request.LeaveRequestDTO;
import com.tadobasolutions.dto.request.LeaveReviewRequestDTO;
import com.tadobasolutions.dto.response.CurrentlyOnLeaveDTO;
import com.tadobasolutions.dto.response.LeaveResponseDTO;
import com.tadobasolutions.entity.enums.LeaveStatus;

import java.util.List;

public interface LeaveService {
    LeaveResponseDTO applyLeave(String email, LeaveRequestDTO dto);
    LeaveResponseDTO review(String leaveId, LeaveReviewRequestDTO dto);
    LeaveResponseDTO getLeaveById(String id);
    List<LeaveResponseDTO> getAllLeaves(LeaveStatus status, String employeeId);
    void cancelLeave(String id, String employeeId);
    List<CurrentlyOnLeaveDTO> getCurrentlyOnLeave();
    List<LeaveResponseDTO> getReviewHistory(String approverId);
}