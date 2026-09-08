package com.example.acres.controller;

import com.example.acres.dto.PasswordDtos.ChangePasswordRequest;
import com.example.acres.service.CurrentUserService;
import com.example.acres.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class PasswordController {
    private final CurrentUserService current;
    private final UserService users;

    public PasswordController(CurrentUserService current, UserService users) {
        this.current = current;
        this.users = users;
    }

    @PostMapping("/password")
    public void change(@Valid @RequestBody ChangePasswordRequest r, HttpServletRequest req) {
        users.changePassword(current.get(), r.currentPassword(), r.newPassword(), req.getRemoteAddr());
    }
}
