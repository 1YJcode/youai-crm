package com.youai.crm.ledger;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount,Long>, JpaSpecificationExecutor<LedgerAccount> { Optional<LedgerAccount> findByLedgerNo(String ledgerNo); boolean existsByOrderNo(String orderNo); }
