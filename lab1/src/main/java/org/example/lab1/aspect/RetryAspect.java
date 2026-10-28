package org.example.lab1.aspect;

import org.example.lab1.annotation.RetryOnTransientFailure;
import org.springframework.dao.TransientDataAccessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** Повторює виклик методу після тимчасової помилки доступу до даних. */
@Aspect
@Component
public class RetryAspect {
    @Around("@annotation(retry)")
    public Object retry(ProceedingJoinPoint joinPoint, RetryOnTransientFailure retry) throws Throwable {
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                return joinPoint.proceed();
            } catch (TransientDataAccessException ex) {
                // Інші помилки не перехоплюються, а після ліміту виняток передається далі.
                if (attempts >= retry.maxAttempts()) throw ex;
            }
        }
    }
}
