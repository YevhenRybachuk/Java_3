package org.example.lab1.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.lab1.annotation.CurrentJwt;
import org.example.lab1.annotation.PostCreatedMapping;
import org.example.lab1.dto.SubscriptionRequestDto;
import org.example.lab1.dto.SubscriptionResponseDto;
import org.example.lab1.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // Тип 2 (@PostCreatedMapping замість @PostMapping)
    // Тип 1 (@Valid активує @ValidSubscriptionRequest на DTO)
    // Тип 3 (@CurrentJwt замість @AuthenticationPrincipal)
    @PostCreatedMapping
    public SubscriptionResponseDto create(
            @Valid @RequestBody SubscriptionRequestDto dto,
            @CurrentJwt Jwt jwt) {
        return subscriptionService.create(dto, jwt);
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
    public SubscriptionResponseDto update(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionRequestDto dto,
            @CurrentJwt Jwt jwt) {
        return subscriptionService.update(id, dto, jwt);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @CurrentJwt Jwt jwt) {
        subscriptionService.delete(id, jwt);
    }

    @DeleteMapping("/{id}/force")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forceDelete(@PathVariable Long id) {
        subscriptionService.deleteAnySubscription(id);
    }
}