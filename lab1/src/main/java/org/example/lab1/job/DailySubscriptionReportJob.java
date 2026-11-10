package org.example.lab1.job;

import org.example.lab1.service.ScheduledMaintenanceService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Генерує щоденний знімок статистики; одночасне виконання цього job заборонене. */
@DisallowConcurrentExecution
@Component
public class DailySubscriptionReportJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(DailySubscriptionReportJob.class);
    private final ScheduledMaintenanceService maintenanceService;

    public DailySubscriptionReportJob(ScheduledMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            int created = maintenanceService.createDailySubscriptionReport();
            log.info("Створення щоденного звіту завершено; нових записів: {}", created);
        } catch (RuntimeException ex) {
            log.error("Не вдалося створити щоденний звіт; повтор буде під час наступного запуску", ex);
            throw new JobExecutionException(ex, false);
        }
    }
}
