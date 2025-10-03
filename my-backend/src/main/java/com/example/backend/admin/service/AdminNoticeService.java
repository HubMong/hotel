package com.example.backend.admin.service;

import com.example.backend.admin.domain.Notice;
import com.example.backend.admin.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminNoticeService {
    private final NoticeRepository noticeRepository;

    public Page<Notice> list(Boolean active, Pageable pageable) { return noticeRepository.search(active, pageable); }
    public Notice get(Long id) { return noticeRepository.findById(id).orElseThrow(); }
    public Notice create(Notice n) { return noticeRepository.save(n); }
    public Notice update(Long id, Notice req) {
        Notice n = get(id);
        n.setTitle(req.getTitle());
        n.setContent(req.getContent());
        n.setActive(req.getActive());
        n.setPinned(req.getPinned());
        return noticeRepository.save(n);
    }
    public void delete(Long id) { noticeRepository.deleteById(id); }
}
