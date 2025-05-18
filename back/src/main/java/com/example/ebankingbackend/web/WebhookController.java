package com.example.ebankingbackend.webhook;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class WebhookController {

    @PostMapping("/github_webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody(required = false) String payload) {
        System.out.println("📦 Webhook GitHub reçu : " + payload);
        return ResponseEntity.ok("✅ Webhook reçu avec succès");
    }
}
