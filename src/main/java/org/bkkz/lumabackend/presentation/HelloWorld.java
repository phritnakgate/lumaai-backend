package org.bkkz.lumabackend.presentation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class HelloWorld {
    @GetMapping(value = "hello-world")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<String> getHelloWorld() {
        return ResponseEntity.ok().body("Hello World");
    }
}