package org.example.lab1.job;

import org.example.lab1.service.ScheduledMaintenanceService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Тонкий Quartz job: читає горизонт нагадування та передає його сервісу. */
@Component
@DisallowConcurrentExecution
public class BillingReminderJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(BillingReminderJob.class);
    private final ScheduledMaintenanceService maintenanceService;

    public BillingReminderJob(ScheduledMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (!context.getMergedJobDataMap().containsKey("daysAhead")) {
            throw new JobExecutionException("Обов’язковий параметр JobDataMap 'daysAhead' відсутній", false);
        }
        int daysAhead;
        try {
            daysAhead = context.getMergedJobDataMap().getInt("daysAhead");
        } catch (RuntimeException ex) {
            throw new JobExecutionException("Параметр JobDataMap 'daysAhead' має бути цілим числом", ex, false);
        }
        try {
            int created = maintenanceService.scheduleBillingReminders(daysAhead);
            log.info("Створено {} нагадувань про списання на горизонті {} днів", created, daysAhead);
        } catch (RuntimeException ex) {
            log.error("Не вдалося зареєструвати нагадування; повтор буде під час наступного запуску", ex);
            throw new JobExecutionException(ex, false);
        }
    }
}
