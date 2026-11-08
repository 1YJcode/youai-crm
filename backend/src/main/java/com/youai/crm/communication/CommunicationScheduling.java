package com.youai.crm.communication;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class CommunicationScheduling {
    @Bean
    public ThreadPoolTaskScheduler communicationTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("communication-");
        return scheduler;
    }

    // Preserve the existing single-thread scheduler for the other CRM jobs.
    @Bean(name = "taskScheduler")
    @ConditionalOnMissingBean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("crm-scheduled-");
        return scheduler;
    }
}
