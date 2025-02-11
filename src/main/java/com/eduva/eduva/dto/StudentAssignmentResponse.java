package com.eduva.eduva.dto;

import com.eduva.eduva.model.AssignmentData;
import com.eduva.eduva.model.CourseData;
import com.eduva.eduva.model.SubmissionData;
import com.eduva.eduva.model.enums.SubmissionStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data  // This will automatically generate getters, setters, toString, equals, and hashCode
@NoArgsConstructor
public class StudentAssignmentResponse {
    private Long id;
    private CourseData course;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate dueDate;
    private String rubricContent;
    private SubmissionStatus submissionStatus;
    private Long grade;
    private String submissionContent;

    public StudentAssignmentResponse(AssignmentData assignment, SubmissionData submission) {
        this.id = assignment.getId();
        this.title = assignment.getTitle();
        this.description = assignment.getDescription();
        this.startDate = assignment.getStartDate();
        this.dueDate = assignment.getDueDate();
        this.rubricContent = assignment.getRubric_content();
        this.course = assignment.getCourse();


        if (submission != null) {
            this.submissionStatus = submission.getStatus();
            this.grade = submission.getGrade();
            this.submissionContent = submission.getSubmissionContent();
        } else {
            this.submissionStatus = null;
            this.grade = null;
            this.submissionContent = null;
        }
    }
}
