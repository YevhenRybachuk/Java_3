package org.example.lab1.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.lab1.dto.SubscriptionRequestDto;
import org.example.lab1.dto.SubscriptionResponseDto;
import org.example.lab1.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<SubscriptionResponseDto> create(
            @RequestBody SubscriptionRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(subscriptionService.create(dto, jwt));
    }

    @GetMapping("/{id}")
    public SubscriptionResponseDto findById(@PathVariable Long id) {
        return subscriptionService.findById(id);
    }

    @GetMapping
    public List<SubscriptionResponseDto> findAllByUser(@RequestParam Long userId) {
        return subscriptionService.findAllByUser(userId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponseDto> update(
            @PathVariable Long id,
            @RequestBody SubscriptionRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(subscriptionService.update(id, dto, jwt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        subscriptionService.delete(id, jwt);
        return ResponseEntity.noContent().build();
    }
    /**
     * FilterChain дозволяє будь-якому автентифікованому користувачу (/api/**).
     * Доступ ADMIN перевіряється лише через {@code @PreAuthorize} у сервісі (AC7).
     */
    @DeleteMapping("/{id}/force")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forceDelete(@PathVariable Long id) {
        subscriptionService.deleteAnySubscription(id);
    }
}
