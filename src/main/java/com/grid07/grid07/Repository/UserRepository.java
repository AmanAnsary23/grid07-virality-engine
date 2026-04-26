package com.grid07.grid07.Repository;

import com.grid07.grid07.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}