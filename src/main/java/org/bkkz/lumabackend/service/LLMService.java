package org.bkkz.lumabackend.service;

import org.bkkz.lumabackend.model.llm.llmResponse.DecoratedItem;
import org.bkkz.lumabackend.model.task.CreateTaskRequest;
import org.bkkz.lumabackend.model.task.UpdateTaskRequest;
import org.bkkz.lumabackend.utils.LLMIntent;
import org.bkkz.lumabackend.utils.StringUtil;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LLMService {

    private final DecoratedItem decoratedItem;
    private final TaskService taskService;
    private final FormService formService;


    private List<Map<String, Object>> userTasks;
    private Map<String, List<Map<String, Object>>> serviceResponse;

    public LLMService(DecoratedItem decoratedItem, TaskService taskService, FormService formService) {
        this.decoratedItem = decoratedItem;
        this.taskService = taskService;
        this.formService = formService;
        this.serviceResponse = new HashMap<>();
        this.serviceResponse.put("results", new ArrayList<>());
        this.serviceResponse.put("errors", new ArrayList<>());
    }

    public Map<String, List<Map<String, Object>>> processIntent() {
        for (String intent : decoratedItem.intent()) {
            doIntent(intent);
        }
        return serviceResponse;
    }

    private void doIntent(String intent) {
        LLMIntent llmIntent = LLMIntent.fromString(intent);

        switch (llmIntent) {
            case ADD:
                handleAdd();
                break;
            case CHECK:
                handleCheck();
                break;
            case EDIT:
                handleEdit();
                break;
            case REMOVE:
                handleRemove();
                break;
            case SEARCH:
                serviceResponse.get("results").add(Map.of(
                        "intent", "SEARCH",
                        "message", decoratedItem.response()
                ));
                break;
            case GOOGLESEARCH:
                serviceResponse.get("results").add(Map.of(
                        "intent", "GOOGLESEARCH",
                        "message", decoratedItem.source()
                ));
                break;
            case PLAN:
                serviceResponse.get("results").add(Map.of(
                        "intent", "PLAN",
                        "message", decoratedItem.response()
                ));
                break;
            case GENFORM:
                handleGenForm();
                break;
            default:
                serviceResponse.get("errors").add(Map.of(
                        "intent", "UNKNOWN",
                        "message", "Unknown intent"
                ));
                break;
        }

    }

    private void handleCheck() {
        try{
            List<Map<String, Object>> allUserTasks = taskService.getTasksByDate(null).get();
            userTasks = allUserTasks.stream()
                    .filter(task -> task.get("name").toString().contains(decoratedItem.task()))
                    .collect(Collectors.toCollection(ArrayList::new));
            System.out.println("Filter by name: "+ userTasks);
            //ถ้ามีแค่ Check และไม่มี Add, Remove, Edit จะกรองวันที่ด้วย
            if(decoratedItem.intent().contains("Check") &&
                    !decoratedItem.intent().contains("Add") &&
                    !decoratedItem.intent().contains("Remove") &&
                    !decoratedItem.intent().contains("Edit")) {
                filterTaskByDate();
            }

            Map<String, Object> checkResult = Map.of("intent", "CHECK", "output", userTasks);
            serviceResponse.get("results").add(checkResult);

        } catch (Exception e) {
            serviceResponse.get("errors").add(Map.of("intent", "CHECK", "message", e.getMessage()));
        }

    }

    private void filterTaskByDate(){
        LocalDate dateNow = ZonedDateTime.now(ZoneId.of("GMT+7")).toLocalDate();
        String query = dateNow.getYear() + "-" +
                String.format("%02d", dateNow.getMonthValue()) + "-" +
                String.format("%02d", dateNow.getDayOfMonth());

        if(!decoratedItem.date().isEmpty()) {
            userTasks = userTasks.stream()
                    .filter(task -> task.get("dateTime").toString().startsWith(decoratedItem.date()))
                    .collect(Collectors.toCollection(ArrayList::new));
            System.out.println("Filter by specific date: "+userTasks);
        }else{
            userTasks = userTasks.stream()
                    .filter(task -> task.get("dateTime").toString().startsWith(query))
                    .collect(Collectors.toCollection(ArrayList::new));
            System.out.println("Filter by today: "+userTasks);
        }
    }

    private void handleAdd() {
        try{
            filterTaskByDate();
            if(userTasks.isEmpty()){
                CreateTaskRequest taskRequest = new CreateTaskRequest();
                taskRequest.setName(decoratedItem.task());
                taskRequest.setDescription("");
                taskRequest.setDueDate(decoratedItem.date());
                taskRequest.setDueTime(decoratedItem.time());
                taskRequest.setCategory(0);
                taskRequest.setPriority(0);
                taskService.createTask(taskRequest);

                serviceResponse.get("results").add(Map.of(
                        "intent", "ADD",
                        "message", "สร้างงานเรียบร้อยครับ :D"
                ));
            }else{
                userTasks.add(0, Map.of(
                        "id","-1",
                        "name", decoratedItem.task(),
                        "description", "",
                        "dateTime", (decoratedItem.date().isEmpty() && decoratedItem.time().isEmpty()) ? "" : toIsoOffsetDateTime(decoratedItem.date(), decoratedItem.time()),
                        "isFinished",false,
                        "userId","-1"

                ));
                serviceResponse.get("errors").add(Map.of(
                        "intent", "ADD",
                        "message", "ในวันนี้มีงานอยู่แล้ว คุณยืนยันที่จะเพิ่มงานนี้หรือไม่",
                        "output", userTasks
                ));
            }
        } catch (Exception e) {
            serviceResponse.get("errors").add(Map.of("intent", "ADD", "message", e.getMessage()));
        }


    }

    private void handleEdit() {
        try{

            if (userTasks.isEmpty()) {

                serviceResponse.get("errors").add(Map.of("intent", "EDIT", "message", "ตรวจสอบแล้ว ไม่พบงานที่ต้องการแก้ไขครับ"));
            } else if (userTasks.size() == 1) {
                UpdateTaskRequest taskRequest = new UpdateTaskRequest();
                taskRequest.setName(decoratedItem.task());
                if(!decoratedItem.date().isEmpty() && !decoratedItem.time().isEmpty()) {
                    taskRequest.setDateTime(decoratedItem.date() + "T" + decoratedItem.time() + ":00+07:00");
                }
                taskService.updateTask((String) userTasks.get(0).get("id"),taskRequest);
                serviceResponse.get("results").add(Map.of(
                        "intent", "EDIT",
                        "message", "แก้ไขงานเรียบร้อยครับ :D"
                ));
            }else{
                userTasks.add(0, Map.of(
                        "id","-1",
                        "name", decoratedItem.task(),
                        "description", "",
                        "dateTime", (decoratedItem.date().isEmpty() && decoratedItem.time().isEmpty()) ? "" : toIsoOffsetDateTime(decoratedItem.date(), decoratedItem.time()),
                        "isFinished",false,
                        "userId","-1"

                ));
                serviceResponse.get("errors").add(Map.of(
                        "intent", "EDIT",
                        "message", "ตรวจสอบแล้ว พบงานที่แก้ไขได้หลายงาน กรุณากดเลือกแก้ไขครับ",
                        "output", userTasks));
            }
        } catch (Exception e) {
            serviceResponse.get("errors").add(Map.of("intent", "EDIT", "message", e.getMessage()));
        }

    }
    private void handleRemove() {
        try{
            filterTaskByDate();
            if (userTasks.isEmpty()) {

                serviceResponse.get("errors").add(Map.of("intent", "REMOVE", "message", "ตรวจสอบแล้ว ไม่พบงานที่ต้องการลบครับ"));
            } else if (userTasks.size() == 1) {
                taskService.deleteTask((String) userTasks.get(0).get("id"));
                serviceResponse.get("results").add(Map.of(
                        "intent", "REMOVE",
                        "message", "ลบงานเรียบร้อยครับ :D"
                ));
            }else{

                serviceResponse.get("errors").add(Map.of(
                        "intent", "REMOVE",
                        "message", "ตรวจสอบแล้ว พบงานที่ลบได้หลายงาน กรุณากดเลือกลบครับ",
                        "output", userTasks));
            }
        } catch (Exception e) {
            serviceResponse.get("errors").add(Map.of("intent", "REMOVE", "message", e.getMessage()));
        }
    }

    private String toIsoOffsetDateTime(String dateStr, String timeStr) {
        if(dateStr.isEmpty()){
            dateStr = LocalDate.now(ZoneId.of("Asia/Bangkok")).toString();
        }
        if(timeStr.isEmpty()){
            timeStr = LocalTime.now(ZoneId.of("Asia/Bangkok")).toString();
        }

        LocalDate date = LocalDate.parse(dateStr);
        LocalTime time = LocalTime.parse(timeStr);

        LocalDateTime localDateTime = LocalDateTime.of(date, time);

        ZoneId bangkokZoneId = ZoneId.of("Asia/Bangkok");
        ZonedDateTime zonedDateTime = localDateTime.atZone(bangkokZoneId);

        return zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private void handleGenForm(){
        String userRequest = decoratedItem.text();
        String reportYm = "";
        if(!userRequest.contains("สรุปงาน")){
            serviceResponse.get("errors").add(Map.of(
                    "intent", "GENFORM",
                    "message", "ไม่รองรับคำขอสร้างฟอร์มนี้ครับ"
            ));
            return;
        }
        if(userRequest.contains("เดือนนี้")){
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Bangkok"));
            reportYm = String.format("%04d-%02d", today.getYear(), today.getMonthValue());
        }else{
            for(Map.Entry<String, Integer> entry : StringUtil.THAI_MONTHS.entrySet()){
                if(userRequest.contains(entry.getKey())){
                    int month = entry.getValue();
                    int year = Year.now(ZoneId.of("Asia/Bangkok")).getValue();
                    var thaiYearMatcher = StringUtil.THAI_YEAR_PATTERN.matcher(userRequest);
                    if(thaiYearMatcher.find()){
                        year = Integer.parseInt(thaiYearMatcher.group(1)) - 543;
                    }else{
                        var yearMatcher = StringUtil.YEAR_PATTERN.matcher(userRequest);
                        if(yearMatcher.find()){
                            year = Integer.parseInt(yearMatcher.group(1));
                        }
                    }
                    reportYm = String.format("%04d-%02d", year, month);
                    break;
                }
            }
        }
        try{
            System.out.println(reportYm);
            String uid = SecurityContextHolder.getContext().getAuthentication().getName();
            List<Map<String, Object>> tasks = taskService.getTasksByDate(reportYm).get();

            byte[] report = formService.getMonthlyTaskReport(reportYm, tasks);
            InputStream inputStream = new ByteArrayInputStream(report);

            String reportType = "monthly_task_report";
            String downloadUrl = formService.uploadPdfFile(uid, inputStream, reportType, reportYm);
            serviceResponse.get("results").add(Map.of(
                    "intent", "GENFORM",
                    "message", downloadUrl
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
