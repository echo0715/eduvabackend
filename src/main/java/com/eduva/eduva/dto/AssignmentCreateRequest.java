package com.eduva.eduva.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public class AssignmentCreateRequest {
    private String title;
    private LocalDateTime startDate;
    private LocalDateTime dueDate;
    private String description;
    private String rubric;
    private Long courseId;
    private Long teacherId;
    private String rubricOption;
    private String rubricInstruction;
    private List<String> problemSetFileIds;
    private List<String> rubricFileIds;
    private List<MultipartFile> problemSetFiles;  // Added
    private List<MultipartFile> rubricFiles;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRubricOption() {
        return rubricOption;
    }

    public String getRubricInstruction() {
        return rubricInstruction;
    }

    public void setRubricInstruction(String rubricInstruction) {
        this.rubricInstruction = rubricInstruction;
    }

    public void setRubricOption(String rubricOption) {
        this.rubricOption = rubricOption;
    }

    public String getRubric() {
        return rubric;
    }

    public void setRubric(String rubric) {
        this.rubric = rubric;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public List<String> getProblemSetFileIds() {
        return problemSetFileIds;
    }

    public void setProblemSetFileIds(List<String> problemSetFileIds) {
        this.problemSetFileIds = problemSetFileIds;
    }

    public List<String> getRubricFileIds() {
        return rubricFileIds;
    }

    public void setRubricFileIds(List<String> rubricFileIds) {
        this.rubricFileIds = rubricFileIds;
    }

    public List<MultipartFile> getProblemSetFiles() {
        return problemSetFiles;
    }

    public void setProblemSetFiles(List<MultipartFile> problemSetFiles) {
        this.problemSetFiles = problemSetFiles;
    }

    public List<MultipartFile> getRubricFiles() {
        return rubricFiles;
    }

    public void setRubricFiles(List<MultipartFile> rubricFiles) {
        this.rubricFiles = rubricFiles;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }
}
