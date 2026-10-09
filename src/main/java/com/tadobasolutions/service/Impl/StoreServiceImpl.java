package com.tadobasolutions.service.Impl;

import com.tadobasolutions.dto.request.*;
import com.tadobasolutions.dto.response.ComponentResponseDTO;
import com.tadobasolutions.dto.response.UsageResponseDTO;
import com.tadobasolutions.entity.Department;
import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.entity.StoreComponent;
import com.tadobasolutions.entity.StoreComponentUsage;
import com.tadobasolutions.entity.enums.Role;
import com.tadobasolutions.exception.BadRequestException;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.DepartmentRepository;
import com.tadobasolutions.repository.StoreComponentRepository;
import com.tadobasolutions.repository.StoreComponentUsageRepository;
import com.tadobasolutions.service.StoreService;
import com.tadobasolutions.utilities.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import java.util.LinkedHashMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private static final String IMAGE_FOLDER = "mis/store-components";

    private final StoreComponentRepository componentRepository;
    private final StoreComponentUsageRepository usageRepository;
    private final DepartmentRepository departmentRepository;
    private final MongoTemplate mongoTemplate;
    private final CloudinaryService cloudinaryService;

    // ── Access helpers ───────────────────────────────────────────────────────

    private boolean isAdmin(Employee employee) {
        return employee.getRole() == Role.ADMIN;
    }

    // Only the department's incharge (Department.inchargeId) or an Admin
    // may add new components or top up stock.
    private void requireIncharge(Employee employee) {
        if (isAdmin(employee)) return;

        if (employee.getDepartmentId() == null) {
            throw new AccessDeniedException("You are not assigned to a department");
        }
        Department dept = departmentRepository.findById(employee.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        if (dept.getInchargeId() == null || !dept.getInchargeId().equals(employee.getId())) {
            throw new AccessDeniedException("Only the department incharge can manage stock");
        }
    }

    private void requireSameDepartment(Employee employee, StoreComponent component) {
        if (isAdmin(employee)) return;
        if (employee.getDepartmentId() == null
                || !employee.getDepartmentId().equals(component.getDepartmentId())) {
            throw new AccessDeniedException("This component does not belong to your department");
        }
    }

    private StoreComponent findComponentOrThrow(String componentId) {
        return componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Component not found"));
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // ── Component management ────────────────────────────────────────────────

    @Override
    public ComponentResponseDTO addComponent(AddComponentRequestDTO dto, Employee currentEmployee) {
        requireIncharge(currentEmployee);

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BadRequestException("Component name is required");
        }
        if (dto.getInitialQuantity() < 0) {
            throw new BadRequestException("Initial quantity cannot be negative");
        }
        if (dto.getUnitPrice() != null && dto.getUnitPrice() < 0) {
            throw new BadRequestException("Unit price cannot be negative");
        }

        String departmentId   = currentEmployee.getDepartmentId();
        String departmentName = currentEmployee.getDepartmentName();

        if (isAdmin(currentEmployee) && departmentId == null) {
            throw new BadRequestException("Admin must be assigned to a department to add components directly, or use an employee account");
        }

        componentRepository.findByDepartmentIdAndNameIgnoreCase(departmentId, dto.getName().trim())
                .ifPresent(c -> {
                    throw new BadRequestException("Component '" + dto.getName() + "' already exists in this department");
                });

        StoreComponent component = new StoreComponent();
        component.setName(dto.getName().trim());
        component.setUnit(dto.getUnit());

        component.setDescription(trimToNull(dto.getDescription()));
        component.setBrand(trimToNull(dto.getBrand()));
        component.setModelNumber(trimToNull(dto.getModelNumber()));
        component.setCategory(trimToNull(dto.getCategory()));
        component.setSpecifications(dto.getSpecifications());
        component.setUnitPrice(dto.getUnitPrice());
        component.setSupplier(trimToNull(dto.getSupplier()));
        component.setWarranty(trimToNull(dto.getWarranty()));

        component.setTotalQuantity(dto.getInitialQuantity());
        component.setUsedQuantity(0);
        component.setAvailableQuantity(dto.getInitialQuantity());
        component.setDepartmentId(departmentId);
        component.setDepartmentName(departmentName);
        component.setCreatedByEmployeeId(currentEmployee.getId());
        component.setCreatedByName(currentEmployee.getName());
        component.setCreatedAt(LocalDateTime.now());
        component.setUpdatedAt(LocalDateTime.now());

        return toComponentDTO(componentRepository.save(component));
    }

    @Override
    public ComponentResponseDTO updateComponent(String componentId, UpdateComponentRequestDTO dto, Employee currentEmployee) {
        StoreComponent component = findComponentOrThrow(componentId);

        requireIncharge(currentEmployee);
        requireSameDepartment(currentEmployee, component);

        if (dto.getName() != null) {
            String newName = dto.getName().trim();
            if (newName.isEmpty()) {
                throw new BadRequestException("Component name cannot be empty");
            }
            if (!newName.equalsIgnoreCase(component.getName())) {
                componentRepository.findByDepartmentIdAndNameIgnoreCase(component.getDepartmentId(), newName)
                        .ifPresent(c -> {
                            throw new BadRequestException("Component '" + newName + "' already exists in this department");
                        });
            }
            component.setName(newName);
        }
        if (dto.getUnitPrice() != null && dto.getUnitPrice() < 0) {
            throw new BadRequestException("Unit price cannot be negative");
        }

        if (dto.getUnit() != null)           component.setUnit(dto.getUnit());
        if (dto.getDescription() != null)    component.setDescription(trimToNull(dto.getDescription()));
        if (dto.getBrand() != null)          component.setBrand(trimToNull(dto.getBrand()));
        if (dto.getModelNumber() != null)    component.setModelNumber(trimToNull(dto.getModelNumber()));
        if (dto.getCategory() != null)       component.setCategory(trimToNull(dto.getCategory()));
        if (dto.getSpecifications() != null) component.setSpecifications(dto.getSpecifications());
        if (dto.getUnitPrice() != null)      component.setUnitPrice(dto.getUnitPrice());
        if (dto.getSupplier() != null)       component.setSupplier(trimToNull(dto.getSupplier()));
        if (dto.getWarranty() != null)       component.setWarranty(trimToNull(dto.getWarranty()));

        component.setUpdatedAt(LocalDateTime.now());
        return toComponentDTO(componentRepository.save(component));
    }

    @Override
    public ComponentResponseDTO getComponentById(String componentId, Employee currentEmployee) {
        StoreComponent component = findComponentOrThrow(componentId);
        requireSameDepartment(currentEmployee, component);
        return toComponentDTO(component);
    }

    @Override
    public ComponentResponseDTO uploadComponentImage(String componentId, MultipartFile image, Employee currentEmployee) {
        StoreComponent component = findComponentOrThrow(componentId);

        requireIncharge(currentEmployee);
        requireSameDepartment(currentEmployee, component);

        CloudinaryService.UploadResult uploaded = cloudinaryService.uploadImage(image, IMAGE_FOLDER);

        String oldPublicId = component.getImagePublicId();
        component.setImageUrl(uploaded.url());
        component.setImagePublicId(uploaded.publicId());
        component.setUpdatedAt(LocalDateTime.now());
        StoreComponent saved = componentRepository.save(component);

        // Remove the previous image only after the new one is safely saved
        cloudinaryService.deleteImage(oldPublicId);

        return toComponentDTO(saved);
    }

    @Override
    public ComponentResponseDTO deleteComponentImage(String componentId, Employee currentEmployee) {
        StoreComponent component = findComponentOrThrow(componentId);

        requireIncharge(currentEmployee);
        requireSameDepartment(currentEmployee, component);

        String oldPublicId = component.getImagePublicId();
        component.setImageUrl(null);
        component.setImagePublicId(null);
        component.setUpdatedAt(LocalDateTime.now());
        StoreComponent saved = componentRepository.save(component);

        cloudinaryService.deleteImage(oldPublicId);

        return toComponentDTO(saved);
    }

    @Override
    public ComponentResponseDTO addStock(String componentId, AddStockRequestDTO dto, Employee currentEmployee) {
        StoreComponent existing = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Component not found"));

        requireIncharge(currentEmployee);
        requireSameDepartment(currentEmployee, existing);

        if (dto.getQuantity() <= 0) {
            throw new BadRequestException("Quantity to add must be greater than 0");
        }

        Query query = new Query(Criteria.where("id").is(componentId));
        Update update = new Update()
                .inc("totalQuantity", dto.getQuantity())
                .inc("availableQuantity", dto.getQuantity())
                .set("updatedAt", LocalDateTime.now());

        StoreComponent updated = mongoTemplate.findAndModify(
                query, update,
                FindAndModifyOptions.options().returnNew(true),
                StoreComponent.class
        );

        if (updated == null) {
            throw new ResourceNotFoundException("Component not found");
        }
        return toComponentDTO(updated);
    }

    // ── Usage recording (atomic subtract) ───────────────────────────────────

    @Override
    public UsageResponseDTO recordUsage(UsageRequestDTO dto, Employee currentEmployee) {
        if (dto.getQuantityUsed() <= 0) {
            throw new BadRequestException("Quantity used must be greater than 0");
        }
        if (dto.getPurpose() == null || dto.getPurpose().isBlank()) {
            throw new BadRequestException("Purpose is required");
        }

        StoreComponent target = componentRepository.findById(dto.getComponentId())
                .orElseThrow(() -> new ResourceNotFoundException("Component not found"));

        // Any employee may use stock, but only from their own department
        // (Admin may use any department's stock).
        requireSameDepartment(currentEmployee, target);

        Query query = new Query(
                Criteria.where("id").is(dto.getComponentId())
                        .and("availableQuantity").gte(dto.getQuantityUsed())
        );
        Update update = new Update()
                .inc("availableQuantity", -dto.getQuantityUsed())
                .inc("usedQuantity", dto.getQuantityUsed())
                .set("updatedAt", LocalDateTime.now());

        StoreComponent updated = mongoTemplate.findAndModify(
                query, update,
                FindAndModifyOptions.options().returnNew(true),
                StoreComponent.class
        );

        if (updated == null) {
            StoreComponent existing = componentRepository.findById(dto.getComponentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Component not found"));
            throw new BadRequestException(
                    "Only " + existing.getAvailableQuantity() + " " + existing.getUnit() +
                            " left for '" + existing.getName() + "'"
            );
        }

        StoreComponentUsage usage = new StoreComponentUsage();
        usage.setComponentId(updated.getId());
        usage.setComponentName(updated.getName());
        usage.setUnit(updated.getUnit());
        usage.setQuantityUsed(dto.getQuantityUsed());
        usage.setPurpose(dto.getPurpose());
        usage.setDepartmentId(updated.getDepartmentId());
        usage.setDepartmentName(updated.getDepartmentName());
        usage.setUsedByEmployeeId(currentEmployee.getId());
        usage.setUsedByName(currentEmployee.getName());
        usage.setUsedAt(LocalDateTime.now());

        return toUsageDTO(usageRepository.save(usage));
    }

    // ── History & export ─────────────────────────────────────────────────────

    @Override
    public List<UsageResponseDTO> getUsageHistory(Employee currentEmployee, String departmentFilter) {
        List<StoreComponentUsage> usages;

        if (isAdmin(currentEmployee)) {
            usages = (departmentFilter == null || departmentFilter.isBlank())
                    ? usageRepository.findAllByOrderByUsedAtDesc()
                    : usageRepository.findByDepartmentNameOrderByUsedAtDesc(departmentFilter);
        } else {
            // Non-admins only ever see their own department's history,
            // regardless of what they pass in the query param.
            usages = usageRepository.findByDepartmentIdOrderByUsedAtDesc(currentEmployee.getDepartmentId());
        }

        return usages.stream().map(this::toUsageDTO).collect(Collectors.toList());
    }

    @Override
    public ByteArrayInputStream exportUsageExcel(Employee currentEmployee, String departmentFilter) {
        List<UsageResponseDTO> usageList = getUsageHistory(currentEmployee, departmentFilter);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Component Usage");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle purposeStyle = workbook.createCellStyle();
            purposeStyle.setVerticalAlignment(VerticalAlignment.TOP);
            purposeStyle.setWrapText(true);

            // Purpose comes first — one dispatch/purpose can cover several
            // components at once, so purpose groups the rows and each component
            // used for that purpose is listed beneath it.
            String[] headers = {"Purpose", "Component Name", "Quantity Used", "Unit", "Department", "Used By", "Date & Time"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            LinkedHashMap<String, List<UsageResponseDTO>> grouped = new LinkedHashMap<>();
            for (UsageResponseDTO usage : usageList) {
                String key = (usage.getBatchId() != null && !usage.getBatchId().isBlank())
                        ? usage.getBatchId()
                        : usage.getPurpose() + "||" + usage.getUsedByName() + "||" + usage.getDepartmentName();
                grouped.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(usage);
            }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            int rowIdx = 1;

            for (List<UsageResponseDTO> group : grouped.values()) {
                int groupStartRow = rowIdx;

                for (UsageResponseDTO usage : group) {
                    Row row = sheet.createRow(rowIdx);
                    Cell purposeCell = row.createCell(0);
                    purposeCell.setCellValue(usage.getPurpose());
                    purposeCell.setCellStyle(purposeStyle);

                    row.createCell(1).setCellValue(usage.getComponentName());
                    row.createCell(2).setCellValue(usage.getQuantityUsed());
                    row.createCell(3).setCellValue(usage.getUnit());
                    row.createCell(4).setCellValue(usage.getDepartmentName());
                    row.createCell(5).setCellValue(usage.getUsedByName());
                    row.createCell(6).setCellValue(usage.getUsedAt().format(fmt));
                    rowIdx++;
                }

                // Merge the Purpose column across every row in this group, so it
                // visually reads as "one purpose → several components listed below it".
                if (group.size() > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(groupStartRow, rowIdx - 1, 0, 0));
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            log.error("Failed to generate store usage excel", e);
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ComponentResponseDTO> getComponents(Employee currentEmployee, String departmentIdFilter) {
        List<StoreComponent> components;

        if (isAdmin(currentEmployee)) {
            components = (departmentIdFilter == null || departmentIdFilter.isBlank())
                    ? componentRepository.findAll()
                    : componentRepository.findByDepartmentId(departmentIdFilter);
        } else {
            components = componentRepository.findByDepartmentId(currentEmployee.getDepartmentId());
        }

        return components.stream().map(this::toComponentDTO).collect(Collectors.toList());
    }

    @Override
    public List<UsageResponseDTO> recordBatchUsage(BatchUsageRequestDTO dto, Employee currentEmployee) {
        if (dto.getPurpose() == null || dto.getPurpose().isBlank()) {
            throw new BadRequestException("Purpose is required");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BadRequestException("Add at least one component");
        }

        // Pre-check every component exists, belongs to the caller's department,
        // and has enough stock — before touching anything. This avoids leaving
        // a batch half-committed if item #3 of 5 turns out to be short on stock.
        for (UsageItemDTO item : dto.getItems()) {
            if (item.getQuantityUsed() <= 0) {
                throw new BadRequestException("Quantity must be greater than 0 for every component");
            }
            StoreComponent component = componentRepository.findById(item.getComponentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Component not found: " + item.getComponentId()));

            requireSameDepartment(currentEmployee, component);

            if (component.getAvailableQuantity() < item.getQuantityUsed()) {
                throw new BadRequestException(
                        "Only " + component.getAvailableQuantity() + " " + component.getUnit() +
                                " available for '" + component.getName() + "'"
                );
            }
        }

        String batchId = java.util.UUID.randomUUID().toString();
        List<UsageResponseDTO> results = new java.util.ArrayList<>();

        for (UsageItemDTO item : dto.getItems()) {
            Query query = new Query(
                    Criteria.where("id").is(item.getComponentId())
                            .and("availableQuantity").gte(item.getQuantityUsed())
            );
            Update update = new Update()
                    .inc("availableQuantity", -item.getQuantityUsed())
                    .inc("usedQuantity", item.getQuantityUsed())
                    .set("updatedAt", LocalDateTime.now());

            StoreComponent updated = mongoTemplate.findAndModify(
                    query, update,
                    FindAndModifyOptions.options().returnNew(true),
                    StoreComponent.class
            );

            if (updated == null) {
                // Stock changed between the pre-check and now (race condition) —
                // fail loudly rather than silently skip this item.
                throw new BadRequestException("Stock changed while processing your request — please try again.");
            }

            StoreComponentUsage usage = new StoreComponentUsage();
            usage.setBatchId(batchId);
            usage.setComponentId(updated.getId());
            usage.setComponentName(updated.getName());
            usage.setUnit(updated.getUnit());
            usage.setQuantityUsed(item.getQuantityUsed());
            usage.setPurpose(dto.getPurpose());
            usage.setDepartmentId(updated.getDepartmentId());
            usage.setDepartmentName(updated.getDepartmentName());
            usage.setUsedByEmployeeId(currentEmployee.getId());
            usage.setUsedByName(currentEmployee.getName());
            usage.setUsedAt(LocalDateTime.now());

            results.add(toUsageDTO(usageRepository.save(usage)));
        }

        return results;
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private ComponentResponseDTO toComponentDTO(StoreComponent c) {
        return ComponentResponseDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .unit(c.getUnit())
                .description(c.getDescription())
                .brand(c.getBrand())
                .modelNumber(c.getModelNumber())
                .category(c.getCategory())
                .specifications(c.getSpecifications())
                .unitPrice(c.getUnitPrice())
                .supplier(c.getSupplier())
                .warranty(c.getWarranty())
                .imageUrl(c.getImageUrl())
                .totalQuantity(c.getTotalQuantity())
                .usedQuantity(c.getUsedQuantity())
                .availableQuantity(c.getAvailableQuantity())
                .departmentId(c.getDepartmentId())
                .departmentName(c.getDepartmentName())
                .build();
    }

    private UsageResponseDTO toUsageDTO(StoreComponentUsage u) {
        return UsageResponseDTO.builder()
                .id(u.getId())
                .batchId(u.getBatchId())
                .componentName(u.getComponentName())
                .unit(u.getUnit())
                .quantityUsed(u.getQuantityUsed())
                .purpose(u.getPurpose())
                .departmentName(u.getDepartmentName())
                .usedByName(u.getUsedByName())
                .usedAt(u.getUsedAt())
                .build();
    }
}