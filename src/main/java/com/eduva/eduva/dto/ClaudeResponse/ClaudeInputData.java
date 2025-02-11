package com.eduva.eduva.dto.ClaudeResponse;

import java.util.List;

public class ClaudeInputData {
    private List<ClaudeQuestionItem> questions;

    public List<ClaudeQuestionItem> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ClaudeQuestionItem> questions) {
        this.questions = questions;
    }
}
