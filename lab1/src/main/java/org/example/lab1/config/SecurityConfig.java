package org.example.lab1.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Два окремі {@link SecurityFilterChain}: API (JWT resource server) і UI (OAuth2/OIDC login).
 * <p>
 * Порядок {@code @Order} критичний: Spring Security обирає перший ланцюг, чий
 * {@code securityMatcher} підходить до запиту. Конкретніший matcher {@code /api/**}
 * має йти з меншим {@code @Order} (першим). Інакше загальніший UI-ланцюг
 * (без matcher / з {@code /**}) перехопить і API-запити, і JWT-валідація не спрацює.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * Stateless API: Bearer JWT від Keycloak, без сесії та CSRF.
     * Має {@code @Order(1)}, бо {@code securityMatcher("/api/**")} конкретніший
     * за UI-ланцюг і повинен оцінюватися першим.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    /**
     * UI / browser-потоки: OAuth2 Login (OIDC) через Keycloak з application.yml.
     * {@code @Order(2)} — fallback для всього, що не потрапило в {@code apiFilterChain}.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain uiFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout
                        // Замість локального logoutSuccessUrl відправляємо запит у Keycloak на закриття SSO-сесії
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .build();
    }

    /**
     * Хендлер виходу з OIDC: здійснює редірект на ендпоінт Keycloak logout,
     * а після виходу повертає користувача на головну сторінку додатка.
     */
    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository repository) {
        OidcClientInitiatedLogoutSuccessHandler successHandler = new OidcClientInitiatedLogoutSuccessHandler(repository);
        successHandler.setPostLogoutRedirectUri("{baseUrl}/subscriptions");
        return successHandler;
    }

    /**
     * Keycloak кладе ролі в {@code realm_access.roles} без префікса {@code ROLE_}.
     * Spring Security очікує {@code ROLE_ADMIN} для {@code hasRole("ADMIN")}, тому додаємо префікс тут.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
                return List.of();
            }
            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(
                            role.startsWith("ROLE_") ? role : "ROLE_" + role))
                    .collect(Collectors.toList());
        });
        return converter;
    }
}