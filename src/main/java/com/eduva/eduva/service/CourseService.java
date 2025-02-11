// CourseService.java
package com.eduva.eduva.service;


import com.eduva.eduva.dto.CourseCreateRequest;
import com.eduva.eduva.model.CourseData;
import com.eduva.eduva.model.UserData;
import com.eduva.eduva.repository.CourseRepository;
import com.eduva.eduva.repository.UserRepository;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;


    public List<CourseData> getTeacherCourses(Long teacherId) {
        return courseRepository.findByTeacherId(teacherId);
    }

    public CourseData createTeacherCourse(CourseCreateRequest courseCreateRequest) throws Exception {

        UserData teacher = userRepository.findById(courseCreateRequest.getTeacherId())
                .orElseThrow(() -> new Exception("Teacher not found with id: " + courseCreateRequest.getTeacherId()));
        // Create new course
        CourseData course = new CourseData();
        course.setName(courseCreateRequest.getCourseName());
        course.setDescription(courseCreateRequest.getDescription());
        course.setTeacher(teacher);
        course.setCreatedAt(LocalDateTime.now());

        // Generate unique course code (you can customize this generation logic)
        String courseCode = generateUniqueCourseCode();
        course.setCourseCode(courseCode);

        // Save and return the course
        return courseRepository.save(course);
    }

    private String generateUniqueCourseCode() {
        String code;
        do {
            // Take first 6 characters of a UUID
            code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (courseRepository.existsByCourseCode(code));

        return code;
    }
}