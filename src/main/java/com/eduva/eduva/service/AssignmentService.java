package com.eduva.eduva.service;

import com.eduva.eduva.dto.*;
import com.eduva.eduva.dto.ClaudeResponse.ClaudeQuestionInfo;
import com.eduva.eduva.model.*;
import com.eduva.eduva.model.enums.EnrollmentStatus;
import com.eduva.eduva.model.enums.SubmissionStatus;
import com.eduva.eduva.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ClaudeService claudeService;

    @Autowired
    AzureDocIntelligenceService azureDocIntelligenceService;


    public AssignmentData createAssignment(AssignmentCreateRequest assignmentCreateRequest) throws IOException {
        CourseData courseData = courseRepository.findById(assignmentCreateRequest.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        AssignmentData assignment = new AssignmentData();
        assignment.setTitle(assignmentCreateRequest.getTitle());
        assignment.setDescription(assignmentCreateRequest.getDescription());
        assignment.setStartDate(assignmentCreateRequest.getStartDate().toLocalDate());
        assignment.setDueDate(assignmentCreateRequest.getDueDate().toLocalDate());
        assignment.setRubricContent(assignmentCreateRequest.getRubric());
        assignment.setCourse(courseData);
        assignment.setCreatedAt(LocalDateTime.now());

        List<String> allFileName = new ArrayList<>();
        List<FileData> rubricFiles = new ArrayList<>();
        if (assignmentCreateRequest.getRubricFiles() != null) {
            for (MultipartFile file : assignmentCreateRequest.getRubricFiles()) {
                try {
                    String storedFileName = fileStorageService.storeFile(
                            file,
                            assignmentCreateRequest.getTeacherId(),  // Using teacherId
                            0L                   // Using 0 as placeholder since assignment ID isn't created yet
                    );

                    FileData fileData = new FileData();
                    fileData.setFileId(storedFileName);  // Using the stored filename as fileId
                    fileData.setFileType("RUBRIC");
                    rubricFiles.add(fileData);
                    allFileName.add(storedFileName);

                } catch (IOException e) {
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Failed to upload rubric file: " + file.getOriginalFilename()
                    );
                }
            }
        }
        assignment.setRubricFiles(rubricFiles);




        List<FileData> problemSetFiles = new ArrayList<>();
        if (assignmentCreateRequest.getProblemSetFiles() != null) {
            for (MultipartFile file : assignmentCreateRequest.getProblemSetFiles()) {
                try {
                    String storedFileName = fileStorageService.storeFile(
                            file,
                            assignmentCreateRequest.getTeacherId(),  // Using teacherId instead of studentId
                            0L                   // Using 0 as placeholder since assignment ID isn't created yet
                    );
                    FileData fileData = new FileData();
                    fileData.setFileId(storedFileName);  // Using the stored filename as fileId
                    fileData.setFileType("PROBLEMSET");
                    problemSetFiles.add(fileData);
                    allFileName.add(storedFileName);

                } catch (IOException e) {
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Failed to upload problem set file: " + file.getOriginalFilename()
                    );
                }
            }
        }

        assignment.setProblemSetFiles(problemSetFiles);

        List<String> fileUrls = new ArrayList<>();
        for (String filename : allFileName) {
            fileUrls.add(fileStorageService.generatePresignedUrl(filename));
        }
//        azureDocIntelligenceService.getFormatFigurePosition(fileUrls.get(0));

        String extractingIdPrompt = "I need to grade each question individually in a question set or an exam, or just an essay. Please identify the small unit of each question that requires separate grading, and output a json file contain those question IDs. In some cases, if there are big problem contain and multiple sub problem like (a),(b), (c) or (1), (2), (3),  please identify the question IDs to the smallest unit, which means like 3, 2(a), 4(1) etc.";
        QuestionIdsWithCacheFiles questionIdsWithCacheFiles = claudeService.extractQuestionIds(fileUrls, extractingIdPrompt, assignmentCreateRequest.getTeacherId());
