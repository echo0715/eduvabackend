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
    private Boolean isPureText;

    public ClaudeQuestionInfo(
            String questionId,
            String content,
            String rubric,
            String grade,
            String explanation,
            String maxGrade,
            String preContext,
            Boolean isPureText) {

        this.questionId = questionId;
        this.content = content;
        this.rubric = rubric;
        this.grade = grade;
        this.explanation = explanation;
        this.maxGrade = maxGrade;
        this.preContext = preContext;
        this.isPureText = isPureText;
    }

    // You might also want a no-args constructor for frameworks like Spring/Jackson
    public ClaudeQuestionInfo() {
    }

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

    public Boolean getIsPureText() {
        return isPureText;
    }

    public void setIsPureText(Boolean pureText) {
        isPureText = pureText;
    }
}
