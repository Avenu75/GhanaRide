package com.ghanaride.service;

import com.ghanaride.dto.ContactFormDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handles contact form submission.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final EmailService emailService;

    public void sendContactEmail(ContactFormDTO form) {
        log.info("Contact form: name={} email={} subject={}", form.getName(), form.getEmail(), form.getSubject());
        emailService.sendContactEmail(form);
    }
}
