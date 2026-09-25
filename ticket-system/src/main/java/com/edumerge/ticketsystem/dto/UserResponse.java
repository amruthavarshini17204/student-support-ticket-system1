package com.edumerge.ticketsystem.dto;

import com.edumerge.ticketsystem.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private String department;
}
