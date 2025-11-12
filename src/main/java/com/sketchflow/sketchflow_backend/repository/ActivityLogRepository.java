package com.sketchflow.sketchflow_backend.repository;

import com.sketchflow.sketchflow_backend.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {

    Page<ActivityLog> findAllByOrderByTimestampDesc(Pageable pageable);

    List<ActivityLog> findByUserIdOrderByTimestampDesc(String userId);

    Page<ActivityLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    List<ActivityLog> findByActionOrderByTimestampDesc(String action);

    Page<ActivityLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);

    List<ActivityLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    Page<ActivityLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end, Pageable pageable);

    List<ActivityLog> findBySeverityOrderByTimestampDesc(String severity);

    Page<ActivityLog> findBySeverityOrderByTimestampDesc(String severity, Pageable pageable);

    Long countByTimestampAfter(LocalDateTime timestamp);

    Long countByActionAndTimestampAfter(String action, LocalDateTime timestamp);
}

