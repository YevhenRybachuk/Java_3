package org.example.lab1.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.lab1.config.QuartzConfiguration;
import org.example.lab1.service.ScheduledMaintenanceService;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SimpleTrigger;

class ScheduledJobsTest {
    private final ScheduledMaintenanceService service = mock(ScheduledMaintenanceService.class);
    private final QuartzConfiguration configuration = new QuartzConfiguration();

    @Test
    void jobsDisallowConcurrentExecution() {
        assertTrue(BillingReminderJob.class.isAnnotationPresent(DisallowConcurrentExecution.class));
        assertTrue(DailySubscriptionReportJob.class.isAnnotationPresent(DisallowConcurrentExecution.class));
    }

    @Test
    void billingJobUsesMergedTriggerValueAndFallsBackToJobDefault() throws Exception {
        BillingReminderJob job = new BillingReminderJob(service);
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap defaults = new JobDataMap();
        defaults.put("daysAhead", 3);
        JobDataMap triggerOverrides = new JobDataMap();
        triggerOverrides.put("daysAhead", 1);
        when(context.getMergedJobDataMap()).thenReturn(merged(defaults, triggerOverrides));

        job.execute(context);

        verify(service).scheduleBillingReminders(1);

        when(context.getMergedJobDataMap()).thenReturn(merged(defaults, new JobDataMap()));
        job.execute(context);
        verify(service).scheduleBillingReminders(3);
    }

    @Test
    void billingJobRejectsMissingParameterAndDoesNotCallService() {
        BillingReminderJob job = new BillingReminderJob(service);
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        JobExecutionException exception = assertThrows(JobExecutionException.class, () -> job.execute(context));

        assertTrue(exception.getMessage().contains("daysAhead"));
        verify(service, never()).scheduleBillingReminders(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void triggersReferenceNamedJobsAndDeclareMisfirePoliciesAndUtcZone() {
        var reminderJob = configuration.billingReminderJobDetail();
        SimpleTrigger frequent = configuration.billingReminderFrequentTrigger(reminderJob);
        CronTrigger dailyReminder = configuration.billingReminderDailyTrigger(reminderJob);
        var reportJob = configuration.dailySubscriptionReportJobDetail();
        CronTrigger report = configuration.dailySubscriptionReportTrigger(reportJob);

        assertEquals(new JobKey("billing-reminder", "subscription-jobs"), frequent.getJobKey());
        assertEquals(new JobKey("billing-reminder", "subscription-jobs"), dailyReminder.getJobKey());
        assertEquals(new JobKey("daily-subscription-report", "subscription-jobs"), report.getJobKey());
        assertEquals("subscription-triggers", frequent.getKey().getGroup());
        assertEquals(1, frequent.getJobDataMap().getInt("daysAhead"));
        assertEquals(3, reminderJob.getJobDataMap().getInt("daysAhead"));
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT,
                frequent.getMisfireInstruction());
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, dailyReminder.getMisfireInstruction());
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, report.getMisfireInstruction());
        assertEquals("UTC", dailyReminder.getTimeZone().getID());
        assertEquals("UTC", report.getTimeZone().getID());
    }

    @Test
    void dailyReportJobDoesNotRefireAfterFailure() {
        DailySubscriptionReportJob job = new DailySubscriptionReportJob(service);
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(service.createDailySubscriptionReport()).thenThrow(new IllegalStateException("database unavailable"));

        JobExecutionException exception = assertThrows(JobExecutionException.class, () -> job.execute(context));

        assertFalse(exception.refireImmediately());
        verify(service).createDailySubscriptionReport();
    }

    @Test
    void billingJobDoesNotRefireAfterFailure() {
        BillingReminderJob job = new BillingReminderJob(service);
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("daysAhead", 2);
        when(context.getMergedJobDataMap()).thenReturn(dataMap);
        when(service.scheduleBillingReminders(2)).thenThrow(new IllegalStateException("database unavailable"));

        JobExecutionException exception = assertThrows(JobExecutionException.class, () -> job.execute(context));

        assertFalse(exception.refireImmediately());
        verify(service).scheduleBillingReminders(2);
    }

    private JobDataMap merged(JobDataMap defaults, JobDataMap overrides) {
        JobDataMap merged = new JobDataMap(defaults);
        merged.putAll(overrides);
        return merged;
    }
}