//        String questionIdsString = String.join(",", questionIds);
        List<String> questionIds = questionIdsWithCacheFiles.getQuestionIds();
        List<Map<String, Object>> fileContents = questionIdsWithCacheFiles.getProcessedFiles();


        if (assignmentCreateRequest.getRubricOption().equals("upload")) {
            List<ClaudeQuestionInfo> formattedQuestions = claudeService.formatQuestions(fileContents, questionIds, assignmentCreateRequest.getTeacherId());
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonRubricContent = objectMapper.writeValueAsString(formattedQuestions);
            assignment.setRubricContent(jsonRubricContent);
        } else if (assignmentCreateRequest.getRubricOption().equals("create")) {
            List<ClaudeQuestionInfo> formattedQuestions = claudeService.formatQuestionsByAutoGenerateRubrics(fileContents, questionIds);
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonRubricContent = objectMapper.writeValueAsString(formattedQuestions);
            assignment.setRubricContent(jsonRubricContent);
        }

        AssignmentData savedAssignment = assignmentRepository.save(assignment);
        List<CourseEnrollmentData> activeEnrollments = courseEnrollmentRepository
                .findByCourseAndStatus(courseData, EnrollmentStatus.ACTIVE);

        List<SubmissionData> submissions = activeEnrollments.stream()
                .map(enrollment -> {
                    SubmissionData submission = new SubmissionData();
                    submission.setAssignment(savedAssignment);
                    submission.setStudent(enrollment.getStudent());
                    submission.setStatus(SubmissionStatus.NO_SUBMISSION);
                    return submission;
                })
                .collect(Collectors.toList());

        submissionRepository.saveAll(submissions);
        return savedAssignment;
    }

    public List<AssignmentData> findByCourseId(Long courseId) {
        return assignmentRepository.findByCourseId(courseId);
    }

    public AssignmentData updateRubricContent(Long id, String rubricContent) {
        AssignmentData assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        // Directly save the JSON string
        assignment.setRubricContent(rubricContent);
        return assignmentRepository.save(assignment);
    }

    public List<StudentAssignmentResponse> getAssignmentsForStudentInCourse(Long studentId, Long courseId) {
//        CourseData course = courseRepository.findById(courseId)
//                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        // Get all assignments for the course
        System.out.println(courseId);
        System.out.println(studentId);
        List<AssignmentData> assignments = assignmentRepository.findByCourseId(courseId);

        // For each assignment, get its submission status for this student
        return assignments.stream()
                .map(assignment -> {
                    Optional<SubmissionData> submission = submissionRepository
                            .findByAssignmentAndStudentId(assignment, studentId);

                    return new StudentAssignmentResponse(
                            assignment,
                            submission.orElse(null)
                    );
                })
                .collect(Collectors.toList());
    }

    public SubmissionData createSubmission(Long studentId, Long assignmentId, String fileName) {
        UserData student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        AssignmentData assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        // Check if submission is within deadline
//        LocalDate today = LocalDate.now();
//        if (today.isAfter(assignment.getDueDate())) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due Date Passed");
//        }

        // Create or update submission
        SubmissionData submission = submissionRepository
                .findByAssignmentAndStudentId(assignment, studentId)
                .orElse(new SubmissionData());

        submission.setStudent(student);
        submission.setAssignment(assignment);
        submission.setSubmitFileName(fileName);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.SUBMITTED);

        return submissionRepository.save(submission);
    }


    public List<ViewSubmissionResponse> getViewAssignment(Long assignmentId){
        AssignmentData assignmentData = assignmentRepository.getById(assignmentId);
        List<ViewSubmissionResponse> responses = new ArrayList<>();
        List<SubmissionData> submissions = assignmentData.getSubmissions();
        for (SubmissionData submission : submissions) {
            ViewSubmissionResponse response = new ViewSubmissionResponse();
            UserData student = submission.getStudent();
            response.setUserId(student.getId());
            response.setSubmissionId(submission.getId());
            response.setUsername(student.getUserName());
            response.setUserEmail(student.getEmail());
            response.setSubmissionStatus(submission.getStatus());
            response.setGrade(submission.getGrade());
            response.setSubmittedAt(submission.getSubmittedAt());
            responses.add(response);
        }

        return responses;
    }

    public SubmissionData getSubmissionDetails(Long submissionId) {
        SubmissionData submissionData = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Submission not found"));
        String fileName = submissionData.getSubmitFileName();
        String presignedUrl = fileStorageService.generatePresignedUrl(fileName);

        System.out.println(presignedUrl);


        // I don't want to create a new dto, so I just modify the file name to be the temporary url
        submissionData.setSubmitFileName(presignedUrl);

        return submissionData;
    }

    public List<ClaudeQuestionInfo> autoGrade(Long submissionId) throws IOException, InterruptedException {
        SubmissionData submissionData = submissionRepository.findById(submissionId).
                orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submssion data not found"));

        String studentfileurl = submissionData.getSubmitFileName();
        AssignmentData assignmentData = submissionData.getAssignment();
//        List<FileData> rubricList = assignmentData.getRubricFiles();
        List<FileData> problemSetFiles = assignmentData.getProblemSetFiles();

        String rubricContent = assignmentData.getRubricContent();

        List<String> fileIds = new ArrayList<>();

//        for (FileData fileData : rubricList) {
//            fileIds.add(fileData.getFileId());
//        }

//        for (FileData fileData : problemSetFiles) {
//            fileIds.add(fileData.getFileId());
//        }

        fileIds.add(studentfileurl);
        List<String> presignedUrls = new ArrayList<>();
        for (String fileId : fileIds) {
            String presignedUrl = fileStorageService.generatePresignedUrl(fileId);
            presignedUrls.add(presignedUrl);
        }

        return claudeService.autoGrade(presignedUrls, submissionData, rubricContent);
    }

}
