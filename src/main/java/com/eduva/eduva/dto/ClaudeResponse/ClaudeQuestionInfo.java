package com.eduva.eduva.dto.ClaudeResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClaudeQuestionInfo {
    @JsonProperty("question_id")
    private String questionId;
    private String content;

    // rubric contain the rubric image key
    private String rubric;
    private String grade;
    private String explanation;
    private String maxGrade;
    private String preContext;
    private Boolean isPureText;
    private String rubricType;

    //rubric text contain the text version of the iage
    private String rubricText;

    public ClaudeQuestionInfo(
            String questionId,
            String content,
            String rubric,
            String grade,
            String explanation,
            String maxGrade,
            String preContext,
            Boolean isPureText,
            String rubricType) {

        this.questionId = questionId;
        this.content = content;
        this.rubric = rubric;
        this.grade = grade;
        this.explanation = explanation;
        this.maxGrade = maxGrade;
        this.preContext = preContext;
        this.isPureText = isPureText;
        this.rubricType = rubricType;
    }

    // You might also want a no-args constructor for frameworks like Spring/Jackson
    public ClaudeQuestionInfo() {
    }

    public ClaudeQuestionInfo(String questionId) {
        this.questionId = questionId;
        // Initialize other fields as needed
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

    public Boolean getPureText() {
        return isPureText;
    }

    public void setPureText(Boolean pureText) {
        isPureText = pureText;
    }

    public String getRubricType() {
        return rubricType;
    }

    public void setRubricType(String rubricType) {
        this.rubricType = rubricType;
    }

    public String getRubricText() {
        return rubricText;
    }

    public void setRubricText(String rubricText) {
        this.rubricText = rubricText;
    }
}
