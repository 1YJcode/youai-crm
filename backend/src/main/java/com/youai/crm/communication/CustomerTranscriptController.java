package com.youai.crm.communication;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.*;
@RestController @RequestMapping("/api/customers/{customerNo}/transcript")
public class CustomerTranscriptController {
    private final CustomerTranscriptService transcripts;
    public CustomerTranscriptController(CustomerTranscriptService transcripts){this.transcripts=transcripts;}
    @ModelAttribute public void noCache(jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    @GetMapping public CustomerTranscriptService.ArchiveView get(@PathVariable String customerNo,@RequestParam(defaultValue="0") int page,Authentication auth){return transcripts.get(customerNo,page,auth);}
    @GetMapping("/segments/{id}") public CustomerTranscriptService.SegmentView source(@PathVariable String customerNo,@PathVariable Long id,Authentication auth){return transcripts.source(customerNo,id,auth);}
    public record Selection(@NotEmpty @Size(max=20) List<@NotNull Long> segmentIds){}
    @PostMapping("/case") public Map<String,Long> sourceCase(@PathVariable String customerNo,@Valid @RequestBody Selection selection,Authentication auth){return Map.of("id",transcripts.selectionCase(customerNo,selection.segmentIds(),auth));}
}
