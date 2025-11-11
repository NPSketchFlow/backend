package com.sketchflow.sketchflow_backend.repository;

import com.sketchflow.sketchflow_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
	java.util.List<User> findByStatus(String status);
}
