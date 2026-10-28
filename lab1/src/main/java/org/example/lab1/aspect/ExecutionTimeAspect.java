package org.example.lab1.aspect;

import org.example.lab1.annotation.MeasureExecutionTime;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Вимірює тривалість виконання методів, позначених відповідною анотацією. */
@Aspect
@Component
public class ExecutionTimeAspect {
    private static final Logger log = LoggerFactory.getLogger(ExecutionTimeAspect.class);

    @Around("@annotation(org.example.lab1.annotation.MeasureExecutionTime)")
    public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
        // finally гарантує запис часу і тоді, коли метод завершується винятком.
        long startedAt = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedMicros = (System.nanoTime() - startedAt) / 1_000;
            log.info("{}.{} completed in {} μs", joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(), elapsedMicros);
        }
    }
}
