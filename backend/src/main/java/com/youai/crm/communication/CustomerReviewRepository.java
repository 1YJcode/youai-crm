package com.youai.crm.communication;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
public interface CustomerReviewRepository extends JpaRepository<CustomerReview,Long> {
    Optional<CustomerReview> findByCustomerNoAndFingerprint(String customerNo,String fingerprint);
    Optional<CustomerReview> findFirstByCustomerNoOrderByIdDesc(String customerNo);
    Optional<CustomerReview> findFirstByCustomerNoAndStateOrderByIdDesc(String customerNo,String state);
    List<CustomerReview> findByCustomerNoOrderByIdDesc(String customerNo,Pageable page);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select r from CustomerReview r where r.id=:id")
    Optional<CustomerReview> lockById(Long id);
    @Query("select r.id from CustomerReview r where r.state='QUEUED' or (r.state='PROCESSING' and r.leaseUntil<CURRENT_TIMESTAMP) order by r.id")
    List<Long> pending(Pageable page);
}
