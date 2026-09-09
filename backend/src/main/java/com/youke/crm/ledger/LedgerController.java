package com.youke.crm.ledger;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/ledger-accounts")
public class LedgerController {
 private final LedgerService service; public LedgerController(LedgerService service){this.service=service;}
 @GetMapping public List<LedgerResponse> list(Authentication a){return service.list(a);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public LedgerResponse create(@Valid @RequestBody LedgerRequest r,Authentication a){return service.create(r,a);}
 @PatchMapping("/{id}/execute") public LedgerResponse execute(@PathVariable String id,Authentication a){return service.execute(id,a);}
}
