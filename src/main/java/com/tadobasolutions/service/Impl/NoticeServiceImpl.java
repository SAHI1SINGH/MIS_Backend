package com.tadobasolutions.service.Impl;

import com.tadobasolutions.dto.NoticeDTO;
import com.tadobasolutions.entity.Notice;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.EmployeeRepository;
import com.tadobasolutions.repository.NoticeRepository;
import com.tadobasolutions.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository       noticeRepository;
    private final EmployeeRepository     employeeRepository;
    private final FcmNotificationService fcmService;

    private NoticeDTO mapToDTO(Notice notice) {
        return new NoticeDTO(
                notice.getId(),
                notice.getDescription(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    @Override
    public NoticeDTO createNotice(NoticeDTO dto) {
        Notice notice = new Notice();
        notice.setDescription(dto.getDescription());
        NoticeDTO saved = mapToDTO(noticeRepository.save(notice));

        // Collect every token from every employee (multiple devices supported)
        List<String> tokens = employeeRepository.findAll().stream()
                .filter(e -> e.getFcmTokens() != null)
                .flatMap(e -> e.getFcmTokens().stream())
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (!tokens.isEmpty()) {
            fcmService.sendToTokens(tokens, "📢 New Notice", dto.getDescription());
        }

        return saved;
    }

    @Override
    public NoticeDTO updateNotice(String id, NoticeDTO dto) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found"));
        notice.setDescription(dto.getDescription());
        return mapToDTO(noticeRepository.save(notice));
    }

    @Override
    public void deleteNotice(String id) {
        if (!noticeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notice not found");
        }
        noticeRepository.deleteById(id);
    }

    @Override
    public NoticeDTO getNoticeById(String id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found"));
        return mapToDTO(notice);
    }

    @Override
    public List<NoticeDTO> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}
