package org.bkkz.lumabackend.service;

import org.bkkz.lumabackend.model.llm.llmResponse.DecoratedItem;
import org.bkkz.lumabackend.model.task.CreateTaskRequest;
import org.bkkz.lumabackend.model.task.UpdateTaskRequest;
import org.bkkz.lumabackend.utils.LLMIntent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LLMService {

    private final DecoratedItem decoratedItem;
    private final TaskService taskService;


    private List<Map<String, Object>> userTasks;
    private Map<String, List<Map<String, Object>>> serviceResponse;

    public LLMService(DecoratedItem decoratedItem, TaskService taskService) {
        this.decoratedItem = decoratedItem;
        this.taskService = taskService;
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
                // Logic for PLAN intent
                break;
        }

    }

    private void handleCheck() {
        try{
            List<Map<String, Object>> allUserTasks = taskService.getTasksByDate(null).get();
            userTasks = allUserTasks.stream()
                    .filter(task -> task.get("name").toString().contains(decoratedItem.task()))
                    .toList();
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
                    .toList();
            System.out.println("Filter by specific date: "+userTasks);
        }else{
            userTasks = userTasks.stream()
                    .filter(task -> task.get("dateTime").toString().startsWith(query))
                    .toList();
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
                taskService.createTask(taskRequest);

                serviceResponse.get("results").add(Map.of(
                        "intent", "ADD",
                        "message", "Task Created"
                ));
            }else{

                serviceResponse.get("errors").add(Map.of(
                        "intent", "ADD",
                        "message", "Task Exists on this date, created again?",
                        "output", userTasks
                ));
            }
        } catch (Exception e) {
            serviceResponse.get("errors").add(Map.of("intent", "ADD", "message", e.getMessage()));
        }


    }

    private void handleEdit() {
        try{
            filterTaskByDate();
            if (userTasks.isEmpty()) {

                serviceResponse.get("errors").add(Map.of("intent", "EDIT", "message", "Task not found for editing"));
            } else if (userTasks.size() == 1) {
                UpdateTaskRequest taskRequest = new UpdateTaskRequest();
                taskRequest.setName(decoratedItem.task());
                if(!decoratedItem.date().isEmpty() && !decoratedItem.time().isEmpty()) {
                    taskRequest.setDateTime(decoratedItem.date() + "T" + decoratedItem.time() + ":00+07:00");
                }
                taskService.updateTask((String) userTasks.get(0).get("id"),taskRequest);
                serviceResponse.get("results").add(Map.of(
                        "intent", "EDIT",
                        "message", "Task Edited"
                ));
            }else{
                serviceResponse.get("errors").add(Map.of(
                        "intent", "EDIT",
                        "message", "Many task found, choose which to edit",
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

                serviceResponse.get("errors").add(Map.of("intent", "REMOVE", "message", "Task not found for remove"));
            } else if (userTasks.size() == 1) {
                taskService.deleteTask((String) userTasks.get(0).get("id"));
                serviceResponse.get("results").add(Map.of(
                        "intent", "REMOVE",
                        "message", "Task Removed"
                ));
            }else{

                serviceResponse.get("errors").add(Map.of(
                        "intent", "REMOVE",
                        "message", "Many task found, choose which to remove",
                        "output", userTasks));
            }
        } catch (Exception e) {
            serviceResponse.get("errors").add(Map.of("intent", "REMOVE", "message", e.getMessage()));
        }
    }

}
