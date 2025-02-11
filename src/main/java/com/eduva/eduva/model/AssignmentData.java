package com.eduva.eduva.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRawValue;
import jakarta.persistence.*;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "assignments")
public class AssignmentData{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private CourseData course;

    @OneToMany(mappedBy = "assignment")
    private List<RubricData> rubrics;

    @OneToMany(mappedBy = "assignment")
    private List<SubmissionData> submissions;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "assignment_id")
    @Where(clause = "file_type = 'RUBRIC'")
    private List<FileData> rubricFiles;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "assignment_id")
    @Where(clause = "file_type = 'PROBLEMSET'")
    private List<FileData> problemSetFiles;

    @Column(name = "rubric_content", columnDefinition = "TEXT")
    private String rubric_content;


    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getRubric_content() {
        return rubric_content;
    }

    public void setRubric_content(String rubric_content) {
        this.rubric_content = rubric_content;
    }

    public CourseData getCourse() {
        return course;
    }

    public void setCourse(CourseData course) {
        this.course = course;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<RubricData> getRubrics() {
        return rubrics;
    }

    public void setRubrics(List<RubricData> rubrics) {
        this.rubrics = rubrics;
    }

    public List<SubmissionData> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(List<SubmissionData> submissions) {
        this.submissions = submissions;
    }

    public List<FileData> getRubricFiles() {
        return rubricFiles;
    }

    public void setRubricFiles(List<FileData> rubricFiles) {
        this.rubricFiles = rubricFiles;
    }

    public List<FileData> getProblemSetFiles() {
        return problemSetFiles;
    }

    public void setProblemSetFiles(List<FileData> problemSetFiles) {
        this.problemSetFiles = problemSetFiles;
    }
}

