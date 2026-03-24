package com.vantage.dialer.api.dto;

import java.util.List;

public class IvrFlowRequest {
    private String name;
    private String description;
    private List<IvrStepRequest> steps;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<IvrStepRequest> getSteps() {
        return steps;
    }

    public void setSteps(List<IvrStepRequest> steps) {
        this.steps = steps;
    }
}
