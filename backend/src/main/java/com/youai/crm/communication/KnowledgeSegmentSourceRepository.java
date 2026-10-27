package com.youai.crm.communication;
import org.springframework.data.jpa.repository.*;
public interface KnowledgeSegmentSourceRepository extends JpaRepository<KnowledgeSegmentSource,Long> {
    @Query("select s from CustomerTranscriptSegment s,KnowledgeSegmentSource l where l.articleId=:articleId and l.segmentId=s.id order by s.sourceTime,s.id")
    java.util.List<CustomerTranscriptSegment> sources(Long articleId);
}
