package com.eduva.eduva.dto.ClaudeResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClaudeQuestionInfo {
    @JsonProperty("question_id")
    private String questionId;
    private String content;
    private String rubric;
    private String grade;
    private String explanation;
    private String maxGrade;
    private String preContext;

    // Getters and setters
    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRubric() {
        return rubric;
    }

    public void setRubric(String rubric) {
        this.rubric = rubric;
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

    public String getMaxGrade() {
        return maxGrade;
    }

    public void setMaxGrade(String maxGrade) {
        this.maxGrade = maxGrade;
    }

    public String getPreContext() {
        return preContext;
    }

    public void setPreContext(String preContext) {
        this.preContext = preContext;
    }
}
