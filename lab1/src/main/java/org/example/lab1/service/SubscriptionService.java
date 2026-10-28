package org.example.lab1.service;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.lab1.dto.SubscriptionRequestDto;
import org.example.lab1.dto.SubscriptionResponseDto;
import org.example.lab1.annotation.AuditOperation;
import org.example.lab1.annotation.MeasureExecutionTime;
import org.example.lab1.entity.Category;
import org.example.lab1.entity.Subscription;
import org.example.lab1.entity.User;
import org.example.lab1.mapper.SubscriptionMapper;
import org.example.lab1.repository.CategoryRepository;
import org.example.lab1.repository.SubscriptionRepository;
import org.example.lab1.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubscriptionMapper subscriptionMapper;


    @Transactional
    public SubscriptionResponseDto create(SubscriptionRequestDto requestDto, OidcUser principal) {
        User currentUser = findOrCreateUser(principal);
        return processCreate(requestDto, currentUser, isAdmin(principal));
    }

    @Transactional
    public SubscriptionResponseDto update(Long id, SubscriptionRequestDto requestDto, OidcUser principal) {
        User currentUser = findOrCreateUser(principal);
        return processUpdate(id, requestDto, currentUser, isAdmin(principal));
    }

    @Transactional
    public void delete(Long id, OidcUser principal) {
        User currentUser = findOrCreateUser(principal);
        processDelete(id, currentUser, isAdmin(principal));
    }


    @Transactional
    public SubscriptionResponseDto create(SubscriptionRequestDto requestDto, Jwt jwt) {
        User currentUser = findOrCreateUser(jwt);
        return processCreate(requestDto, currentUser, isAdmin(jwt));
    }

    @Transactional
    public SubscriptionResponseDto update(Long id, SubscriptionRequestDto requestDto, Jwt jwt) {
        User currentUser = findOrCreateUser(jwt);
        return processUpdate(id, requestDto, currentUser, isAdmin(jwt));
    }

    @Transactional
    public void delete(Long id, Jwt jwt) {
        User currentUser = findOrCreateUser(jwt);
        processDelete(id, currentUser, isAdmin(jwt));
    }


    @Transactional(readOnly = true)
    public SubscriptionResponseDto findById(Long id) {
        return subscriptionMapper.toDto(findSubscriptionById(id));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponseDto> findAll() {
        return subscriptionRepository.findAll().stream()
                .map(subscriptionMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    @MeasureExecutionTime
    public List<SubscriptionResponseDto> findAllByUser(Long userId) {
        return subscriptionRepository.findAllByUserId(userId).stream()
                .map(subscriptionMapper::toDto)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @AuditOperation
    public void deleteAnySubscription(Long id) {
        Subscription subscription = findSubscriptionById(id);
        subscriptionRepository.delete(subscription);
    }


    private SubscriptionResponseDto processCreate(SubscriptionRequestDto requestDto, User currentUser, boolean isAdmin) {
          if (!isAdmin && requestDto.getUserId() != null && !requestDto.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Заборонено створювати підписки для інших користувачів!");
        }

        Subscription subscription = subscriptionMapper.toEntity(requestDto);

        if (isAdmin && requestDto.getUserId() != null) {
            subscription.setUser(findUserById(requestDto.getUserId()));
        } else {
            subscription.setUser(currentUser);
        }

        subscription.setCategory(findCategoryById(requestDto.getCategoryId()));
        return subscriptionMapper.toDto(subscriptionRepository.save(subscription));
    }

    private SubscriptionResponseDto processUpdate(Long id, SubscriptionRequestDto requestDto, User currentUser, boolean isAdmin) {
        Subscription subscription = findSubscriptionById(id);

        if (!isAdmin && !subscription.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Ви не можете редагувати чужу підписку!");
        }

        if (!isAdmin && requestDto.getUserId() != null && !requestDto.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Ви не можете змінювати власника підписки!");
        }

        subscriptionMapper.updateEntityFromDto(requestDto, subscription);

        if (isAdmin && requestDto.getUserId() != null) {
            subscription.setUser(findUserById(requestDto.getUserId()));
        }

        subscription.setCategory(findCategoryById(requestDto.getCategoryId()));
        return subscriptionMapper.toDto(subscriptionRepository.save(subscription));
    }

    private void processDelete(Long id, User currentUser, boolean isAdmin) {
        Subscription subscription = findSubscriptionById(id);

        if (!isAdmin && !subscription.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Ви не можете видалити чужу підписку");
        }

        subscriptionRepository.delete(subscription);
    }


    private User findOrCreateUser(OidcUser principal) {
        String username = getUsername(principal);
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);

                    String email = principal.getEmail();
                    newUser.setEmail(email != null && !email.isBlank() ? email : username + "@example.com");
                    newUser.setRole(isAdmin(principal) ? "ADMIN" : "USER");

                    return userRepository.save(newUser);
                });
    }

    private User findOrCreateUser(Jwt jwt) {
        String username = getUsername(jwt);
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);

                    String email = jwt.getClaimAsString("email");
                    newUser.setEmail(email != null && !email.isBlank() ? email : username + "@example.com");
                    newUser.setRole(isAdmin(jwt) ? "ADMIN" : "USER");

                    return userRepository.save(newUser);
                });
    }

    private String getUsername(OidcUser principal) {
        if (principal.getPreferredUsername() != null) {
            return principal.getPreferredUsername();
        }
        return principal.getName();
    }

    private String getUsername(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return username != null ? username : jwt.getSubject();
    }

    private boolean isAdmin(OidcUser principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
    }

    @SuppressWarnings("unchecked")
    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && roles.contains("ADMIN")) {
            return true;
        }
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            return realmRoles.contains("ADMIN") || realmRoles.contains("admin");
        }
        return false;
    }

    private Subscription findSubscriptionById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Subscription with id " + id + " was not found"));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with id " + id + " was not found"));
    }

    private Category findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category with id " + id + " was not found"));
    }
}
