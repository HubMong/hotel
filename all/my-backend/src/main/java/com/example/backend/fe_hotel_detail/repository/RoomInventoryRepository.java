package com.example.backend.fe_hotel_detail.repository;

import com.example.backend.fe_hotel_detail.domain.RoomInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface RoomInventoryRepository extends JpaRepository<RoomInventory, Long> {}
