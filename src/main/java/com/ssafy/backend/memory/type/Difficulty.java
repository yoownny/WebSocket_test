package com.ssafy.backend.memory.type;

public enum Difficulty {
    EASY("쉬움", 1),
    NORMAL("보통", 2),
    HARD("어려움", 3);

    private final String displayName;
    private final int level; // 정렬이나 필터링에 유용

    Difficulty(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }
}
