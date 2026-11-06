package com.youai.crm.order;
import java.time.LocalDateTime;
import java.util.*;
import com.youai.crm.account.AccessPolicy;
import com.youai.crm.common.NotFoundException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/contracts")
@Transactional
public class ContractController {
 private final SalesContractRepository repository;
 private final AccessPolicy policy;
 private final OrderService orders;
 private final com.youai.crm.account.CrmUserRepository users;
 public ContractController(SalesContractRepository repository, AccessPolicy policy, OrderService orders, com.youai.crm.account.CrmUserRepository users) {
  this.repository=repository; this.policy=policy; this.orders=orders; this.users=users;
 }
 public record ContractResponse(String contractNo,String orderId,String customerId,String customer,String owner,
   String status,LocalDateTime createdAt,LocalDateTime reviewedAt,Long reviewedBy,String reviewNote) {
  static ContractResponse from(SalesContract c) { return new ContractResponse(c.contractNo,c.order.getOrderNo(),
   c.order.getCustomerNo(),c.order.getCustomerName(),c.order.getOwner(),c.status,c.createdAt,c.reviewedAt,c.reviewedBy,c.reviewNote); }
 }
 private Specification<SalesContract> scope(Authentication auth) {
  policy.scopedOwner(auth);
  return (r,q,b)->policy.isAdmin(auth)?b.conjunction():b.or(
   b.equal(r.get("order").get("ownerId"),policy.currentUserId(auth)),
   b.equal(r.get("order").get("confirmationSalesId"),policy.currentUserId(auth)));
 }
 @GetMapping
 public Page<ContractResponse> list(@RequestParam(required=false) String customerId,
   @RequestParam(required=false) String owner,@RequestParam(required=false) String status,
   @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,Authentication auth) {
  Specification<SalesContract> spec=scope(auth).and((r,q,b)->{
   var p=new ArrayList<jakarta.persistence.criteria.Predicate>();
   if(customerId!=null&&!customerId.isBlank())p.add(b.equal(r.get("order").get("customerNo"),customerId.trim()));
   if(owner!=null&&!owner.isBlank())p.add(b.equal(r.get("order").get("owner"),owner.trim()));
   if(status!=null&&!status.isBlank())p.add(b.equal(r.get("status"),status.trim()));
   return b.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
  });
  return repository.findAll(spec,PageRequest.of(Math.max(0,page),Math.max(1,Math.min(100,size)),Sort.by(Sort.Direction.DESC,"createdAt","id"))).map(ContractResponse::from);
 }
 @GetMapping("/owners")
 public List<String> owners(Authentication auth) {
  // Include employees before their first contract, retaining historical owners.
  policy.scopedOwner(auth);
  var names = new TreeSet<String>(repository.visibleOwners(policy.isAdmin(auth),policy.isAdmin(auth)?null:policy.currentUserId(auth)));
  if (policy.isAdmin(auth)) {
   users.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
    .filter(user -> user.getRoles().stream().noneMatch(role -> "ADMIN".equals(role.getCode())))
    .map(com.youai.crm.account.CrmUser::getDisplayName).forEach(names::add);
  } else {
   names.add(policy.currentDisplayName(auth));
  }
  return new ArrayList<>(names);
 }
 @GetMapping("/{no}")
 public ContractResponse detail(@PathVariable String no,Authentication auth) {
  SalesContract c=get(no); orders.find(c.order.getOrderNo(),auth); return ContractResponse.from(c);
 }
 public record ReviewRequest(String status,String note) {}
 @PatchMapping("/{no}/review")
 public ContractResponse review(@PathVariable String no,@RequestBody ReviewRequest request,Authentication auth) {
  if(!policy.isAdmin(auth))throw new AccessDeniedException("只有管理员可以审核合同");
  if(!Set.of("审核通过","审核驳回").contains(Objects.toString(request.status(),"")))throw new IllegalArgumentException("审核状态无效");
  if(request.note()!=null&&request.note().length()>500)throw new IllegalArgumentException("审核备注不能超过500字");
  SalesContract c=get(no); c.status=request.status();c.reviewNote=request.note();c.reviewedAt=LocalDateTime.now();c.reviewedBy=policy.currentUserId(auth);
  return ContractResponse.from(repository.save(c));
 }
 private SalesContract get(String no) { return repository.findByContractNo(no).orElseThrow(()->new NotFoundException("未找到合同："+no)); }
}
