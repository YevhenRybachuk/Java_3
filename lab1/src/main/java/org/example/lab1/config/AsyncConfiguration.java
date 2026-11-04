package org.example.lab1.config;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Налаштування обмеженого пулу потоків для асинхронних доменних операцій. */
@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {
    private static final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

    @Bean(name = "subscriptionTaskExecutor")
    public ThreadPoolTaskExecutor subscriptionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("subscription-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return subscriptionTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (exception, method, parameters) -> log.error(
                "Помилка асинхронного void-методу {} з параметрами {}",
                method.getName(), parameters, exception);
    }
}
