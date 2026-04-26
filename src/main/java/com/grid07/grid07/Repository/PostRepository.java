package com.grid07.grid07.Repository;

import com.grid07.grid07.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
