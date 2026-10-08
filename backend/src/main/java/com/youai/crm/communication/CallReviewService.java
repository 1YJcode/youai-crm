package com.youai.crm.communication;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.common.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CallReviewService {

    private final CallRecordRepository callRepository;
    private final CallReviewRepository reviewRepository;
    private final AccessPolicy accessPolicy;
    private final com.youai.crm.customer.CustomerAccessPolicy customerAccess;

    public CallReviewService(CallRecordRepository callRepository, CallReviewRepository reviewRepository, AccessPolicy accessPolicy, com.youai.crm.customer.CustomerAccessPolicy customerAccess) {
        this.callRepository = callRepository;
        this.reviewRepository = reviewRepository;
        this.accessPolicy = accessPolicy;
        this.customerAccess = customerAccess;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Map<Long, Boolean> list(Authentication authentication) {
        accessPolicy.scopedOwner(authentication); // also rejects direct calls without an identity
        List<Long> callIds = callRepository.findAllByOrderByStartedAtDesc().stream()
                .filter(call -> customerAccess.canAccess(call.getCustomerNo(), authentication))
                .map(CallRecord::getId).toList();
        Map<Long, Boolean> result = new LinkedHashMap<>();
        if (!callIds.isEmpty()) reviewRepository.findAllByCallIdIn(callIds).forEach(review -> result.put(review.getCallId(), review.isReviewed()));
        return result;
    }

    public boolean update(Long callId, CallReviewRequest request, Authentication authentication) {
        CallRecord call = callRepository.findById(callId).orElseThrow(() -> new NotFoundException("未找到通话记录：" + callId));
        customerAccess.requireAccess(call.getCustomerNo(), authentication);
        CallReview review = reviewRepository.findById(callId).orElseGet(CallReview::new);
        review.setCallId(callId);
        review.setReviewerUsername(authentication == null ? "system" : authentication.getName());
        review.setReviewed(request.reviewed());
        review.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(review);
        return review.isReviewed();
    }
}
