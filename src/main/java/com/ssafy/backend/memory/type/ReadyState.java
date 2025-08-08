package com.ssafy.backend.memory.type;

public enum ReadyState {
    READY("준비 완료"),
    WAITING("준비 대기");

    private final String description;

    ReadyState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
