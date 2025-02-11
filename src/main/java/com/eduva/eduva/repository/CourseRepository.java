package com.eduva.eduva.repository;

import com.eduva.eduva.model.CourseData;
import com.eduva.eduva.model.UserData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<CourseData, Long> {
    List<CourseData> findByTeacherId(Long teacherId);
    Optional<CourseData> findByCourseCode(String courseCode);
    boolean existsByCourseCode(String courseCode);
}
