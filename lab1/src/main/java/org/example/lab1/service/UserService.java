package org.example.lab1.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.lab1.entity.User;
import org.example.lab1.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }
}
