package com.eduva.eduva.dto.ClaudeResponse;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;


public class ClaudeInputData {
    @JsonDeserialize(using = QuestionsDeserializer.class)
    private List<ClaudeQuestionInfo> questions;

    public List<ClaudeQuestionInfo> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ClaudeQuestionInfo> questions) {
        this.questions = questions;
    }
}
