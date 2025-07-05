package org.bkkz.lumabackend.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorld {
    @GetMapping(value = "hello-world")
    public ResponseEntity<String> getHelloWorld() {
        return ResponseEntity.ok().body("Hello World");
    }
}