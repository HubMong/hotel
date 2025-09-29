package com.example.backend.admin.service;

import com.example.backend.admin.domain.Inquiry;
import com.example.backend.admin.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminCSService {
    private final InquiryRepository inquiryRepository;

    public Page<Inquiry> listInquiries(Inquiry.Status status, Pageable pageable) {
        return inquiryRepository.search(status, pageable);
    }

    public Inquiry reply(Long id, String reply) {
        Inquiry i = inquiryRepository.findById(id).orElseThrow();
        i.setReply(reply);
        i.setStatus(Inquiry.Status.ANSWERED);
        i.setRepliedAt(LocalDateTime.now());
        return inquiryRepository.save(i);
    }
}
