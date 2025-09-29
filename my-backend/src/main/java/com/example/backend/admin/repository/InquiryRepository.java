package com.example.backend.admin.repository;

import com.example.backend.admin.domain.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    @Query("SELECT i FROM Inquiry i WHERE (:status IS NULL OR i.status = :status)")
    Page<Inquiry> search(@Param("status") Inquiry.Status status, Pageable pageable);
    
    long countByStatus(Inquiry.Status status);
}
