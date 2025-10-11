package com.example.backend.admin.repository;

import com.example.backend.admin.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    @Query("SELECT n FROM Notice n WHERE (:active IS NULL OR n.active = :active)")
    Page<Notice> search(@Param("active") Boolean active, Pageable pageable);
}
