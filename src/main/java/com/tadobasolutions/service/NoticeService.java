package com.tadobasolutions.service;

import com.tadobasolutions.dto.NoticeDTO;

import java.util.List;

public interface NoticeService {
    NoticeDTO createNotice(NoticeDTO dto);
    NoticeDTO updateNotice(String id, NoticeDTO dto);
    void deleteNotice(String id);
    NoticeDTO getNoticeById(String id);
    List<NoticeDTO> getAllNotices();
}
