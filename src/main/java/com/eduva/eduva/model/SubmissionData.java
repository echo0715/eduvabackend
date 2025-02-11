package com.eduva.eduva.model;

import com.eduva.eduva.model.enums.SubmissionStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submissions")
public class SubmissionData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    @JsonIgnoreProperties("submissions")
    private AssignmentData assignment;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnoreProperties("submissions")
    private UserData student;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "submission_content")
    private String submissionContent;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionGrading> questions = new ArrayList<>();

    @Column(name = "submit_file_name")
    private String submitFileName;

    @Column(name = "grade")
    private Long grade;


    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SubmissionStatus status = SubmissionStatus.NO_SUBMISSION;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AssignmentData getAssignment() {
        return assignment;
    }

    public void setAssignment(AssignmentData assignment) {
        this.assignment = assignment;
    }

    public UserData getStudent() {
        return student;
    }

    public void setStudent(UserData student) {
        this.student = student;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getSubmissionContent() {
        return submissionContent;
    }

    public void setSubmissionContent(String submissionContent) {
        this.submissionContent = submissionContent;
    }

    public List<QuestionGrading> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionGrading> questions) {
        this.questions = questions;
    }

    public Long getGrade() {
        return grade;
    }

    public void setGrade(Long grade) {
        this.grade = grade;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public String getSubmitFileName() {
        return submitFileName;
    }

    public void setSubmitFileName(String submitFileName) {
        this.submitFileName = submitFileName;
    }
}
