package com.example.AtlazDB.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.example.AtlazDB.dto.LoginRequestDTO;
import com.example.AtlazDB.enums.Profile;
import com.example.AtlazDB.model.User;
import com.example.AtlazDB.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthController authController;

    @Test
    void shouldReturn401WhenUserNotFound() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("missing@test.com");
        dto.setPassword("123456");

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.login(dto);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void shouldReturn401WhenPasswordDoesNotMatch() {
        User user = new User();
        user.setEmail("user@test.com");
        user.setPasswordHash("correct_pass");

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("user@test.com");
        dto.setPassword("wrong_pass");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = authController.login(dto);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void shouldReturn200WhenCredentialsAreValid() {
        User user = new User();
        user.setId(1L);
        user.setName("Ricardo");
        user.setEmail("user@test.com");
        user.setPasswordHash("correct_pass");
        user.setProfile(Profile.ADMIN);

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("user@test.com");
        dto.setPassword("correct_pass");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = authController.login(dto);

        assertEquals(200, response.getStatusCode().value());
    }
}