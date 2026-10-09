package com.tadobasolutions.service;

import com.tadobasolutions.dto.HighlightDTO;

import java.util.List;

public interface HighlightService {
    HighlightDTO createHighlight(HighlightDTO dto);
    HighlightDTO updateHighlight(String id, HighlightDTO dto);
    void deleteHighlight(String id);
    HighlightDTO getHighlightById(String id);
    List<HighlightDTO> getAllHighlights();
}
