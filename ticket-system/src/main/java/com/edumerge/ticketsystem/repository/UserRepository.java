package com.edumerge.ticketsystem.repository;

import com.edumerge.ticketsystem.entity.Role;
import com.edumerge.ticketsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByRole(Role role);
}
