package com.ramirez.mediturnosback;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/")
    public String home() {
        return "Mediturnos backend OK";
    }

    @GetMapping("/api/ping")
    public String ping() {
        return "pong";
    }
}