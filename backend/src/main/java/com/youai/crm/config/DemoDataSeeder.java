package com.youai.crm.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.youai.crm.customer.CustomerRequest;
import com.youai.crm.customer.Customer;
import com.youai.crm.customer.CustomerRepository;
import com.youai.crm.customer.CustomerService;
import com.youai.crm.communication.CallRecord;
import com.youai.crm.communication.CallRecordRepository;
import com.youai.crm.communication.Conversation;
import com.youai.crm.communication.ConversationRepository;
import com.youai.crm.communication.CrmMessage;
import com.youai.crm.communication.MessageRepository;
import com.youai.crm.communication.MessageTemplateService;
import com.youai.crm.order.SalesOrder;
import com.youai.crm.order.SalesOrderRepository;
import com.youai.crm.task.FollowUpTask;
import com.youai.crm.task.FollowUpTaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class DemoDataSeeder {

    @Bean
    @Order(2)
    CommandLineRunner seedDemoData(
            CustomerService customerService,
            CustomerRepository customerRepository,
            FollowUpTaskRepository taskRepository,
            SalesOrderRepository orderRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            CallRecordRepository callRecordRepository,
            MessageTemplateService messageTemplateService) {
        return args -> {
            messageTemplateService.seedDefaults();
            if (customerRepository.count() == 0) {
                seedCustomers(customerService);
            }
            if (taskRepository.count() == 0) {
                seedTasks(taskRepository);
            }
            if (orderRepository.count() == 0) {
                seedOrders(orderRepository);
            }
            if (conversationRepository.count() == 0) {
                seedCommunications(customerRepository, conversationRepository, messageRepository, callRecordRepository);
            }
        };
    }

    private void seedCustomers(CustomerService service) {
        service.create(customer("周雨桐", "13821678821", "天津澄途科技", "线上咨询", "林夕", "需求确认", "重点客户", 128000, "天津", List.of("高意向", "企业版")));
        service.create(customer("陈嘉宇", "18610283706", "北京云杉商贸", "老客转介绍", "陈晨", "方案报价", "重点客户", 86000, "北京", List.of("连锁零售")));
        service.create(customer("宋晓婉", "15900625938", "上海栖木设计", "市场活动", "周倩", "初步沟通", "普通客户", 32000, "上海", List.of("设计服务")));
        service.create(customer("王泽", "13920881649", "津南餐饮管理", "主动开发", "赵磊", "商务谈判", "重点客户", 176000, "天津", List.of("多门店", "高价值")));
        service.create(customer("刘思远", "13752190042", "星海教育咨询", "线上咨询", "林夕", "已成交", "重点客户", 98000, "天津", List.of("已签约")));
    }

    private CustomerRequest customer(
            String name, String phone, String company, String source, String owner,
            String stage, String level, long amount, String city, List<String> tags) {
        return new CustomerRequest(
                name, phone, company, source, owner, stage, level, BigDecimal.valueOf(amount), city,
                "由系统初始化的演示客户，可直接编辑并用于联调。",
                LocalDateTime.now().plusDays(1),
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null,
                tags);
    }

    private void seedTasks(FollowUpTaskRepository repository) {
        repository.save(task("回访周雨桐，确认门店数量", "周雨桐", "林夕", 4, "电话跟进", "today", "紧急", false));
        repository.save(task("为陈嘉宇更新 25 席位报价", "陈嘉宇", "陈晨", 7, "发送资料", "today", "高", false));
        repository.save(task("安排启程汽车技术接口评估", "马骁", "陈晨", 28, "会议", "upcoming", "普通", false));
        repository.save(task("跟进王泽合同法务意见", "王泽", "赵磊", 32, "合同", "upcoming", "高", false));
    }

    private FollowUpTask task(
            String title, String customer, String owner, int dueHours, String type,
            String status, String priority, boolean completed) {
        FollowUpTask task = new FollowUpTask();
        task.setTitle(title);
        task.setCustomerName(customer);
        task.setOwner(owner);
        task.setDueAt(LocalDateTime.now().plusHours(dueHours));
        task.setTaskType(type);
        task.setStatus(status);
        task.setPriority(priority);
        task.setCompleted(completed);
        return task;
    }

    private void seedOrders(SalesOrderRepository repository) {
        repository.save(order("SO20260817008", "刘思远", "专业版 · 20 席位", 98000, 98000, "已支付", "待开通", "林夕"));
        repository.save(order("SO20260816023", "王泽", "企业版 · 35 席位", 176000, 88000, "部分支付", "实施中", "赵磊"));
        repository.save(order("SO20260815017", "陈嘉宇", "标准版 · 25 席位", 86000, 0, "待支付", "未开始", "陈晨"));
    }

    private void seedCommunications(
            CustomerRepository customerRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            CallRecordRepository callRecordRepository) {
        List<Customer> customers = customerRepository.findAll().stream().limit(4).toList();
        for (Customer customer : customers) {
            Conversation conversation = new Conversation();
            conversation.setCustomerNo(customer.getCustomerNo());
            conversation.setCustomerName(customer.getName());
            conversation.setCompany(customer.getCompany());
            conversation.setOwner(customer.getOwner());
            conversation.setLastMessageAt(LocalDateTime.now().minusMinutes(20));
            conversation = conversationRepository.save(conversation);

            CrmMessage inbound = new CrmMessage();
            inbound.setConversationId(conversation.getId());
            inbound.setSender(customer.getName());
            inbound.setDirection("INBOUND");
            inbound.setContent("您好，想进一步了解客户管理方案。");
            inbound.setSentAt(LocalDateTime.now().minusMinutes(22));
            inbound.setRead(false);
            messageRepository.save(inbound);

            CrmMessage outbound = new CrmMessage();
            outbound.setConversationId(conversation.getId());
            outbound.setSender(customer.getOwner());
            outbound.setDirection("OUTBOUND");
            outbound.setContent("您好，我会整理方案并与您确认下一步安排。");
            outbound.setSentAt(LocalDateTime.now().minusMinutes(20));
            outbound.setRead(true);
            messageRepository.save(outbound);

            CallRecord call = new CallRecord();
            call.setCustomerNo(customer.getCustomerNo());
            call.setCustomerName(customer.getName());
            call.setPhone(customer.getPhone());
            call.setOwner(customer.getOwner());
            call.setAgent(customer.getOwner());
            call.setDirection("呼出");
            call.setStatus("已接通");
            call.setDurationSeconds(360);
            call.setNote("系统初始化通话记录");
            call.setStartedAt(LocalDateTime.now().minusHours(2));
            callRecordRepository.save(call);
        }
    }

    private SalesOrder order(
            String orderNo, String customer, String product, long amount, long paid,
            String paymentStatus, String serviceStatus, String owner) {
        SalesOrder order = new SalesOrder();
        order.setOrderNo(orderNo);
        order.setCustomerName(customer);
        order.setProduct(product);
        order.setAmount(BigDecimal.valueOf(amount));
        order.setPaidAmount(BigDecimal.valueOf(paid));
        order.setPaymentStatus(paymentStatus);
        order.setServiceStatus(serviceStatus);
        order.setOwner(owner);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
}
