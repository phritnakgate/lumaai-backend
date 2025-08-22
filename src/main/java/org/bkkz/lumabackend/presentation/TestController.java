package org.bkkz.lumabackend.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/test")
public class TestController {
    @GetMapping(value = "hello-world")
    public ResponseEntity<String> getHelloWorld() { return ResponseEntity.ok().body("Hello World"); }

    /**
     * Mock endpoint to simulate LLM response handling.
     * This endpoint is a placeholder and should be replaced with actual LLM service calls when integrated.
     *
     * @return A mock response simulating LLM handling.
     */
    @PostMapping(value = "handle-input")
    public ResponseEntity<?> llmMock() {
        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> decoratedList = new ArrayList<>();
        Map<String, Object> task1 = new HashMap<>();
        task1.put("task", "จัดตารางงงงงงงง");
        task1.put("date", "");
        task1.put("time", "");
        task1.put("intent", Arrays.asList("Check", "Add"));
        decoratedList.add(task1);

        Map<String, Object> task2 = new HashMap<>();
        task2.put("task", "ทานอาหาร");
        task2.put("date", "2025-07-30");
        task2.put("time", "18:30");
        task2.put("intent", Arrays.asList("Check", "Edit"));
        decoratedList.add(task2);
        mockResponse.put("text", "เพิ่มจัดตารางเรียนและเพิ่มทานอาหารเย็นให้หน่อย");
        mockResponse.put("decorated_input",Map.of(
                "text","เพิ่มจัดตารางเรียน, เพิ่มทานอาหารเย็น",
                "decorated", decoratedList
        ));
        mockResponse.put("message", "เพิ่มจัดตารางและเพิ่มทานอาหารเรียบร้อยแล้วครับ ต้องการให้ช่วยอะไรเพิ่มเติมอีกมั้ยครับ?");
        return ResponseEntity.ok().body(mockResponse);
    }
}