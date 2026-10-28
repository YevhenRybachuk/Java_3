package org.example.lab1.service;

import org.example.lab1.annotation.RetryOnTransientFailure;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.stereotype.Service;

/** Демонстрація того, що прямий виклик через this обходить Spring-проксі та аспект. */
@Service
public class SelfInvocationDemoService {
    private int attempts;
    private int failuresBeforeSuccess;

    public void failBeforeSuccess(int failures) {
        this.failuresBeforeSuccess = failures;
        this.attempts = 0;
    }

    @RetryOnTransientFailure(maxAttempts = 3)
    public String operation() {
        attempts++;
        if (attempts <= failuresBeforeSuccess) {
            throw new TransientDataAccessResourceException("temporary failure");
        }
        return "done";
    }

    public String callInternally() {
        // Самовиклик іде напряму до методу цього об'єкта, минаючи проксі з RetryAspect.
        return this.operation();
    }

    public int getAttempts() {
        return attempts;
    }
}
