package org.example.lab1.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lab1.dto.SubscriptionRequestDto;
import org.example.lab1.service.CategoryService;
import org.example.lab1.service.SubscriptionService;
import org.example.lab1.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionUiController {
    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("subscriptions", subscriptionService.findAll());
        return "subscriptions/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("subscription", new SubscriptionRequestDto());
        model.addAttribute("formTitle", "Нова підписка");
        model.addAttribute("formAction", "/subscriptions");
        addFormOptions(model);
        return "subscriptions/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("subscription") SubscriptionRequestDto subscription,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal OidcUser principal) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formTitle", "Нова підписка");
            model.addAttribute("formAction", "/subscriptions");
            addFormOptions(model);
            return "subscriptions/form";
        }
        subscriptionService.create(subscription, principal);
        redirectAttributes.addFlashAttribute("successMessage", "Підписку створено");
        return "redirect:/subscriptions";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, @AuthenticationPrincipal OidcUser principal) {
        var existing = subscriptionService.findById(id);

        SubscriptionRequestDto form = new SubscriptionRequestDto();
        form.setName(existing.getName());
        form.setPrice(existing.getPrice());
        form.setBillingDate(existing.getBillingDate());
        form.setUserId(existing.getUserId());
        form.setCategoryId(existing.getCategoryId());

        model.addAttribute("subscription", form);
        model.addAttribute("formTitle", "Редагування підписки #" + id);
        model.addAttribute("formAction", "/subscriptions/" + id);

        addFormOptions(model, principal);
        return "subscriptions/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("subscription") SubscriptionRequestDto subscription,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal OidcUser principal) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formTitle", "Редагування підписки #" + id);
            model.addAttribute("formAction", "/subscriptions/" + id);
            addFormOptions(model);
            return "subscriptions/form";
        }
        subscriptionService.update(id, subscription, principal);
        redirectAttributes.addFlashAttribute("successMessage", "Підписку оновлено");
        return "redirect:/subscriptions";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal OidcUser principal) {
        subscriptionService.delete(id, principal);
        redirectAttributes.addFlashAttribute("successMessage", "Підписку видалено");
        return "redirect:/subscriptions";
    }

    private void addFormOptions(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("categories", categoryService.findAll());
    }
    private void addFormOptions(Model model, OidcUser principal) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("categories", categoryService.findAll());

        if (isAdmin) {
            model.addAttribute("users", userService.findAll());
        }
    }
}

