package com.huddlee.backendspringboot.repos;

import com.huddlee.backendspringboot.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends MongoRepository<User, Long> {
    User findByUsername(String username);

    boolean existsUserByUsername(String username);
}
