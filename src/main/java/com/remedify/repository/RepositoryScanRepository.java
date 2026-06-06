package com.remedify.repository;

import com.remedify.model.RepositoryScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RepositoryScanRepository extends JpaRepository<RepositoryScan, UUID> {
  Page<RepositoryScan> findByOrderByCreatedAtDesc(Pageable pageable);

  List<RepositoryScan> findByCreatedAtBefore(LocalDateTime date);

  @Query("SELECT r FROM RepositoryScan r WHERE r.currentStage != 'COMPLETED' ORDER BY r.updatedAt DESC")
  List<RepositoryScan> findInProgressScans();
}
