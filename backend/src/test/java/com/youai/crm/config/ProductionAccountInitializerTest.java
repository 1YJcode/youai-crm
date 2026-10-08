package com.youai.crm.config;

import com.youai.crm.account.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductionAccountInitializerTest {
    private final CrmUserRepository users = mock(CrmUserRepository.class);
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);

    @Test
    void emptyInstallationRequiresExplicitCredentials() {
        when(users.findAllByOrderByDisplayNameAsc()).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> initializer("", "").run());
        verify(users, never()).save(any());
    }

    @Test
    void createsOnlyConfiguredAdministratorAndNeverResetsExistingAccount() {
        when(users.findAllByOrderByDisplayNameAsc()).thenReturn(List.of());
        Department department = new Department();
        Role role = new Role(); role.setCode("ADMIN");
        when(departments.findByCode("ADMIN")).thenReturn(Optional.of(department));
        when(roles.findByCode("ADMIN")).thenReturn(Optional.of(role));
        initializer("initial_admin", "A-long-random-password-2026!").run();
        var captured = org.mockito.ArgumentCaptor.forClass(CrmUser.class);
        verify(users).save(captured.capture());
        CrmUser saved = captured.getValue();
        assertEquals("initial_admin", saved.getUsername());
        assertTrue(passwords.matches("A-long-random-password-2026!", saved.getPasswordHash()));
        when(users.findAllByOrderByDisplayNameAsc()).thenReturn(List.of(saved));
        initializer("another_admin", "Another-long-password-2026!").run();
        verify(users, times(1)).save(any());
    }

    @Test
    void refusesEnabledLegacyDemoPasswordsWithoutChangingData() {
        CrmUser demo = new CrmUser();
        demo.setPasswordHash(passwords.encode("Admin2@123"));
        when(users.findAllByOrderByDisplayNameAsc()).thenReturn(List.of(demo));
        assertThrows(IllegalStateException.class, () -> initializer("", "").run());
        verify(users, never()).save(any());
    }

    private ProductionAccountInitializer initializer(String username, String password) {
        return new ProductionAccountInitializer(users, departments, roles, passwords, username, password);
    }
}
