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
        Map<String, Object> checkDate = new HashMap<>();
        checkDate.put("task", "");
        checkDate.put("date", "2025-08-21");
        checkDate.put("time", "");
        checkDate.put("intent", Arrays.asList("Check"));
        decoratedList.add(checkDate);

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

        Map<String, Object> search = new HashMap<>();
        search.put("intent", Arrays.asList("Search", "GoogleSearch"));
        search.put("response", "ขนมปังยอดฮิตในปี 2025 จากข้อมูลที่ได้รับ มีแนวโน้มว่าจะเป็น \\\"Shio Pan\\\" หรือ ขนมปังเกลือ เนื่องจากมีหลายร้านที่แนะนำและให้ความสนใจ โดยเฉพาะร้าน Eric Kayser และ Jayeon Do Salted Bakery");
        search.put("source","https://www.lemon8-app.com/@benzneb4/7489666656105677313?region=th");
        decoratedList.add(search);

        mockResponse.put("text", "เพิ่มจัดตารางเรียนและเพิ่มทานอาหารเย็นให้หน่อย หาเรื่องขนมปังยอดฮิตในปี 2025 ด้วย");
        mockResponse.put("decorated_input",Map.of(
                "text","เพิ่มจัดตารางเรียน, เพิ่มทานอาหารเย็นม ค้นหาเรื่องขนมปังยอดฮิตในปี 2025",
                "decorated", decoratedList
        ));
        mockResponse.put("message", "เพิ่มจัดตารางและเพิ่มทานอาหารเรียบร้อยแล้วครับ และขนมปังยอดฮิตในปี 2025 จากข้อมูลที่ได้รับ มีแนวโน้มว่าจะเป็น \\\"Shio Pan\\\" หรือ ขนมปังเกลือ เนื่องจากมีหลายร้านที่แนะนำและให้ความสนใจ โดยเฉพาะร้าน Eric Kayser และ Jayeon Do Salted Bakery ต้องการให้ช่วยอะไรเพิ่มเติมอีกมั้ยครับ?");
        return ResponseEntity.ok().body(mockResponse);
    }
}