package com.eduva.eduva.dto.ClaudeResponse;

import java.util.List;

public class ClaudeInputData {
    private List<ClaudeQuestionInfo> questions;

    public List<ClaudeQuestionInfo> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ClaudeQuestionInfo> questions) {
        this.questions = questions;
    }
}
