package com.smartattendance.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "name", "QR Attendance API",
                "status", "running",
                "loginEndpoint", "/api/auth/login",
                "registerEndpoint", "/api/auth/register",
                "proxyCheckEndpoint", "/api/ai/proxy-check"
        );
    }
}
