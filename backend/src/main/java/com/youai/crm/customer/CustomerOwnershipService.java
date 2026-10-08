package com.youai.crm.customer;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.account.CrmUser;
import com.youai.crm.account.UserIdentityResolver;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** Customer identity writes; labels remain presentation snapshots only. */
@Service
public class CustomerOwnershipService {
    private final UserIdentityResolver identities;
    private final AccessPolicy access;

    public CustomerOwnershipService(UserIdentityResolver identities, AccessPolicy access) {
        this.identities = identities;
        this.access = access;
    }

    public void assign(Customer customer, String reference) {
        if ("公海".equals(reference) || "白板".equals(reference)) {
            customer.setOwner(reference);
            customer.setOwnerId(null);
            customer.setCollaborator("");
            customer.setCollaboratorIds(java.util.Set.of());
        } else {
            assign(customer, identities.enabledEmployee(reference));
        }
    }

    public void assign(Customer customer, CrmUser user) {
        customer.setOwner(user.getDisplayName());
        customer.setOwnerId(user.getId());
    }

    public void assignCurrent(Customer customer, Authentication authentication) {
        customer.setOwner(access.currentDisplayName(authentication));
        customer.setOwnerId(access.currentUserId(authentication));
    }

    public void collaborators(Customer customer, String references) {
        var ids = new LinkedHashSet<Long>();
        var labels = new ArrayList<String>();
        if (references != null && !references.isBlank()) {
            for (String reference : references.split("[、,，]")) {
                if (reference.isBlank()) continue;
                CrmUser user = identities.enabledUser(reference);
                if (ids.add(user.getId())) labels.add(user.getDisplayName());
            }
        }
        customer.setCollaboratorIds(ids);
        customer.setCollaborator(String.join("、", labels));
    }
}
