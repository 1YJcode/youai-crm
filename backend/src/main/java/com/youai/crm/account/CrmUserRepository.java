package com.youai.crm.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmUserRepository extends JpaRepository<CrmUser, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from CrmUser u where u.id=:id")
    Optional<CrmUser> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<CrmUser> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<CrmUser> findByPhone(String phone);

    @EntityGraph(attributePaths = {"department", "roles"})
    List<CrmUser> findAllByEnabledTrueOrderByDisplayNameAsc();

    @EntityGraph(attributePaths = {"department", "roles"})
    List<CrmUser> findAllByOrderByDisplayNameAsc();
}

