package com.edumerge.ticketsystem.controller;

import com.edumerge.ticketsystem.dto.UserResponse;
import com.edumerge.ticketsystem.entity.Role;
import com.edumerge.ticketsystem.entity.User;
import com.edumerge.ticketsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public List<UserResponse> list(@RequestParam(required = false) Role role) {
        List<User> users = (role != null) ? userRepository.findByRole(role) : userRepository.findAll();
        return users.stream()
                .map(u -> UserResponse.builder()
                        .id(u.getId()).name(u.getName()).email(u.getEmail())
                        .role(u.getRole()).department(u.getDepartment())
                        .build())
                .toList();
    }
}
