package com.sketchflow.sketchflow_backend.repository;

import com.sketchflow.sketchflow_backend.model.Notification;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByReceiverIdOrderByTimestampDesc(String receiverId);
    List<Notification> findByReceiverIdAndReadFalseOrderByTimestampDesc(String receiverId);
    long countByReceiverIdAndReadFalse(String receiverId);

    default List<Notification> findAllOrdered() {
        return findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }
}
