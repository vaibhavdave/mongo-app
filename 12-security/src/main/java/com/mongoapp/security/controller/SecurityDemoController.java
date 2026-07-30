package com.mongoapp.security.controller;

import com.mongoapp.security.service.SecurityDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/security-demo")
public class SecurityDemoController {

    private final SecurityDemoService securityDemoService;

    public SecurityDemoController(SecurityDemoService securityDemoService) {
        this.securityDemoService = securityDemoService;
    }

    @GetMapping("/whoami")
    public Map<String, Object> whoAmI() {
        return securityDemoService.whoAmI();
    }

    @GetMapping("/attempt-cross-database-access")
    public Map<String, String> attemptCrossDatabaseAccess() {
        return Map.of("result", securityDemoService.attemptCrossDatabaseAccess());
    }
}
