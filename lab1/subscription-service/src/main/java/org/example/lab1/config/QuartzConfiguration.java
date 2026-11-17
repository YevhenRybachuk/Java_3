package org.example.lab1.config;

import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;
import org.example.lab1.job.BillingReminderJob;
import org.example.lab1.job.DailySubscriptionReportJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/** Оголошує завдання, тригери та Spring-ін’єкцію залежностей у Quartz jobs. */
@Configuration
public class QuartzConfiguration {
    private static final String JOB_GROUP = "subscription-jobs";
    private static final String TRIGGER_GROUP = "subscription-triggers";
    private static final ZoneId SCHEDULE_ZONE = ZoneId.of("UTC");

    @Bean
    public JobDetail billingReminderJobDetail() {
        return JobBuilder.newJob(BillingReminderJob.class)
                .withIdentity("billing-reminder", JOB_GROUP)
                .withDescription("Ідемпотентна реєстрація нагадувань про списання підписок")
                .usingJobData("daysAhead", 3)
                .storeDurably()
                .build();
    }

    @Bean
    public SimpleTrigger billingReminderFrequentTrigger(
            @Qualifier("billingReminderJobDetail") JobDetail billingReminderJobDetail) {
        return TriggerBuilder.newTrigger()
                .withIdentity("billing-reminder-frequent", TRIGGER_GROUP)
                .forJob(billingReminderJobDetail.getKey())
                .usingJobData("daysAhead", 1)
                .startAt(new Date(System.currentTimeMillis() + 60_000))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(60)
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    @Bean
    public CronTrigger billingReminderDailyTrigger(
            @Qualifier("billingReminderJobDetail") JobDetail billingReminderJobDetail) {
        return TriggerBuilder.newTrigger()
                .withIdentity("billing-reminder-daily", TRIGGER_GROUP)
                .forJob(billingReminderJobDetail.getKey())
                .usingJobData("daysAhead", 3)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 9 * * ?")
                        .inTimeZone(TimeZone.getTimeZone(SCHEDULE_ZONE))
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public JobDetail dailySubscriptionReportJobDetail() {
        return JobBuilder.newJob(DailySubscriptionReportJob.class)
                .withIdentity("daily-subscription-report", JOB_GROUP)
                .withDescription("Щоденний ідемпотентний знімок статистики підписок")
                .storeDurably()
                .build();
    }

    @Bean
    public CronTrigger dailySubscriptionReportTrigger(
            @Qualifier("dailySubscriptionReportJobDetail") JobDetail dailySubscriptionReportJobDetail) {
        return TriggerBuilder.newTrigger()
                .withIdentity("daily-subscription-report-trigger", TRIGGER_GROUP)
                .forJob(dailySubscriptionReportJobDetail.getKey())
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?")
                        .inTimeZone(TimeZone.getTimeZone(SCHEDULE_ZONE))
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public SchedulerFactoryBeanCustomizer quartzJobFactory(AutowireCapableBeanFactory beanFactory) {
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory() {
            @Override
            protected Object createJobInstance(org.quartz.spi.TriggerFiredBundle bundle) throws Exception {
                return beanFactory.createBean(bundle.getJobDetail().getJobClass());
            }
        };
        return schedulerFactory -> schedulerFactory.setJobFactory(jobFactory);
    }
}
