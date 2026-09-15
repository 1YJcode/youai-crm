package com.youai.crm.communication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.common.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class MessageTemplateService {

    private static final String SYSTEM_OWNER = "*";
    private final MessageTemplateRepository repository;
    private final AccessPolicy accessPolicy;

    public MessageTemplateService(MessageTemplateRepository repository, AccessPolicy accessPolicy) {
        this.repository = repository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<MessageTemplateResponse> list(Authentication authentication) {
        String username = username(authentication);
        return repository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(template -> SYSTEM_OWNER.equals(template.getOwnerUsername())
                        || accessPolicy.isAdmin(authentication)
                        || template.getOwnerUsername().equalsIgnoreCase(username))
                .map(MessageTemplateResponse::from)
                .toList();
    }

    public MessageTemplateResponse create(MessageTemplateRequest request, Authentication authentication) {
        MessageTemplate template = new MessageTemplate();
        template.setId("TPL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT));
        template.setOwnerUsername(username(authentication));
        apply(template, request);
        return MessageTemplateResponse.from(repository.save(template));
    }

    public MessageTemplateResponse update(String id, MessageTemplateRequest request, Authentication authentication) {
        MessageTemplate template = getEditable(id, authentication);
        apply(template, request);
        return MessageTemplateResponse.from(repository.save(template));
    }

    public void delete(String id, Authentication authentication) {
        MessageTemplate template = getEditable(id, authentication);
        repository.delete(template);
    }

    public void seedDefaults() {
        if (repository.existsById("welcome") || repository.count() > 0) return;
        saveDefault("welcome", "首次跟进", "微信 / 短信", "您好，我是优爱的客户顾问，想和您确认一下当前的采购计划。方便时回复我即可。");
        saveDefault("quote", "报价跟进", "微信 / 短信", "您好，之前发送的方案和报价您看得怎么样？如果有需要调整的地方，我可以继续为您完善。");
        saveDefault("meeting", "会议确认", "微信 / 短信", "您好，提醒您我们约定的沟通时间即将开始。如时间需要调整，请提前告诉我。");
    }

    private void saveDefault(String id, String name, String channel, String content) {
        MessageTemplate template = new MessageTemplate();
        template.setId(id);
        template.setOwnerUsername(SYSTEM_OWNER);
        template.setName(name);
        template.setChannel(channel);
        template.setContent(content);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        repository.save(template);
    }

    private void apply(MessageTemplate template, MessageTemplateRequest request) {
        template.setName(request.name().trim());
        template.setChannel(request.channel().trim());
        template.setContent(request.content().trim());
    }

    private MessageTemplate getEditable(String id, Authentication authentication) {
        MessageTemplate template = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("未找到消息模板：" + id));
        if (!SYSTEM_OWNER.equals(template.getOwnerUsername())
                && !accessPolicy.isAdmin(authentication)
                && !template.getOwnerUsername().equalsIgnoreCase(username(authentication))) {
            throw new AccessDeniedException("只能维护本人创建的消息模板");
        }
        return template;
    }

    private String username(Authentication authentication) {
        return authentication == null || !StringUtils.hasText(authentication.getName()) ? "system" : authentication.getName();
    }
}
