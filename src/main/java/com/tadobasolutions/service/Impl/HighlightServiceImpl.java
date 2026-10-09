package com.tadobasolutions.service.Impl;

import com.tadobasolutions.dto.HighlightDTO;
import com.tadobasolutions.entity.Highlight;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.EmployeeRepository;
import com.tadobasolutions.repository.HighlightRepository;
import com.tadobasolutions.service.HighlightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighlightServiceImpl implements HighlightService {

    private final HighlightRepository    highlightRepository;
    private final EmployeeRepository     employeeRepository;
    private final FcmNotificationService fcmService;

    private HighlightDTO mapToDTO(Highlight highlight) {
        return new HighlightDTO(
                highlight.getId(),
                highlight.getDescription(),
                highlight.getCreatedAt(),
                highlight.getUpdatedAt()
        );
    }

    @Override
    public HighlightDTO createHighlight(HighlightDTO dto) {
        Highlight highlight = new Highlight();
        highlight.setDescription(dto.getDescription());
        HighlightDTO saved = mapToDTO(highlightRepository.save(highlight));

        // Collect every token from every employee (multiple devices supported)
        List<String> tokens = employeeRepository.findAll().stream()
                .filter(e -> e.getFcmTokens() != null)
                .flatMap(e -> e.getFcmTokens().stream())
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (!tokens.isEmpty()) {
            fcmService.sendToTokens(tokens, "🌟 New Highlight", dto.getDescription());
        }

        return saved;
    }

    @Override
    public HighlightDTO updateHighlight(String id, HighlightDTO dto) {
        Highlight highlight = highlightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Highlight not found"));
        highlight.setDescription(dto.getDescription());
        return mapToDTO(highlightRepository.save(highlight));
    }

    @Override
    public void deleteHighlight(String id) {
        if (!highlightRepository.existsById(id)) {
            throw new ResourceNotFoundException("Highlight not found");
        }
        highlightRepository.deleteById(id);
    }

    @Override
    public HighlightDTO getHighlightById(String id) {
        Highlight highlight = highlightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Highlight not found"));
        return mapToDTO(highlight);
    }

    @Override
    public List<HighlightDTO> getAllHighlights() {
        return highlightRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}
