package com.youai.crm.knowledge;

import com.youai.crm.common.NotFoundException;
import com.youai.crm.common.ConflictException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeRepository articles;
    private final com.youai.crm.customer.CustomerAccessPolicy access;
    private final com.youai.crm.communication.CustomerTranscriptService transcripts;
    public KnowledgeController(KnowledgeRepository articles, com.youai.crm.customer.CustomerAccessPolicy access,com.youai.crm.communication.CustomerTranscriptService transcripts) { this.articles=articles; this.access=access;this.transcripts=transcripts; }
    private org.springframework.data.jpa.domain.Specification<KnowledgeArticle> visible(Authentication auth) {
        return (root,query,cb)->cb.or(cb.isNull(root.get("sourceCallId")),cb.isTrue(root.get("companyCase")),access.visibleReference(root.get("customerNo"),query,cb,auth));
    }
    public record ArticleRequest(@NotBlank @Size(max=160) String title,
            @NotBlank @Size(max=64) String category, @NotBlank @Size(max=100000) String content, Long version) {}
    public record Summary(Long id,String title,String category,String preview,String updatedBy,LocalDateTime updatedAt) {}
    public record CompanyCaseRequest(@NotBlank @Size(max=160) String title,
            @NotBlank @Size(max=64) String category,@NotBlank @Size(max=100000) String content,
            @NotNull Long version,@NotNull @AssertTrue Boolean sharingConfirmed) {}
    @ModelAttribute
    public void noCache(jakarta.servlet.http.HttpServletResponse response) { response.setHeader("Cache-Control","no-store"); }
    @GetMapping
    public Page<Summary> list(@RequestParam(defaultValue="") String keyword,
            @RequestParam(defaultValue="") String category, @RequestParam(defaultValue="0") int page, Authentication auth) {
        String term=keyword.trim().toLowerCase(java.util.Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
        org.springframework.data.jpa.domain.Specification<KnowledgeArticle> matching=(root,query,cb)->cb.and(
                category.isBlank()?cb.conjunction():cb.equal(root.get("category"),category.trim()),
                cb.or(cb.like(cb.lower(root.get("title")),"%"+term+"%",'!'),cb.like(cb.lower(root.get("content")),"%"+term+"%",'!')));
        return articles.findAll(visible(auth).and(matching),PageRequest.of(Math.max(0,page),12,Sort.by(Sort.Direction.DESC,"updatedAt").and(Sort.by(Sort.Direction.DESC,"id"))))
                .map(a->new Summary(a.id,a.title,a.category,a.content.substring(0,Math.min(a.content.length(),140)),a.updatedBy,a.updatedAt));
    }
    @GetMapping("/categories") public List<String> categories(Authentication auth) {return articles.findAll(visible(auth)).stream().map(a->a.category).distinct().sorted().toList();}
    @GetMapping("/{id}") public Map<String,Object> get(@PathVariable Long id, Authentication auth) {
        var article=find(id);
        if(article.sourceCallId!=null && !article.companyCase) access.requireAccess(article.customerNo,auth);
        return view(article,auth);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> create(@Valid @RequestBody ArticleRequest request,Authentication auth) {
        var article=new KnowledgeArticle(); article.createdBy=auth.getName();
        apply(article,request,auth); return view(articles.saveAndFlush(article),auth);
    }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional
    public Map<String,Object> update(@PathVariable Long id,@Valid @RequestBody ArticleRequest request,Authentication auth) {
        var article=find(id);
        if(request.version()==null || !request.version().equals(article.version)) throw new ConflictException("知识内容已更新，请重新打开后编辑");
        apply(article,request,auth); return view(articles.saveAndFlush(article),auth);
    }
    @PostMapping("/{id}/company-case") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public Map<String,Object> publishCompanyCase(@PathVariable Long id,@Valid @RequestBody CompanyCaseRequest request,Authentication auth) {
        var source=articles.lockById(id).orElseThrow(()->new NotFoundException("未找到来源案例"));
        if(source.sourceCallId==null || source.companyCase)
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"请选择客户通话的私有来源案例");
        access.requireAccess(source.customerNo,auth);
        if(!request.version().equals(source.version)) throw new ConflictException("来源内容已更新，请重新打开后整理");
        if(articles.findByDerivedFromId(source.id).isPresent()) throw new ConflictException("此来源已经整理为公司案例，请在知识库查看");
        var article=new KnowledgeArticle(); article.companyCase=true; article.derivedFromId=source.id;
        article.sourceCallId=source.sourceCallId; article.customerNo=source.customerNo;
        article.customerName=source.customerName; article.salesUserId=source.salesUserId; article.salesName=source.salesName;
        article.recordingRevision=source.recordingRevision; article.recordingSource=source.recordingSource;
        article.transcriptionProvider=source.transcriptionProvider; article.callStartedAt=source.callStartedAt;
        article.recordingUploadedAt=source.recordingUploadedAt; article.transcribedAt=source.transcribedAt;
        article.createdBy=auth.getName(); article.updatedBy=auth.getName();
        article.title=request.title().trim(); article.category=request.category().trim(); article.content=request.content().trim();
        articles.saveAndFlush(article);transcripts.copySources(source.id,article.id);return view(article,auth);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") @Transactional
    public void delete(@PathVariable Long id,@RequestParam Long version) {
        var article=find(id);
        if(!version.equals(article.version)) throw new ConflictException("知识内容已更新，请重新打开后删除");
        articles.delete(article); articles.flush();
    }
    private KnowledgeArticle find(Long id) {return articles.findById(id).orElseThrow(()->new NotFoundException("未找到知识内容"));}
    private Map<String,Object> view(KnowledgeArticle article,Authentication auth) {
        // Whitelist public fields. A shared case never returns source identifiers to other staff.
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("id",article.id); result.put("title",article.title); result.put("category",article.category);
        result.put("content",article.content); result.put("version",article.version); result.put("companyCase",article.companyCase);
        result.put("createdBy",article.createdBy); result.put("createdAt",article.createdAt);
        result.put("updatedBy",article.updatedBy); result.put("updatedAt",article.updatedAt);
        boolean canAccessSource=article.sourceCallId!=null && access.canAccess(article.customerNo,auth);
        result.put("canAccessSource",canAccessSource); result.put("canPublishCase",canAccessSource && !article.companyCase);
        if(canAccessSource) {
            result.put("sourceSegments",transcripts.sources(article.id));
            result.put("sourceCallId",article.sourceCallId); result.put("recordingRevision",article.recordingRevision);
            result.put("recordingSource",article.recordingSource); result.put("transcriptionProvider",article.transcriptionProvider);
            result.put("customerNo",article.customerNo); result.put("customerName",article.customerName);
            result.put("salesUserId",article.salesUserId); result.put("salesName",article.salesName);
            result.put("callStartedAt",article.callStartedAt); result.put("recordingUploadedAt",article.recordingUploadedAt);
            result.put("transcribedAt",article.transcribedAt); result.put("analyzedAt",article.analyzedAt); result.put("aiStatus",article.aiStatus);
            if(article.companyCase) result.put("sourceArticleId",article.derivedFromId);
            else articles.findByDerivedFromId(article.id).ifPresent(shared->result.put("companyCaseId",shared.id));
        }
        return result;
    }
    private void apply(KnowledgeArticle article,ArticleRequest request,Authentication auth) {
        article.title=request.title().trim(); article.category=request.category().trim();
        article.content=request.content().trim(); article.updatedBy=auth.getName();
    }
}
