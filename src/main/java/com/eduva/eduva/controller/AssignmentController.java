package com.eduva.eduva.controller;

import com.eduva.eduva.dto.*;
import com.eduva.eduva.dto.ClaudeResponse.ClaudeQuestionInfo;
import com.eduva.eduva.dto.QuestionImageRequest.QuestionImageUpload;
import com.eduva.eduva.dto.QuestionImageRequest.QuestionImageUploadNotificationRequest;
import com.eduva.eduva.model.AssignmentData;
import com.eduva.eduva.model.QuestionImage;
import com.eduva.eduva.model.SubmissionData;
import com.eduva.eduva.service.AssignmentService;
import com.eduva.eduva.service.FileStorageService;
import com.eduva.eduva.service.QuestionImageService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private QuestionImageService questionImageService;

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
    public ResponseEntity<List<ClaudeQuestionInfo>> autoGradeSubmission(@PathVariable Long submissionId) {
        try{
            System.out.println(submissionId);
            List<ClaudeQuestionInfo> claudeQuestionItemList = assignmentService.autoGrade(submissionId);
            return ResponseEntity.ok(claudeQuestionItemList);
        } catch(Exception e) {
            System.out.println(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error grading students' response ");
        }
    }

    @PostMapping("predesign-url")
    public ResponseEntity<Map<String, String>> getPredesignUrl(@RequestBody Map<String, String> requestBody) {
        try {
            String fileId = requestBody.get("fileId");
            if (fileId == null || fileId.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("error", "File ID is required"));
            }

            // Retrieve the predesign URL from your service
            String predesignUrl = fileStorageService.generatePresignedUrl(fileId);
            // Create a response map
            Map<String, String> response = new HashMap<>();
            response.put("predesignUrl", predesignUrl);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to generate predesign URL: " + e.getMessage()));
        }
    }



    @PostMapping("/register-cos-uploads")
    public ResponseEntity<?> registerCosUploads(@RequestBody QuestionImageUploadNotificationRequest request) {
        try {
            // Process the uploaded files information
            Long assignmentId = request.getAssignmentId();
            List<QuestionImageUpload> uploadResults = request.getUploadResults();

            // Log or process the data as needed
            System.out.println("Received notification for assignment: " + assignmentId);
            System.out.println("Number of uploads: " + uploadResults.size());
            List<QuestionImage> questionImages = questionImageService.saveQuestionImages(uploadResults, assignmentId);

            // Return success response
            return ResponseEntity.ok().body(questionImages);
        } catch (Exception e) {
            // Log the exception
            System.err.println("Error processing upload notification: " + e.getMessage());
            e.printStackTrace();

            // Return error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process upload: " + e.getMessage());
        }
    }

    @PostMapping("/images")
    public ResponseEntity<Map<String, String>> getImageUrls(@RequestBody List<Map<String, Object>> imageQuestions) {
        Map<String, String> result = new HashMap<>();

        for (Map<String, Object> question : imageQuestions) {
            String questionId = question.get("question_id").toString();
            String imageKey = question.get("imageKey").toString();

            // Generate or retrieve the URL for this image key
            String imageUrl = fileStorageService.generateImagePresignedUrl(imageKey);

            result.put(questionId, imageUrl);
        }

        return ResponseEntity.ok(result);
    }
}
