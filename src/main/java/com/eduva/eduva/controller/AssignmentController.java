package com.eduva.eduva.controller;

import com.eduva.eduva.dto.*;
import com.eduva.eduva.dto.ClaudeResponse.ClaudeQuestionItem;
import com.eduva.eduva.model.AssignmentData;
import com.eduva.eduva.model.SubmissionData;
import com.eduva.eduva.service.AssignmentService;
import com.eduva.eduva.service.ClaudeService;
import com.eduva.eduva.service.FileStorageService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/assignments")
@CrossOrigin(
        origins = {"http://localhost:3000"},
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowCredentials = "true"
)
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping(value = "/teacher/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssignmentData> createAssignment(@ModelAttribute AssignmentCreateRequest assignmentCreateRequest) {
        try {
            AssignmentData createdAssignment = assignmentService.createAssignment(assignmentCreateRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAssignment);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating assignment");
        }
    }

    @GetMapping("/teacher/{courseId}")
    public List<AssignmentData> getAssignmentsByCourseId(@PathVariable Long courseId) {
        return assignmentService.findByCourseId(courseId);
    }

    @PostMapping("/updaterubric/{assignmentId}")
    public ResponseEntity<?> updateRubricContent(@PathVariable Long assignmentId, @RequestBody String rubricContent) {
        try {
            AssignmentData updatedAssignment = assignmentService.updateRubricContent(assignmentId, rubricContent);
            return ResponseEntity.ok(updatedAssignment);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Failed to update rubric: " + e.getMessage());
        }
    }

    @PostMapping("/student/getAssignmentsByStudentIdCourseId")
    public ResponseEntity<List<StudentAssignmentResponse>> getAssignmentsByStudentIdCourseId(@RequestBody StudentAssignmentRequest studentAssignmentRequest) {
        try {
            List<StudentAssignmentResponse> studentAssignmentResponse = assignmentService.getAssignmentsForStudentInCourse(studentAssignmentRequest.getStudentId(), studentAssignmentRequest.getCourseId());
            return ResponseEntity.ok(studentAssignmentResponse);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error getting students' assignment");
        }

    }

    @PostMapping("/student/submit")
    public ResponseEntity<SubmissionData> submitAssignment(@RequestParam("studentId") Long studentId, @RequestParam("assignmentId") Long assignmentId, @RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new BadRequestException("File cannot be empty");
            }
            // Validate file size (10MB max)
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new BadRequestException("File size exceeds maximum limit of 10MB");
            }
            String fileName = fileStorageService.storeFile(file, studentId, assignmentId);
            // Create submission record
            SubmissionData submission = assignmentService.createSubmission(studentId, assignmentId, fileName);

            return ResponseEntity.ok(submission);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/getsubmission/{assignmentId}")
    public ResponseEntity<List<ViewSubmissionResponse>> getSubmission(@PathVariable Long assignmentId) {
        try {
            List<ViewSubmissionResponse> allSubmissions = assignmentService.getViewAssignment(assignmentId);
            return ResponseEntity.ok(allSubmissions);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error getting students' submission");
        }
    }

    @GetMapping("submissiondetails/{submissionId}")
    public ResponseEntity<SubmissionData> getSubmissionDetails(@PathVariable Long submissionId) {
        try {
            SubmissionData submissionDetail = assignmentService.getSubmissionDetails(submissionId);
            return ResponseEntity.ok(submissionDetail);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error getting students' submission details");
        }
    }

    @PostMapping("autograde/{submissionId}")
    public ResponseEntity<List<ClaudeQuestionItem>> autoGradeSubmission(@PathVariable Long submissionId) {
        try{
            System.out.println(submissionId);
            List<ClaudeQuestionItem> claudeQuestionItemList = assignmentService.autoGrade(submissionId);
            return ResponseEntity.ok(claudeQuestionItemList);
        } catch(Exception e) {
            System.out.println(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error grading students' response ");
        }
    }
}
