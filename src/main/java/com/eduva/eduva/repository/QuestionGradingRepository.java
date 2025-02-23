package com.eduva.eduva.repository;

import com.eduva.eduva.model.QuestionGrading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface QuestionGradingRepository extends JpaRepository<QuestionGrading, Long> {
    @Transactional
    void deleteBySubmission_Id(Long submissionId);
}
