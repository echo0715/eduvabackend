package com.eduva.eduva.dto.ClaudeResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClaudeQuestionItem {
    @JsonProperty("question_id")
    private String questionId;
    private String grade;
    private String explanation;

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
