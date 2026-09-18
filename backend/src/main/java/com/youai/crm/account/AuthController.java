package com.youai.crm.account;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return service.login(request, httpRequest.getRemoteAddr());
    }

    @PostMapping("/login/phone")
    public AuthResponse loginByPhone(@Valid @RequestBody PhoneLoginRequest request, HttpServletRequest httpRequest) {
        return service.loginByPhone(request, httpRequest.getRemoteAddr());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return service.refresh(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return service.me(authentication);
    }

    @GetMapping("/users")
    public List<UserResponse> users(Authentication authentication) {
        return service.users(authentication);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createEmployee(
            Authentication authentication,
            @Valid @RequestBody CreateEmployeeRequest request) {
        return service.createEmployee(authentication, request);
    }

    @PatchMapping("/users/{id}/password")
    public UserResponse resetEmployeePassword(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {
        return service.resetEmployeePassword(authentication, id, request);
    }

    @PatchMapping("/users/{id}")
    public UserResponse updateEmployee(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return service.updateEmployee(authentication, id, request);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(
            Authentication authentication,
            @PathVariable Long id) {
        service.deleteEmployee(authentication, id);
    }

    @PatchMapping("/users/{id}/freeze")
    public UserResponse freezeEmployee(
            Authentication authentication,
            @PathVariable Long id) {
        return service.freezeEmployee(authentication, id);
    }

    @PatchMapping("/users/{id}/unfreeze")
    public UserResponse unfreezeEmployee(
            Authentication authentication,
            @PathVariable Long id) {
        return service.unfreezeEmployee(authentication, id);
    }

    @PatchMapping("/users/{id}/unlock")
    public UserResponse unlockEmployee(
            Authentication authentication,
            @PathVariable Long id) {
        return service.unlockEmployee(authentication, id);
    }

    @PostMapping("/switch")
    public AuthResponse switchAccount(
            Authentication authentication,
            @RequestParam String username) {
        return service.switchAccount(authentication, username);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication) {
        service.logout(authentication);
    }
}
