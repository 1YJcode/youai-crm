package com.youke.crm.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmUserRepository extends JpaRepository<CrmUser, Long> {

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<CrmUser> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {"department", "roles"})
    List<CrmUser> findAllByEnabledTrueOrderByDisplayNameAsc();
}

