package com.ramirez.mediturnosback.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class AppClock {
    public static final ZoneId APP_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    public LocalDate today() {
        return LocalDate.now(APP_ZONE);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(APP_ZONE);
    }
}
