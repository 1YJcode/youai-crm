package com.youai.crm.knowledge;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.Map;
@RestController @RequestMapping("/api/knowledge")
public class RagKnowledgeController {
    private final RagKnowledgeService service;
    public RagKnowledgeController(RagKnowledgeService service){this.service=service;}
    public record Question(@NotBlank @Size(max=1200) String question,@Size(max=32) String customerNo,Long segmentId,boolean coaching){}
    @ModelAttribute public void noCache(jakarta.servlet.http.HttpServletResponse r){r.setHeader("Cache-Control","no-store");}
    @GetMapping("/rag-status") public Map<String,Object> status(){return Map.of("configured",service.configured(),"engine","RAG-Notebook");}
    @GetMapping("/rag-source") public RagKnowledgeService.Material source(@RequestParam String id,@RequestParam String customerNo,@RequestParam String version,Authentication auth){return service.source(id,customerNo,version,auth);}
    @PostMapping("/ask") public RagKnowledgeService.Answer ask(@Valid @RequestBody Question q,Authentication auth){
        try{return service.ask(q.question(),q.customerNo(),q.segmentId(),q.coaching(),auth);}
        catch(org.springframework.security.access.AccessDeniedException|com.youai.crm.common.ConflictException|IllegalArgumentException e){throw e;}
        catch(Exception e){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"知识检索或AI回答暂不可用，请稍后重试");}
    }
}
