package org.example.lab1.aspect;

import org.example.lab1.annotation.RetryOnTransientFailure;
import org.example.lab1.service.SelfInvocationDemoService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.dao.TransientDataAccessResourceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AspectsTest {
    @Test
    @DisplayName("Повторює виклик після тимчасової помилки та повертає успішний результат")
    void retry_proceedsAgainAfterTransientFailure() throws Throwable {
        RetryAspect aspect = new RetryAspect();
        RetryOnTransientFailure annotation = mock(RetryOnTransientFailure.class);
        when(annotation.maxAttempts()).thenReturn(3);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenThrow(new TransientDataAccessResourceException("temporary"))
                .thenReturn("ok");

        assertEquals("ok", aspect.retry(joinPoint, annotation));
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    @DisplayName("Викидає тимчасову помилку після вичерпання максимальної кількості спроб")
    void retryRethrowsAfterMaximumAttempts() throws Throwable {
        RetryAspect aspect = new RetryAspect();
        RetryOnTransientFailure annotation = mock(RetryOnTransientFailure.class);
        when(annotation.maxAttempts()).thenReturn(2);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenThrow(new TransientDataAccessResourceException("temporary"));

        assertThrows(TransientDataAccessResourceException.class, () -> aspect.retry(joinPoint, annotation));
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    @DisplayName("Аспекти аудиту й вимірювання часу повертають результат початкового методу")
    void timerAndAuditReturnProceedResult() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("Example");
        when(signature.getName()).thenReturn("operation");
        when(signature.toShortString()).thenReturn("Example.operation()");
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn("result");

        assertEquals("result", new ExecutionTimeAspect().measure(joinPoint));
        assertEquals("result", new AuditAspect().audit(joinPoint));
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    @DisplayName("Зовнішній виклик проходить через аспект, а самовиклик обходить проксі")
    void selfInvocationBypassesProxyAdvice() {
        SelfInvocationDemoService target = new SelfInvocationDemoService();
        target.failBeforeSuccess(1);
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new RetryAspect());
        SelfInvocationDemoService proxy = factory.getProxy();

        assertEquals("done", proxy.operation());
        assertEquals(2, target.getAttempts());

        target.failBeforeSuccess(1);
        assertThrows(TransientDataAccessResourceException.class, proxy::callInternally);
        assertEquals(1, target.getAttempts());
    }
}
