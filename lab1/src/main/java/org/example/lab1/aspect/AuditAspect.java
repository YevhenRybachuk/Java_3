package org.example.lab1.aspect;

import org.example.lab1.annotation.AuditOperation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Записує результат виклику позначеного методу до журналу аудиту. */
@Aspect
@Component
public class AuditAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    @Around("@annotation(org.example.lab1.annotation.AuditOperation)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        // Спершу виконуємо операцію, щоб фіксувати лише успішні зміни.
        Object result = joinPoint.proceed();
        log.info("AUDIT success: {} with arguments {}", joinPoint.getSignature().toShortString(),
                java.util.Arrays.deepToString(joinPoint.getArgs()));
        return result;
    }
}
