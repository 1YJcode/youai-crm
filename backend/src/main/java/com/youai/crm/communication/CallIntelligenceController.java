package com.youai.crm.communication;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import java.util.List;
@RestController @RequestMapping("/api")
public class CallIntelligenceController {
    private final CallIntelligenceService service;private final FollowUpTemplates templates;
    public CallIntelligenceController(CallIntelligenceService service,FollowUpTemplates templates){this.service=service;this.templates=templates;}
    @ModelAttribute public void noCache(jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    @GetMapping("/call-intelligence/templates") public List<FollowUpTemplates.Template> templates(){return templates.all();}
    @GetMapping("/transcript-segments/{id}/intelligence") public CallIntelligenceService.View get(@PathVariable Long id,Authentication auth){return service.get(id,auth);}
    @GetMapping("/customers/{customerNo}/call-intelligence/latest") public CallIntelligenceService.View latest(@PathVariable String customerNo,Authentication auth){return service.latest(customerNo,auth);}
    public record Request(@NotNull @AssertTrue Boolean processingConsent,String preferredTemplate){}
    @PostMapping("/transcript-segments/{id}/intelligence") @ResponseStatus(HttpStatus.ACCEPTED)
    public CallIntelligenceService.View request(@PathVariable Long id,@Valid @RequestBody Request request,Authentication auth){return service.request(id,request.preferredTemplate(),auth);}
    public record Confirmation(@NotNull Long version,@NotBlank String template,@NotBlank @Size(max=2000) String content){}
    @PostMapping("/transcript-segments/{id}/intelligence/confirm")
    public CallIntelligenceService.View confirm(@PathVariable Long id,@Valid @RequestBody Confirmation request,Authentication auth){return service.confirm(id,request.version(),request.template(),request.content(),auth);}
}
