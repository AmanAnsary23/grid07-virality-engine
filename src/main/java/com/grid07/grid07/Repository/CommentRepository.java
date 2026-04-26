package com.grid07.grid07.Repository;

import com.grid07.grid07.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}