package com.youai.crm.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

class AccessPolicyTest {

    private CrmUserRepository userRepository;
    private AccessPolicy policy;
    private CrmUser linxi;
    private CrmUser anotherUser;

    @BeforeEach
    void setUp() {
        userRepository = mock(CrmUserRepository.class);
        policy = new AccessPolicy(userRepository);
        linxi = user(1L, "linxi", "同名负责人");
        anotherUser = user(2L, "another", "同名负责人");
        when(userRepository.findByUsernameIgnoreCase("linxi")).thenReturn(Optional.of(linxi));
        when(userRepository.findAllByEnabledTrueOrderByDisplayNameAsc())
                .thenReturn(List.of(linxi, anotherUser));
    }

    @Test
    void nullAuthenticationIsNeverTreatedAsAdmin() {
        assertThat(policy.isAdmin(null)).isFalse();
        assertThatThrownBy(() -> policy.scopedOwner(null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void ownerAuthorizationUsesStableUserIdAndRejectsAmbiguousNames() {
        var authentication = new TestingAuthenticationToken("linxi", null, "ROLE_SALES");

        assertThat(policy.canAccessOwner("1", authentication)).isTrue();
        assertThat(policy.canAccessOwner("2", authentication)).isFalse();
        assertThat(policy.canAccessOwner("同名负责人", authentication)).isFalse();
    }

    @Test
    void missingAuthenticationCannotPassOwnerCheck() {
        assertThatThrownBy(() -> policy.requireOwner("1", null))
                .isInstanceOf(AccessDeniedException.class);
    }

    private CrmUser user(Long id, String username, String displayName) {
        CrmUser user = mock(CrmUser.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn(username);
        when(user.getDisplayName()).thenReturn(displayName);
        when(user.isEnabled()).thenReturn(true);
        return user;
    }
}
