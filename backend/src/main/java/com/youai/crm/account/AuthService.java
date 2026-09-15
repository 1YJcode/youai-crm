package com.youai.crm.account;

import java.time.Instant;
import java.util.List;

import com.youai.crm.common.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
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

    public AuthService(
            AuthenticationManager authenticationManager,
            CrmUserRepository userRepository,
            JwtService jwtService,
            DepartmentRepository departmentRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            LoginProtectionService loginProtection) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginProtection = loginProtection;
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        String username = request.username().trim();
        try {
            loginProtection.checkAndRecordAttempt(clientIp, username);
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password()));
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

    @Transactional
    public AuthResponse register(RegisterRequest request) {
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
        user.setRoles(java.util.Set.of(salesRole));
        user.setEnabled(true);
        return tokenResponse(new CrmPrincipal(userRepository.save(user)));
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
        user.setRoles(java.util.Set.of(salesRole));
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
        return UserResponse.from(userRepository.save(user));
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
        String token = jwtService.issue(principal.getUser());
        return new AuthResponse(token, "Bearer", jwtService.expiresAt(token), principal.response());
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
