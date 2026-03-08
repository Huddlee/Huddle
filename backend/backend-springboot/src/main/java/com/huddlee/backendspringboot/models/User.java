package com.huddlee.backendspringboot.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private String displayName;
    private String email;
    private String password;
    private String role;

    @Indexed(expireAfter = "0s")
    private Date expireAt;
}

