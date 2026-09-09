package com.youke.crm.account;

import com.youke.crm.common.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AccessPolicy {

    private final CrmUserRepository userRepository;

    public AccessPolicy(CrmUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication == null || authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    public String currentOwner(Authentication authentication) {
        if (authentication == null || isAdmin(authentication)) return null;
        return currentDisplayName(authentication);
    }

    public String currentDisplayName(Authentication authentication) {
        if (authentication == null) return "系统管理员";
        String username = authentication.getName();
        return userRepository.findByUsernameIgnoreCase(username)
                .map(CrmUser::getDisplayName)
                .orElse(username);
    }

    public void requireOwner(String owner, Authentication authentication) {
        if (isAdmin(authentication)) return;
        if (!StringUtils.hasText(owner) || !owner.equalsIgnoreCase(currentOwner(authentication))) {
            throw new AccessDeniedException("只能操作本人负责的数据");
        }
    }

    public String scopedOwner(Authentication authentication) {
        return isAdmin(authentication) ? null : currentOwner(authentication);
    }
}
