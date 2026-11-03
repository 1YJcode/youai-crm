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
        return (root,query,cb)->cb.or(cb.and(cb.isTrue(root.get("companyCase")),cb.equal(root.get("createdBy"),auth.getName())),admin(auth)?cb.conjunction():cb.disjunction(),cb.and(cb.equal(root.get("publicationStatus"),"PUBLISHED"),cb.or(cb.isNull(root.get("sourceCallId")),cb.isTrue(root.get("companyCase")),access.visibleReference(root.get("customerNo"),query,cb,auth))));
    }
    public record ArticleRequest(@NotBlank @Size(max=160) String title,
            @NotBlank @Size(max=64) String category, @NotBlank @Size(max=100000) String content, Long version) {}
    public record Summary(Long id,String title,String category,String preview,String publicationStatus,String updatedBy,LocalDateTime updatedAt) {}
    public record CompanyCaseRequest(@NotBlank @Size(max=160) String title,
            @NotBlank @Size(max=64) String category,@NotBlank @Size(max=100000) String content,
            @NotNull Long version,@NotNull @AssertTrue Boolean sharingConfirmed) {}
    @ModelAttribute
    public void noCache(jakarta.servlet.http.HttpServletResponse response) { response.setHeader("Cache-Control","no-store"); }
    @GetMapping
    public Page<Summary> list(@RequestParam(defaultValue="") String keyword,
            @RequestParam(defaultValue="") String category, @RequestParam(defaultValue="") String publicationStatus, @RequestParam(defaultValue="0") int page, Authentication auth) {
        String term=keyword.trim().toLowerCase(java.util.Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
        org.springframework.data.jpa.domain.Specification<KnowledgeArticle> matching=(root,query,cb)->cb.and(
                (publicationStatus.isBlank()?cb.conjunction():cb.equal(root.get("publicationStatus"),publicationStatus)),
                category.isBlank()?cb.conjunction():cb.equal(root.get("category"),category.trim()),
                cb.or(cb.like(cb.lower(root.get("title")),"%"+term+"%",'!'),cb.like(cb.lower(root.get("content")),"%"+term+"%",'!')));
        return articles.findAll(visible(auth).and(matching),PageRequest.of(Math.max(0,page),12,Sort.by(Sort.Direction.DESC,"updatedAt").and(Sort.by(Sort.Direction.DESC,"id"))))
                .map(a->new Summary(a.id,a.title,a.category,a.content.substring(0,Math.min(a.content.length(),140)),a.publicationStatus,a.updatedBy,a.updatedAt));
    }
    @GetMapping("/categories") public List<String> categories(Authentication auth) {return articles.findAll(visible(auth)).stream().map(a->a.category).distinct().sorted().toList();}
    @GetMapping("/{id}") public Map<String,Object> get(@PathVariable Long id, Authentication auth) {
        var article=find(id);
        if(article.sourceCallId!=null && !article.companyCase) access.requireAccess(article.customerNo,auth);
        if(!article.isPublished() && !admin(auth) && !auth.getName().equals(article.createdBy)) throw new org.springframework.security.access.AccessDeniedException("Knowledge is not published");
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
        var article=new KnowledgeArticle(); article.reviewedSourceVersion=source.version; article.companyCase=true; article.publicationStatus="PENDING"; article.derivedFromId=source.id;
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
    @PutMapping("/{id}/resubmit") @Transactional
    public Map<String,Object> resubmit(@PathVariable Long id,@Valid @RequestBody ArticleRequest request,Authentication auth){
        var a=articles.lockById(id).orElseThrow(()->new NotFoundException("Knowledge not found"));
        if(!admin(auth)&&!auth.getName().equals(a.createdBy))throw new org.springframework.security.access.AccessDeniedException("Not the submitter");
        if(!a.companyCase||a.isPublished())throw new ConflictException("Only unpublished cases can be resubmitted");
        if(request.version()==null||!request.version().equals(a.version))throw new ConflictException("Knowledge changed");
        apply(a,request,auth);a.publicationStatus="PENDING";a.reviewNote=null;a.reviewedAt=null;a.reviewedBy=null;
        return view(articles.saveAndFlush(a),auth);
    }
    private boolean admin(Authentication auth){return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));}
    public record ReviewRequest(@NotNull Long version,@NotBlank String action,@Size(max=1000) String note) {}
    @PostMapping("/{id}/review") @PreAuthorize("hasRole('ADMIN')") @Transactional
    public Map<String,Object> review(@PathVariable Long id,@Valid @RequestBody ReviewRequest request,Authentication auth){
        var a=articles.lockById(id).orElseThrow(()->new NotFoundException("Knowledge not found"));
        if(!request.version().equals(a.version))throw new ConflictException("Knowledge changed; reopen before reviewing");
        if(!a.companyCase && a.sourceCallId!=null)throw new IllegalArgumentException("Private source cannot be published");
        String target=switch(request.action()){case "APPROVE"->"PUBLISHED";case "REJECT"->"REJECTED";case "WITHDRAW"->"WITHDRAWN";default->throw new IllegalArgumentException("Invalid action");};
        if("WITHDRAW".equals(request.action())?!a.isPublished():!"PENDING".equals(a.publicationStatus))throw new ConflictException("Invalid publication state");
        if("REJECT".equals(request.action())&&(request.note()==null||request.note().isBlank()))throw new IllegalArgumentException("Rejection reason required");
        if("PUBLISHED".equals(target)&&a.derivedFromId!=null)a.reviewedSourceVersion=articles.findById(a.derivedFromId).map(source->source.version).orElse(null);
        a.publicationStatus=target;a.reviewNote=request.note();a.reviewedBy=auth.getName();a.reviewedAt=LocalDateTime.now();a.updatedBy=auth.getName();
        return view(articles.saveAndFlush(a),auth);
    }
    private KnowledgeArticle find(Long id) {return articles.findById(id).orElseThrow(()->new NotFoundException("未找到知识内容"));}
    private Map<String,Object> view(KnowledgeArticle article,Authentication auth) {
        // Whitelist public fields. A shared case never returns source identifiers to other staff.
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("publicationStatus",article.publicationStatus); result.put("reviewNote",article.reviewNote); result.put("reviewedBy",article.reviewedBy); result.put("reviewedAt",article.reviewedAt); result.put("id",article.id); result.put("title",article.title); result.put("category",article.category);
        result.put("content",article.content); result.put("version",article.version); result.put("companyCase",article.companyCase);
        result.put("createdBy",article.createdBy); result.put("createdAt",article.createdAt);
        result.put("updatedBy",article.updatedBy); result.put("updatedAt",article.updatedAt);
        boolean canAccessSource=article.sourceCallId!=null && access.canAccess(article.customerNo,auth);
        if(canAccessSource && article.companyCase && article.derivedFromId!=null)result.put("sourceUpdated",articles.findById(article.derivedFromId).map(source->!java.util.Objects.equals(source.version,article.reviewedSourceVersion)).orElse(true));
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
