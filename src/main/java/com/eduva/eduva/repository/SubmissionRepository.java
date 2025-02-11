package com.eduva.eduva.repository;

import com.eduva.eduva.model.AssignmentData;
import com.eduva.eduva.model.CourseData;
import com.eduva.eduva.model.SubmissionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<SubmissionData, Long> {
    Optional<SubmissionData> findByAssignmentAndStudentId(AssignmentData assignment, Long studentId);
}

