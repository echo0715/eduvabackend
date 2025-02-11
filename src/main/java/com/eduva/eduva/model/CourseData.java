package com.eduva.eduva.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "courses")
public class CourseData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private UserData teacher;

    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "course_code")
    private String courseCode;

    @OneToMany(mappedBy = "course")
    @JsonIgnore
    private List<AssignmentData> assignments;

    @OneToMany(mappedBy = "course")
    @JsonIgnore
    private Set<CourseEnrollmentData> enrollments;



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UserData getTeacher() {
        return teacher;
    }

    public void setTeacher(UserData teacher) {
        this.teacher = teacher;
    }


    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<AssignmentData> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<AssignmentData> assignments) {
        this.assignments = assignments;
    }

    public void addAssignment(AssignmentData assignment) {
        assignments.add(assignment);
        assignment.setCourse(this);
    }

    // Helper method to remove an assignment
    public void removeAssignment(AssignmentData assignment) {
        assignments.remove(assignment);
        assignment.setCourse(null);
    }
}