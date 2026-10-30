package com.example.AtlazDB.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.AtlazDB.dto.UserRequestDTO;
import com.example.AtlazDB.enums.UserStatus;
import com.example.AtlazDB.model.BusinessRuleException;
import com.example.AtlazDB.model.User;
import com.example.AtlazDB.repository.ServiceOrderRepository;
import com.example.AtlazDB.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldListAllUsers() {
        when(repository.findAll()).thenReturn(List.of(new User()));
        assertEquals(1, userService.listAll().size());
    }

    @Test
    void shouldFindUserById() {
        when(repository.findById(1L)).thenReturn(Optional.of(new User()));
        assertTrue(userService.findById(1L).isPresent());
    }

    @Test
    void shouldSaveUserWithExplicitAndDefaultStatus() {
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRequestDTO dtoWithStatus = new UserRequestDTO();
        dtoWithStatus.setUserStatus("EM_CAMPO");
        User saved1 = userService.save(dtoWithStatus);
        assertEquals(UserStatus.EM_CAMPO, saved1.getUserStatus());

        UserRequestDTO dtoNullStatus = new UserRequestDTO();
        dtoNullStatus.setUserStatus(null);
        User saved2 = userService.save(dtoNullStatus);
        assertEquals(UserStatus.DISPONIVEL, saved2.getUserStatus());
    }

    @Test
    void shouldDeleteUserById() {
        userService.delete(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundOnUpdate() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> userService.update(99L, new UserRequestDTO()));
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenDeactivatingUserWithActiveOrder() {
        when(repository.findById(1L)).thenReturn(Optional.of(new User()));
        when(serviceOrderRepository.existsByUserIdAndReturnDateIsNull(1L)).thenReturn(true);

        UserRequestDTO dto = new UserRequestDTO();
        dto.setUserStatus("DESLIGADO");

        assertThrows(BusinessRuleException.class, () -> userService.update(1L, dto));
    }

    @Test
    void shouldUpdateUserCoveringAllStatusBranches() {
        when(repository.findById(1L)).thenReturn(Optional.of(new User()));
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        when(serviceOrderRepository.existsByUserIdAndReturnDateIsNull(1L)).thenReturn(false);
        UserRequestDTO dtoDesligado = new UserRequestDTO();
        dtoDesligado.setUserStatus("DESLIGADO");
        User updated1 = userService.update(1L, dtoDesligado);
        assertEquals(UserStatus.DESLIGADO, updated1.getUserStatus());

        UserRequestDTO dtoDisponivel = new UserRequestDTO();
        dtoDisponivel.setUserStatus("DISPONIVEL");
        User updated2 = userService.update(1L, dtoDisponivel);
        assertEquals(UserStatus.DISPONIVEL, updated2.getUserStatus());

        UserRequestDTO dtoNullStatus = new UserRequestDTO();
        dtoNullStatus.setUserStatus(null);
        User updated3 = userService.update(1L, dtoNullStatus);
        assertNotNull(updated3);
    }
}