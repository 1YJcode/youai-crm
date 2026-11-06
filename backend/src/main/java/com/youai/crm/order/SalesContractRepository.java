package com.youai.crm.order;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface SalesContractRepository extends JpaRepository<SalesContract,Long>, JpaSpecificationExecutor<SalesContract> {
 Optional<SalesContract> findByContractNo(String no);
 @Query("select distinct c.order.owner from SalesContract c where (:admin = true or c.order.ownerId = :userId or c.order.confirmationSalesId = :userId) order by c.order.owner")
 java.util.List<String> visibleOwners(@org.springframework.data.repository.query.Param("admin") boolean admin,
   @org.springframework.data.repository.query.Param("userId") Long userId);
}
