package com.tadobasolutions.service;

import com.tadobasolutions.dto.InternDTO;

import java.util.List;

public interface InternService {

    InternDTO createIntern(InternDTO dto);
    List<InternDTO> getAllInterns();
    InternDTO getInternById(String id);
    InternDTO getInternByEmail (String email);
    InternDTO updateIntern( String id ,InternDTO dto);
    void deleteIntern(String id);
}
