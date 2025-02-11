package com.eduva.eduva.repository;

import com.eduva.eduva.model.AssignmentData;
import com.eduva.eduva.model.CourseData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<AssignmentData, Long> {
    List<AssignmentData> findByCourseId(Long courseId);
    @Query("""
        SELECT DISTINCT a, 
        (SELECT s FROM SubmissionData s 
         WHERE s.assignment = a 
         AND s.student.id = :studentId) as submission
        FROM AssignmentData a 
        WHERE a.course.id = :courseId 
        ORDER BY a.dueDate ASC
        """)
    List<AssignmentData> findByCourseWithSubmissionStatus(Long courseId, Long studentId);

}
