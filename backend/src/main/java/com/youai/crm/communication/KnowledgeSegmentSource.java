package com.youai.crm.communication;
import jakarta.persistence.*;
@Entity @Table(name="crm_knowledge_segment_source",uniqueConstraints=@UniqueConstraint(columnNames={"article_id","segment_id"}))
public class KnowledgeSegmentSource {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false) Long articleId;
    @Column(nullable=false) Long segmentId;
    protected KnowledgeSegmentSource() {}
    KnowledgeSegmentSource(Long articleId,Long segmentId){this.articleId=articleId;this.segmentId=segmentId;}
}
