package com.youai.crm.knowledge;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="crm_knowledge_article")
public class KnowledgeArticle {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public Long id;
    @Column(nullable=false, length=160) public String title;
    @Column(nullable=false, length=64) public String category;
    @Column(nullable=false, columnDefinition="LONGTEXT") public String content;
    @Column(nullable=false, length=128) public String createdBy;
    @Column(nullable=false, length=128) public String updatedBy;
    @Column(nullable=false) public LocalDateTime createdAt;
    @Column(nullable=false) public LocalDateTime updatedAt;
    @Version @Column(nullable=false) public Long version;
    @PrePersist void create() { createdAt=LocalDateTime.now(); updatedAt=createdAt; }
    @PreUpdate void update() { updatedAt=LocalDateTime.now(); }
}
