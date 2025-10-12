package com.lcaohoanq.productservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
public class HelloController {

    @Value("${app.welcome}")
    private String welcome;

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok(welcome);
    }
}

