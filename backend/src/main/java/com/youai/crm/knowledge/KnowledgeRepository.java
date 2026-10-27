package com.youai.crm.knowledge;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.List;

public interface KnowledgeRepository extends JpaRepository<KnowledgeArticle,Long>, JpaSpecificationExecutor<KnowledgeArticle> {
    java.util.Optional<KnowledgeArticle> findBySourceKey(String sourceKey);
    java.util.Optional<KnowledgeArticle> findByDerivedFromId(Long derivedFromId);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from KnowledgeArticle a where a.id=:id")
    java.util.Optional<KnowledgeArticle> lockById(Long id);
    @Query("select a from KnowledgeArticle a where (:category='' or a.category=:category) and (lower(a.title) like :keyword escape '!' or lower(a.content) like :keyword escape '!')")
    Page<KnowledgeArticle> search(String keyword, String category, Pageable pageable);
    @Query("select distinct a.category from KnowledgeArticle a order by a.category")
    List<String> categories();
}
