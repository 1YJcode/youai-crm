package com.youai.crm.account;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Component
public class AccessPolicy {

    private final CrmUserRepository userRepository;

    public AccessPolicy(CrmUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    public String currentOwner(Authentication authentication) {
        if (isAdmin(authentication)) return null;
        return currentUser(authentication).getDisplayName();
    }

    public String currentDisplayName(Authentication authentication) {
        return currentUser(authentication).getDisplayName();
    }

    /**
     * Returns the stable database identity of the authenticated user.
     *
     * The display name is mutable and is not unique, so it must never be used
     * as the identity for an authorization decision.
     */
    public Long currentUserId(Authentication authentication) {
        return currentUser(authentication).getId();
    }

    /**
     * Checks ownership by user ID. Existing records may still contain a
     * legacy display name; that value is resolved to exactly one enabled user
     * before the IDs are compared. Ambiguous names are denied.
     */
    public boolean canAccessOwner(String owner, Authentication authentication) {
        if (isAdmin(authentication)) return true;
        if (authentication == null || !StringUtils.hasText(owner)) return false;
        try {
            Long currentId = currentUserId(authentication);
            Long ownerId = resolveOwnerId(owner);
            return currentId != null && ownerId != null && currentId.equals(ownerId);
        } catch (AccessDeniedException ex) {
            return false;
        }
    }

    public boolean canAccessUserId(Long ownerId, Authentication authentication) {
        if (isAdmin(authentication)) return true;
        return ownerId != null && authentication != null && ownerId.equals(currentUserId(authentication));
    }

    public void requireUserId(Long ownerId, Authentication authentication) {
        if (!canAccessUserId(ownerId, authentication)) throw new AccessDeniedException("只能操作本人负责的数据");
    }

    public void requireOwner(String owner, Authentication authentication) {
        if (!canAccessOwner(owner, authentication)) {
            throw new AccessDeniedException("只能操作本人负责的数据");
        }
    }

    public String scopedOwner(Authentication authentication) {
        return isAdmin(authentication) ? null : currentOwner(authentication);
    }

    private CrmUser currentUser(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("未登录");
        }
        if (authentication.getPrincipal() instanceof CrmPrincipal principal
                && principal.getUser().getId() != null
                && principal.getUser().isEnabled()) {
            return principal.getUser();
        }
        if (!StringUtils.hasText(authentication.getName())) {
            throw new AccessDeniedException("未登录");
        }
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .filter(CrmUser::isEnabled)
                .orElseThrow(() -> new AccessDeniedException("当前用户不存在或已停用"));
    }

    private Long resolveOwnerId(String owner) {
        String candidate = owner.trim();
        try {
            return Long.valueOf(candidate);
        } catch (NumberFormatException ignored) {
            // Backward compatibility for records written before owner IDs
            // were used. Authorization still compares the resolved IDs.
        }

        var matches = userRepository.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
                .filter(user -> candidate.equalsIgnoreCase(user.getDisplayName())
                        || candidate.equalsIgnoreCase(user.getUsername()))
                .map(CrmUser::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return matches.size() == 1 ? matches.getFirst() : null;
    }
}
