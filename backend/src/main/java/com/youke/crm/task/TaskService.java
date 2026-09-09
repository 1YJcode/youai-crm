package com.youke.crm.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.youke.crm.common.NotFoundException;
import com.youke.crm.account.AccessPolicy;
import com.youke.crm.customer.Customer;
import com.youke.crm.customer.CustomerRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class TaskService {

    private final FollowUpTaskRepository repository;
    private final AccessPolicy accessPolicy;
    private final CustomerRepository customerRepository;

    public TaskService(FollowUpTaskRepository repository, AccessPolicy accessPolicy, CustomerRepository customerRepository) {
        this.repository = repository;
        this.accessPolicy = accessPolicy;
        this.customerRepository = customerRepository;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<TaskResponse> list(String owner, String status, Boolean completed, Authentication authentication) {
        String scopedOwner = accessPolicy.scopedOwner(authentication);
        return repository.findAllByOrderByDueAtAsc().stream()
                .filter(task -> !StringUtils.hasText(scopedOwner) || task.getOwner().equalsIgnoreCase(scopedOwner))
                .filter(task -> StringUtils.hasText(scopedOwner) || !StringUtils.hasText(owner) || task.getOwner().equalsIgnoreCase(owner.trim()))
                .filter(task -> !StringUtils.hasText(status) || task.getStatus().equalsIgnoreCase(status.trim()))
                .filter(task -> completed == null || task.isCompleted() == completed)
                .map(TaskResponse::from)
                .toList();
    }

    public TaskResponse find(Long id, Authentication authentication) {
        return TaskResponse.from(get(id, authentication));
    }

    public TaskResponse create(TaskRequest request, Authentication authentication) {
        FollowUpTask task = new FollowUpTask();
        apply(task, request);
        if (!accessPolicy.isAdmin(authentication)) task.setOwner(accessPolicy.currentOwner(authentication));
        updateFollowedCustomer(task, request, authentication);
        return TaskResponse.from(repository.save(task));
    }

    public TaskResponse update(Long id, TaskRequest request, Authentication authentication) {
        FollowUpTask task = get(id, authentication);
        apply(task, request);
        if (!accessPolicy.isAdmin(authentication)) task.setOwner(accessPolicy.currentOwner(authentication));
        updateFollowedCustomer(task, request, authentication);
        return TaskResponse.from(repository.save(task));
    }

    public TaskResponse updateCompletion(Long id, boolean completed, Authentication authentication) {
        FollowUpTask task = get(id, authentication);
        task.setCompleted(completed);
        task.setStatus(statusFor(task.getDueAt(), completed));
        if (!StringUtils.hasText(task.getPriority()) || "完成".equals(task.getPriority())) {
            task.setPriority(completed ? "完成" : "普通");
        }
        return TaskResponse.from(repository.save(task));
    }

    public void delete(Long id, Authentication authentication) {
        FollowUpTask task = get(id, authentication);
        repository.delete(task);
    }

    private void apply(FollowUpTask task, TaskRequest request) {
        task.setTitle(request.title().trim());
        task.setCustomerName(request.customer().trim());
        task.setCustomerId(request.customerId());
        task.setCustomerStatus(StringUtils.hasText(request.customerStatus()) ? request.customerStatus().trim() : null);
        if (request.customerId() != null) task.setFollowedAt(LocalDateTime.now());
        task.setOwner(request.owner().trim());
        task.setDueAt(request.dueAt());
        task.setTaskType(request.type().trim());
        boolean completed = Boolean.TRUE.equals(request.completed());
        task.setCompleted(completed);
        task.setStatus(statusFor(request.dueAt(), completed));
        task.setPriority(StringUtils.hasText(request.priority()) ? request.priority().trim() : (completed ? "完成" : "普通"));
    }

    private void updateFollowedCustomer(FollowUpTask task, TaskRequest request, Authentication authentication) {
        if (request.customerId() == null) return;
        Customer customer = customerRepository.findByCustomerNo(request.customerId())
                .orElseThrow(() -> new NotFoundException("未找到客户：" + request.customerId()));
        accessPolicy.requireOwner(customer.getOwner(), authentication);
        task.setCustomerName(customer.getName());
        customer.setLastContactAt(task.getFollowedAt());
        if (StringUtils.hasText(request.customerStatus())) customer.setStage(request.customerStatus().trim());
        customerRepository.save(customer);
    }

    private String statusFor(LocalDateTime dueAt, boolean completed) {
        if (completed) return "done";
        LocalDate today = LocalDate.now();
        if (dueAt.toLocalDate().isBefore(today)) return "overdue";
        if (dueAt.toLocalDate().isEqual(today)) return "today";
        return "upcoming";
    }

    private FollowUpTask get(Long id, Authentication authentication) {
        FollowUpTask task = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("未找到任务：" + id));
        accessPolicy.requireOwner(task.getOwner(), authentication);
        return task;
    }
}
