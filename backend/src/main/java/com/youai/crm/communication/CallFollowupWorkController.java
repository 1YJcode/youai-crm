package com.youai.crm.communication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import java.util.*;
@RestController @RequestMapping("/api/call-followup-work")
public class CallFollowupWorkController {
    private final CallFollowupWorkService service;
    public CallFollowupWorkController(CallFollowupWorkService service){this.service=service;}
    @ModelAttribute public void noCache(jakarta.servlet.http.HttpServletResponse r){r.setHeader("Cache-Control","no-store");}
    @GetMapping public List<CallFollowupWorkService.View> list(@RequestParam(required=false) String customerNo,Authentication auth){return service.list(customerNo,auth);}
    @GetMapping("/sources") public List<CallFollowupWorkService.View> sources(@RequestParam String customerNo,Authentication auth){return service.sources(customerNo,auth);}
    @GetMapping("/by-followup/{id}") public CallFollowupWorkService.Source source(@PathVariable Long id,Authentication auth){return service.sourceForTask(id,auth);}
    @PostMapping("/from-transcript/{segmentId}") public CallFollowupWorkService.View useSource(@PathVariable Long segmentId,Authentication auth){return service.useSource(segmentId,auth);}
    @PostMapping("/daily-summary") public CallFollowupWorkService.Summary summary(Authentication auth){return service.dailySummary(auth);}
    @PostMapping("/{callId}/submit") public com.youai.crm.task.TaskResponse submit(@PathVariable Long callId,@Valid @RequestBody CallFollowupWorkService.Submission request,Authentication auth){return service.submit(callId,request,auth);}
}
