package co.edu.uniquindio.fintech_backend.auth.security;

import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Objects;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final FintechAuthenticationProvider authenticationProvider;
    private final UserRepository userRepository;

    public SecurityConfig(FintechAuthenticationProvider authenticationProvider, UserRepository userRepository) {
        this.authenticationProvider = Objects.requireNonNull(authenticationProvider);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authenticationProvider(authenticationProvider).authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/api/health", "/api/auth/csrf").permitAll().requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll().requestMatchers(HttpMethod.GET, "/api/auth/me").access((authentication, context) -> new AuthorizationDecision(hasAccess(authentication.get(), false))).requestMatchers("/api/users/**").access((authentication, context) -> new AuthorizationDecision(hasAccess(authentication.get(), true))).requestMatchers("/api/wallets", "/api/wallets/**").access((authentication, context) -> new AuthorizationDecision(hasAccess(authentication.get(), false))).anyRequest().denyAll()).csrf(csrf -> {
        }).formLogin(form -> form.loginProcessingUrl("/api/auth/login").usernameParameter("email").successHandler((request, response, authentication) -> {
            response.setStatus(200);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Authenticated\"}");
        }).failureHandler((request, response, exception) -> {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Invalid credentials\"}");
        })).logout(logout -> logout.logoutUrl("/api/auth/logout").invalidateHttpSession(true).clearAuthentication(true).logoutSuccessHandler((request, response, authentication) -> response.setStatus(204))).exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> response.sendError(401)).accessDeniedHandler((request, response, exception) -> response.sendError(403))).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).sessionFixation(fixation -> fixation.changeSessionId())).httpBasic(basic -> basic.disable());
        return http.build();
    }

    private boolean hasAccess(Authentication authentication, boolean adminOnly) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        try {
            UUID id = UUID.fromString(authentication.getName());
            return userRepository.findById(id).filter(User::isActive).filter(user -> !adminOnly || user.getRole() == Role.ADMIN || user.getRole() == Role.SUPERADMIN).isPresent();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
