package com.tadobasolutions.service;

import com.tadobasolutions.dto.request.AddComponentRequestDTO;
import com.tadobasolutions.dto.request.AddStockRequestDTO;
import com.tadobasolutions.dto.request.BatchUsageRequestDTO;
import com.tadobasolutions.dto.request.UpdateComponentRequestDTO;
import com.tadobasolutions.dto.request.UsageRequestDTO;
import com.tadobasolutions.dto.response.ComponentResponseDTO;
import com.tadobasolutions.dto.response.UsageResponseDTO;
import com.tadobasolutions.entity.Employee;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface StoreService {
    ComponentResponseDTO addComponent(AddComponentRequestDTO dto, Employee currentEmployee);
    ComponentResponseDTO updateComponent(String componentId, UpdateComponentRequestDTO dto, Employee currentEmployee);
    ComponentResponseDTO getComponentById(String componentId, Employee currentEmployee);
    ComponentResponseDTO uploadComponentImage(String componentId, MultipartFile image, Employee currentEmployee);
    ComponentResponseDTO deleteComponentImage(String componentId, Employee currentEmployee);
    ComponentResponseDTO addStock(String componentId, AddStockRequestDTO dto, Employee currentEmployee);
    UsageResponseDTO recordUsage(UsageRequestDTO dto, Employee currentEmployee);
    List<UsageResponseDTO> getUsageHistory(Employee currentEmployee, String departmentFilter);
    ByteArrayInputStream exportUsageExcel(Employee currentEmployee, String departmentFilter);
    List<ComponentResponseDTO> getComponents(Employee currentEmployee, String departmentIdFilter);
    List<UsageResponseDTO> recordBatchUsage(BatchUsageRequestDTO dto, Employee currentEmployee);
}