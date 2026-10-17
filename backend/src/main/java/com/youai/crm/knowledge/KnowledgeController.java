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

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeRepository articles;
    public KnowledgeController(KnowledgeRepository articles) { this.articles=articles; }
    public record ArticleRequest(@NotBlank @Size(max=160) String title,
            @NotBlank @Size(max=64) String category, @NotBlank @Size(max=100000) String content, Long version) {}
    public record Summary(Long id,String title,String category,String preview,String updatedBy,LocalDateTime updatedAt) {}
    @GetMapping
    public Page<Summary> list(@RequestParam(defaultValue="") String keyword,
            @RequestParam(defaultValue="") String category, @RequestParam(defaultValue="0") int page) {
        String term=keyword.trim().toLowerCase(java.util.Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
        return articles.search("%"+term+"%",category.trim(),PageRequest.of(Math.max(0,page),12,Sort.by(Sort.Direction.DESC,"updatedAt").and(Sort.by(Sort.Direction.DESC,"id"))))
                .map(a->new Summary(a.id,a.title,a.category,a.content.substring(0,Math.min(a.content.length(),140)),a.updatedBy,a.updatedAt));
    }
    @GetMapping("/categories") public List<String> categories() {return articles.categories();}
    @GetMapping("/{id}") public KnowledgeArticle get(@PathVariable Long id) {return find(id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public KnowledgeArticle create(@Valid @RequestBody ArticleRequest request,Authentication auth) {
        var article=new KnowledgeArticle(); article.createdBy=auth.getName();
        apply(article,request,auth); return articles.saveAndFlush(article);
    }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional
    public KnowledgeArticle update(@PathVariable Long id,@Valid @RequestBody ArticleRequest request,Authentication auth) {
        var article=find(id);
        if(request.version()==null || !request.version().equals(article.version)) throw new ConflictException("知识内容已更新，请重新打开后编辑");
        apply(article,request,auth); return articles.saveAndFlush(article);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") @Transactional
    public void delete(@PathVariable Long id,@RequestParam Long version) {
        var article=find(id);
        if(!version.equals(article.version)) throw new ConflictException("知识内容已更新，请重新打开后删除");
        articles.delete(article); articles.flush();
    }
    private KnowledgeArticle find(Long id) {return articles.findById(id).orElseThrow(()->new NotFoundException("未找到知识内容"));}
    private void apply(KnowledgeArticle article,ArticleRequest request,Authentication auth) {
        article.title=request.title().trim(); article.category=request.category().trim();
        article.content=request.content().trim(); article.updatedBy=auth.getName();
    }
}
