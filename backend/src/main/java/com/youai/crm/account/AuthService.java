package com.youai.crm.account;

import java.time.Instant;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.youai.crm.common.NotFoundException;
import com.youai.crm.customer.CustomerRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {
    private static final Logger SECURITY_AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final AuthenticationManager authenticationManager;
    private final CrmUserRepository userRepository;
    private final JwtService jwtService;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginProtectionService loginProtection;
    private final CustomerRepository customerRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            CrmUserRepository userRepository,
            JwtService jwtService,
            DepartmentRepository departmentRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            LoginProtectionService loginProtection,
            CustomerRepository customerRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginProtection = loginProtection;
        this.customerRepository = customerRepository;
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        String username = request.username().trim();
        return authenticate(username, request.password(), clientIp);
    }

    public AuthResponse loginByPhone(PhoneLoginRequest request, String clientIp) {
        String phone = request.phone().trim();
        loginProtection.recordIpAttempt(clientIp);
        CrmUser account = userRepository.findByPhone(phone)
                .orElse(null);
        if (account == null) {
            loginProtection.checkAccount(phone);
            loginProtection.recordFailure(phone);
            throw new AccountUnavailableException();
        }
        return authenticate(account.getUsername(), request.password(), clientIp, false);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        final var claims = parseRefreshToken(request.refreshToken());
        String username = claims.getSubject();
        Object versionClaim = claims.get("credentialVersion");
        if (!(versionClaim instanceof Number)) {
            throw invalidRefreshToken();
        }
        CrmUser account = userRepository.findByUsernameIgnoreCase(username)
                .filter(CrmUser::isEnabled)
                .orElseThrow(this::invalidRefreshToken);
        if (account.getCredentialVersion() != ((Number) versionClaim).longValue()) {
            throw invalidRefreshToken();
        }
        return tokenResponse(new CrmPrincipal(account));
    }

    private io.jsonwebtoken.Claims parseRefreshToken(String token) {
        try {
            var claims = jwtService.parse(token);
            if (!JwtService.REFRESH_TOKEN_TYPE.equals(jwtService.tokenType(claims))) {
                throw invalidRefreshToken();
            }
            return claims;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException exception) {
            throw invalidRefreshToken();
        }
    }

    private BadCredentialsException invalidRefreshToken() {
        return new BadCredentialsException("refresh token 无效或已过期");
    }

    private AuthResponse authenticate(String username, String password, String clientIp) {
        return authenticate(username, password, clientIp, true);
    }

    private AuthResponse authenticate(String username, String password, String clientIp, boolean recordAttempt) {
        try {
            // Apply the same account/IP protections before lookup so unknown
            // usernames cannot bypass brute-force throttling.
            if (recordAttempt) loginProtection.checkAndRecordAttempt(clientIp, username);
            else loginProtection.checkAccount(username);
            CrmUser account = userRepository.findByUsernameIgnoreCase(username)
                    .orElse(null);
            if (account == null) {
                loginProtection.recordFailure(username);
                throw new AccountUnavailableException();
            }
            if (!account.isEnabled()) {
                throw new AccountFrozenException();
            }
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            loginProtection.recordSuccess(username);
            SECURITY_AUDIT.info("event=login_success username={} ip={}", auditValue(username), auditValue(clientIp));
            return tokenResponse((CrmPrincipal) authentication.getPrincipal());
        } catch (LoginRateLimitException exception) {
            SECURITY_AUDIT.warn("event=login_blocked username={} ip={} retryAfterSeconds={}",
                    auditValue(username), auditValue(clientIp), exception.getRetryAfterSeconds());
            throw exception;
        } catch (AuthenticationException exception) {
            loginProtection.recordFailure(username);
            SECURITY_AUDIT.warn("event=login_failure username={} ip={} reason={}",
                    auditValue(username), auditValue(clientIp), exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private String auditValue(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", "_");
    }

    public AuthResponse register(RegisterRequest request) {
        throw new AccessDeniedException("不支持自行注册，请联系管理员开通账号");
    }

    public UserResponse me(Authentication authentication) {
        return principal(authentication).response();
    }

    @Transactional
    public UserResponse createEmployee(Authentication authentication, CreateEmployeeRequest request) {
        requireAdmin(authentication);
        String username = request.username().trim();
        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new IllegalArgumentException("账号已存在，请换一个账号");
        }
        Department salesDepartment = departmentRepository.findByCode("SALES")
                .orElseThrow(() -> new IllegalStateException("销售部门尚未初始化"));
        Role salesRole = roleRepository.findByCode("SALES")
                .orElseThrow(() -> new IllegalStateException("销售角色尚未初始化"));
        CrmUser user = new CrmUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setPhone(request.phone() == null ? "" : request.phone().trim());
        user.setDepartment(salesDepartment);
        Set<Role> roles = new LinkedHashSet<>();
        if (request.roles() == null || request.roles().isEmpty()) {
            roles.add(salesRole);
        } else {
            for (String requestedCode : request.roles()) {
                String code = requestedCode == null ? "" : requestedCode.trim().toUpperCase(Locale.ROOT);
                if (code.isBlank()) throw new IllegalArgumentException("角色不能为空");
                roles.add(roleRepository.findByCode(code)
                        .orElseThrow(() -> new IllegalArgumentException("角色不存在：" + requestedCode)));
            }
        }
        user.setRoles(roles);
        user.setEnabled(true);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse resetEmployeePassword(
        Authentication authentication, Long id, ResetPasswordRequest request) {
        requireAdmin(authentication);
        CrmUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("员工用户不存在"));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        loginProtection.unlock(user.getUsername());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateEmployee(
            Authentication authentication, Long id, UpdateEmployeeRequest request) {
        requireAdmin(authentication);
        CrmUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("员工用户不存在"));
        String username = request.username().trim();
        userRepository.findByUsernameIgnoreCase(username).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("账号已存在，请换一个账号");
            }
        });
        List<String> requestedCodes = request.roles() == null ? List.of() : request.roles();
        Set<Role> roles = new LinkedHashSet<>();
        for (String requestedCode : requestedCodes) {
            String code = requestedCode == null ? "" : requestedCode.trim().toUpperCase(Locale.ROOT);
            if (code.isBlank()) continue;
            Role role = roleRepository.findByCode(code)
                    .orElseThrow(() -> new IllegalArgumentException("角色不存在：" + requestedCode));
            roles.add(role);
        }
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("至少选择一个角色");
        }
        user.setUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setPhone(request.phone() == null ? "" : request.phone().trim());
        user.setRoles(roles);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteEmployee(Authentication authentication, Long id) {
        requireAdmin(authentication);
        CrmUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("员工用户不存在"));
        boolean administrator = user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getCode()));
        if (administrator) {
            throw new IllegalArgumentException("管理员账号不能删除");
        }
        long customerCount = customerRepository.countByOwnerId(user.getId());
        if (customerCount > 0) {
            throw new IllegalArgumentException("该员工名下仍有 " + customerCount + " 位客户，请先办理离职继承");
        }
        userRepository.delete(user);
        SECURITY_AUDIT.info("event=employee_deleted operator={} target={}",
                auditValue(authentication.getName()), auditValue(user.getUsername()));
    }

    @Transactional
    public UserResponse unlockEmployee(Authentication authentication, Long id) {
        requireAdmin(authentication);
        CrmUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("员工用户不存在"));
        loginProtection.unlock(user.getUsername());
        SECURITY_AUDIT.info("event=employee_login_unlocked operator={} target={}",
                auditValue(authentication.getName()), auditValue(user.getUsername()));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse freezeEmployee(Authentication authentication, Long id) {
        requireAdmin(authentication);
        CrmUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("员工用户不存在"));
        boolean administrator = user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getCode()));
        if (administrator) {
            throw new IllegalArgumentException("管理员账号不能冻结");
        }
        if (user.isEnabled()) {
            user.setEnabled(false);
            user.setCredentialVersion(user.getCredentialVersion() + 1);
            userRepository.save(user);
            SECURITY_AUDIT.info("event=employee_frozen operator={} target={}",
                    auditValue(authentication.getName()), auditValue(user.getUsername()));
        }
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse unfreezeEmployee(Authentication authentication, Long id) {
        requireAdmin(authentication);
        CrmUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("员工用户不存在"));
        if (!user.isEnabled()) {
            user.setEnabled(true);
            // Keep every token issued before the status transition invalid.
            user.setCredentialVersion(user.getCredentialVersion() + 1);
            loginProtection.unlock(user.getUsername());
            userRepository.save(user);
            SECURITY_AUDIT.info("event=employee_unfrozen operator={} target={}",
                    auditValue(authentication.getName()), auditValue(user.getUsername()));
        }
        return UserResponse.from(user);
    }

    @Transactional
    public void logout(Authentication authentication) {
        CrmPrincipal currentPrincipal = principal(authentication);
        CrmUser user = userRepository.findById(currentPrincipal.getUser().getId())
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        userRepository.save(user);
        SECURITY_AUDIT.info("event=logout username={}", auditValue(user.getUsername()));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> users(Authentication authentication) {
        requireAdmin(authentication);
        return userRepository.findAllByOrderByDisplayNameAsc().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuthResponse switchAccount(Authentication authentication, String username) {
        requireAdmin(authentication);
        CrmUser user = userRepository.findByUsernameIgnoreCase(username)
                .filter(CrmUser::isEnabled)
                .orElseThrow(() -> new NotFoundException("目标账号不存在或已停用"));
        return tokenResponse(new CrmPrincipal(user));
    }

    private AuthResponse tokenResponse(CrmPrincipal principal) {
        String accessToken = jwtService.issueAccess(principal.getUser());
        String refreshToken = jwtService.issueRefresh(principal.getUser());
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.expiresAt(accessToken),
                jwtService.expiresAt(refreshToken),
                principal.response());
    }

    private CrmPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CrmPrincipal principal)) {
            throw new AccessDeniedException("未登录");
        }
        return principal;
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("只有管理员可以执行此操作");
        }
    }
}
