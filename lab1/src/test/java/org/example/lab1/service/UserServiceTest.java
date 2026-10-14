package org.example.lab1.service;

import org.example.lab1.entity.User;
import org.example.lab1.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("findAll: коли користувачі існують — повертає список усіх користувачів")
    void findAll_whenUsersExist_returnsUserList() {
        // Arrange
        User user1 = new User(1L, "evgen", "evgen@mail.com", "USER");
        User user2 = new User(2L, "admin", "admin@mail.com", "ADMIN");
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        // Act
        List<User> result = userService.findAll();

        // Assert
        assertEquals(2, result.size());
        assertEquals("evgen", result.get(0).getUsername());
        assertEquals("admin", result.get(1).getUsername());
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("findAll: коли користувачів немає — повертає порожній список")
    void findAll_whenNoUsersExist_returnsEmptyList() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of());

        // Act
        List<User> result = userService.findAll();

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository).findAll();
    }
}