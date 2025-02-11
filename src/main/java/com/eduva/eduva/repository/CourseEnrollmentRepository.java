package com.eduva.eduva.repository;

import com.eduva.eduva.model.CourseData;
import com.eduva.eduva.model.CourseEnrollmentData;
import com.eduva.eduva.model.UserData;
import com.eduva.eduva.model.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollmentData, Long> {
    List<CourseEnrollmentData> findByCourse(CourseData course);
    List<CourseEnrollmentData> findByStudent(UserData student);
    Optional<CourseEnrollmentData> findByCourseAndStudent(CourseData course, UserData student);
    boolean existsByCourseAndStudent(CourseData course, UserData student);
    List<CourseEnrollmentData> findByCourseAndStatus(CourseData course, EnrollmentStatus status);
}
