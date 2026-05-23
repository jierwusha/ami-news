package com.project.aminewsbackend.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String username;
    private String password;
    private String email;
    private String code;
    private String usernameOrEmail;
}
