package com.eduva.eduva.service;

import com.eduva.eduva.model.CourseData;
import com.eduva.eduva.model.CourseEnrollmentData;
import com.eduva.eduva.model.UserData;
import com.eduva.eduva.model.enums.EnrollmentStatus;
import com.eduva.eduva.repository.CourseEnrollmentRepository;
import com.eduva.eduva.repository.CourseRepository;
import com.eduva.eduva.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
