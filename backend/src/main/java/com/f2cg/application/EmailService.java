package com.f2cg.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendVerificationCode(String email, String code) {
        log.info("Verification code for {}: {}", email, code);
    }
}