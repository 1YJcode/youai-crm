package com.youai.crm.account;

import java.time.Instant;
import java.util.List;

import com.youai.crm.common.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final CrmUserRepository userRepository;
    private final JwtService jwtService;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            CrmUserRepository userRepository,
            JwtService jwtService,
            DepartmentRepository departmentRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        return tokenResponse((CrmPrincipal) authentication.getPrincipal());
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

    @Transactional(readOnly = true)
    public List<UserResponse> users(Authentication authentication) {
        requireAdmin(authentication);
        return userRepository.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
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
