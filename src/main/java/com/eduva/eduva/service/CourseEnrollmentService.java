package com.eduva.eduva.service;

import com.eduva.eduva.model.*;
import com.eduva.eduva.model.enums.EnrollmentStatus;
import com.eduva.eduva.model.enums.SubmissionStatus;
import com.eduva.eduva.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseEnrollmentService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private SubmissionRepository submissionRepository;

    public CourseEnrollmentData enrollStudentInCourse(Long userId, String courseCode) {
        UserData student = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Verify the user is a student
        if (!"S".equals(student.getRole())) {
            throw new IllegalStateException("User is not a student");
        }

        CourseData course = courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        // Check if already enrolled
        if (enrollmentRepository.existsByCourseAndStudent(course, student)) {
            throw new IllegalStateException("Student is already enrolled in this course");
        }

        CourseEnrollmentData enrollment = new CourseEnrollmentData();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        List<AssignmentData> assignments = assignmentRepository.findByCourseId(course.getId());

        List<SubmissionData> submissions = new ArrayList<>();

        for (AssignmentData assignment : assignments) {
            SubmissionData submission = new SubmissionData();
            submission.setAssignment(assignment);
            submission.setStudent(student);
            submission.setStatus(SubmissionStatus.NO_SUBMISSION);
            submissions.add(submission);  // Add each submission to the list
        }
        submissionRepository.saveAll(submissions);

        return enrollmentRepository.save(enrollment);
    }

    public List<CourseData> getStudentCourses(Long userId) {
        UserData student = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!"S".equals(student.getRole())) {
            throw new IllegalStateException("User is not a student");
        }

        return enrollmentRepository.findByStudent(student)
                .stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .map(CourseEnrollmentData::getCourse)
                .collect(Collectors.toList());
    }

    public List<UserData> getCourseStudents(String courseCode) {
        CourseData course = courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        return enrollmentRepository.findByCourse(course)
                .stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .map(CourseEnrollmentData::getStudent)
                .collect(Collectors.toList());
    }
}
