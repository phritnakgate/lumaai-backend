package org.bkkz.lumabackend.service;

import org.bkkz.lumabackend.model.llm.llmResponse.DecoratedItem;
import org.bkkz.lumabackend.utils.LLMIntent;
import org.springframework.stereotype.Service;

public class LLMService {

    private final DecoratedItem decoratedItem;

    public LLMService(DecoratedItem decoratedItem) {
        this.decoratedItem = decoratedItem;
    }

    public void processIntent() {
        for(String intent : decoratedItem.intent()) {
            Boolean result = doIntent(intent);
            if (!result) {
                System.out.println("Processed "+ intent +" failed.");
            } else {
                System.out.println("Processed " + intent +" success.");
            }
        }
    }

    private Boolean doIntent(String intent){
        try {
            LLMIntent llmIntent = LLMIntent.fromString(intent);

            switch (llmIntent) {
                case ADD:
                    // Logic for ADD intent
                    break;
                case CHECK:
                    // Logic for CHECK intent
                    break;
                case EDIT:
                    // Logic for EDIT intent
                    break;
                case REMOVE:
                    // Logic for REMOVE intent
                    break;
                case SEARCH:
                    // Logic for SEARCH intent
                    break;
                case GOOGLESEARCH:
                    // Logic for GOOGLESEARCH intent
                    break;
                case PLAN:
                    // Logic for PLAN intent
                    break;
            }
            return true; // Return true if the action was successful
        } catch (Exception e) {
            return false; // Return false if the intent is unknown
        }
    }

    private Boolean handleCheck(){
        // Implement the logic for CHECK intent
        return true;
    }
}
