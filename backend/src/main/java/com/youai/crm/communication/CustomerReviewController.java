package com.youai.crm.communication;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
@RestController @RequestMapping("/api/customers/{no}/review")
public class CustomerReviewController {
    private final CustomerReviewService service;
    public CustomerReviewController(CustomerReviewService service){this.service=service;}
    @ModelAttribute public void noCache(jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    @GetMapping public Map<String,Object> get(@PathVariable String no,Authentication auth){return service.get(no,auth);}
    @GetMapping("/{id}") public Map<String,Object> revision(@PathVariable String no,@PathVariable Long id,Authentication auth){return service.revision(no,id,auth);}
    @PostMapping public Map<String,Object> request(@PathVariable String no,Authentication auth){service.request(no,auth);return service.get(no,auth);}
    public record Publish(@NotNull Long version,@NotBlank @Size(max=160) String title,@NotBlank @Size(max=20000) String content,@NotNull @AssertTrue Boolean sharingConfirmed){}
    @PostMapping("/{id}/publish") public Map<String,Object> publish(@PathVariable String no,@PathVariable Long id,@Valid @RequestBody Publish body,Authentication auth){return Map.of("articleId",service.publish(no,id,body.version(),body.title(),body.content(),auth));}
}
