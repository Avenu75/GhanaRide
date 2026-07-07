package com.ghanaride.service;

import com.ghanaride.dto.ContactFormDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handles contact form submission.
 * Delegates email sending to EmailService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final EmailService emailService;

    /**
     * Processes a contact form submission:
     * 1. Sends message to GhanaRide support inbox
     * 2. Sends auto-reply to the user
     *
     * Both emails are async — this method returns
     * immediately without waiting for emails to send.
     */
    public void sendContactEmail(ContactFormDTO form) {
        log.info(
                "Contact form: name={} email={} subject={}",
                form.getName(),
                form.getEmail(),
                form.getSubject()
        );

        // Send to support team (async)
        emailService.sendContactFormEmail(form);
    }
}