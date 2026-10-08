package org.example.lab1.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.example.lab1.controller.SubscriptionUiController;
import org.example.lab1.controller.SubscriptionUiController;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(assignableTypes = SubscriptionUiController.class)
public class GlobalUiExceptionHandler {

    @ExceptionHandler({AccessDeniedException.class, EntityNotFoundException.class, IllegalArgumentException.class})
    public String handleUiExceptions(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/subscriptions");
    }
}