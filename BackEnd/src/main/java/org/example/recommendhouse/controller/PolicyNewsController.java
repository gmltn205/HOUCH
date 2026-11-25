package org.example.recommendhouse.controller;

import org.example.recommendhouse.dto.PolicyNews;
import org.example.recommendhouse.service.PolicyNewsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PolicyNewsController {
    private final PolicyNewsService policyNewsService;

    public PolicyNewsController(PolicyNewsService policyNewsService) {
        this.policyNewsService = policyNewsService;
    }

    @GetMapping("/policy-news")
    public List<PolicyNews> getPolicyNews() {
        return policyNewsService.getPolicyNews(); // JSON으로 반환
    }
}
