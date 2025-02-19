package com.eduva.eduva.controller;

import com.eduva.eduva.dto.CourseCreateRequest;
import com.eduva.eduva.dto.JoinCourseRequest;
import com.eduva.eduva.model.CourseData;
import com.eduva.eduva.model.CourseEnrollmentData;
import com.eduva.eduva.service.CourseEnrollmentService;
import com.eduva.eduva.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
@CrossOrigin(
        origins = {"http://localhost:3000"},
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowCredentials = "true"
)

public class CourseController {
    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseEnrollmentService courseEnrollmentService;

    @GetMapping("/teacher/{id}")
    public ResponseEntity<List<CourseData>> getTeacherCourses(@PathVariable Long id) {
        List<CourseData> courses = courseService.getTeacherCourses(id);
        return ResponseEntity.ok(courses);
    }

    @PostMapping("/teacher/create")
    public ResponseEntity<CourseData> createTeacherCourse(@RequestBody CourseCreateRequest courseCreateRequest) {
        try {
            CourseData createdCourse = courseService.createTeacherCourse(courseCreateRequest);
            return ResponseEntity.ok(createdCourse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/student/{id}")
    public ResponseEntity<List<CourseData>> getStudentCourses(@PathVariable Long id) {
        try {
            List<CourseData> joinedCourses = courseEnrollmentService.getStudentCourses(id);
            return ResponseEntity.ok(joinedCourses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/join")
    public ResponseEntity<CourseEnrollmentData> joinCourse(@RequestBody JoinCourseRequest joinCourseRequest) {
        try {
            CourseEnrollmentData joinedCourse = courseEnrollmentService.enrollStudentInCourse(joinCourseRequest.getStudentId(), joinCourseRequest.getCourseCode());
            return ResponseEntity.ok(joinedCourse);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }
}
