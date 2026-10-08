package org.example.lab1.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Демонстрація AC6: шлях /api/admin/** захищений лише в SecurityFilterChain (hasRole ADMIN).
 * USER з валідним JWT отримає 403 ще до контролера.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminTestController {

    @GetMapping("/ping")
    public String ping() {
        return "Only admins see this";
    }
}