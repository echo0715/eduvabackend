package com.eduva.eduva.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "grading_questions")
public class QuestionGrading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign-key link to the SubmissionData table
    @ManyToOne
    @JoinColumn(name = "submission_id", nullable = false)
    @JsonIgnoreProperties("questions")
    private SubmissionData submission;

    // Fields for each piece of info you need
    private Long assignmentId;

    @Column(name = "question_id")
    @JsonProperty("question_id")
    private String questionId;

    private String grade;

     @Lob
     @Column(columnDefinition = "TEXT")
    private String explanation;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SubmissionData getSubmission() {
        return submission;
    }

    public void setSubmission(SubmissionData submission) {
        this.submission = submission;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

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
