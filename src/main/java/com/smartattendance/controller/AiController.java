package com.smartattendance.controller;

import com.smartattendance.config.ClientIpFilter;
import com.smartattendance.dto.AttendanceDtos;
import com.smartattendance.service.AiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/proxy-check")
    public AttendanceDtos.ProxyCheckResponse proxyCheck(HttpServletRequest request) {
        String ip = (String) request.getAttribute(ClientIpFilter.CLIENT_IP_ATTRIBUTE);
        return aiService.inspectCurrentRequest(ip);
    }
}
