package com.ghanaride.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler.
 * 
 * Catches all unhandled exceptions and:
 * 1. Logs the real error with full details (server side)
 * 2. Shows user-friendly error page/message (client side)
 * 3. NEVER exposes stack traces or internal details to users
 * 
 * CRITICAL: Returns correct HTTP status codes (404, 403, 500) — NOT 200
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================
    // 404 — Page Not Found
    // =========================================================

    @ExceptionHandler({
        NoHandlerFoundException.class,
        NoResourceFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(
            Exception ex,
            HttpServletRequest request,
            Model model
    ) {
        log.warn("404 — No handler for: {} {}", request.getMethod(), request.getRequestURI());

        model.addAttribute("pageTitle", "Page Not Found — GhanaRide");
        model.addAttribute("requestedUrl", request.getRequestURI());
        model.addAttribute("status", 404);
        model.addAttribute("error", "Page Not Found");
        model.addAttribute("message", "The page you're looking for doesn't exist or has been moved.");
        
        return "error/404";
    }

    // =========================================================
    // 403 — Access Denied
    // =========================================================

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request,
            Model model
    ) {
        log.warn("403 — Access denied: {} {} by {}",
            request.getMethod(),
            request.getRequestURI(),
            request.getUserPrincipal() != null 
                ? request.getUserPrincipal().getName() 
                : "anonymous"
        );

        model.addAttribute("pageTitle", "Access Denied — GhanaRide");
        model.addAttribute("status", 403);
        model.addAttribute("error", "Access Denied");
        model.addAttribute("message", "You don't have permission to access this page.");
        
        return "error/403";
    }

    // =========================================================
    // File Too Large
    // =========================================================

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleFileTooLarge(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        log.warn("File upload too large from: {}", request.getRemoteAddr());

        redirectAttributes.addFlashAttribute("error",
            "File is too large. Maximum allowed size is 5MB. " +
            "Please compress your image and try again.");

        // Redirect back to where they came from
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/profile");
    }

    // =========================================================
    // Booking Business Rule Violations
    // (Shows as flash message, not error page)
    // =========================================================

    @ExceptionHandler(BookingException.class)
    public String handleBookingException(
            BookingException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        log.warn("Booking exception: {}", ex.getMessage());

        redirectAttributes.addFlashAttribute("error", ex.getMessage());

        return "redirect:/my-bookings";
    }

    // =========================================================
    // Resource Not Found (404 equivalent for business logic)
    // =========================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request,
            Model model
    ) {
        log.warn("Resource not found: {}", ex.getMessage());

        model.addAttribute("pageTitle", "Not Found — GhanaRide");
        model.addAttribute("status", 404);
        model.addAttribute("error", "Not Found");
        model.addAttribute("message", ex.getMessage());
        
        return "error/404";
    }

    // =========================================================
    // Validation Failures (@NotBlank, @Min, etc.)
    // =========================================================

    @ExceptionHandler(org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    public String handleMethodValidation(
            org.springframework.web.method.annotation.HandlerMethodValidationException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        StringBuilder errors = new StringBuilder();
        ex.getValueResults().forEach(result -> {
            result.getResolvableErrors().forEach(err -> {
                errors.append(err.getDefaultMessage()).append("; ");
            });
        });

        log.warn("Validation failed on {} {}: {}", 
            request.getMethod(), request.getRequestURI(), errors);

        redirectAttributes.addFlashAttribute("error",
            "Please check your input: " + errors);

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/driver/add-trip");
    }

    // =========================================================
    // All Other Exceptions — Last Resort
    // Shows generic 500 page, NEVER exposes stack trace
    // =========================================================

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleAllExceptions(
            Exception ex,
            HttpServletRequest request,
            Model model
    ) {
        // Log the FULL exception with stack trace (server side only)
        log.error(
            "Unhandled exception on {} {}: {}",
            request.getMethod(),
            request.getRequestURI(),
            ex.getMessage(),
            ex // This logs the full stack trace
        );

        model.addAttribute("pageTitle", "Something Went Wrong — GhanaRide");
        model.addAttribute("status", 500);
        model.addAttribute("error", "Internal Server Error");
        model.addAttribute("message", 
            "We're experiencing a temporary issue. Our team has been notified.");
        
        return "error/500";
    }
}