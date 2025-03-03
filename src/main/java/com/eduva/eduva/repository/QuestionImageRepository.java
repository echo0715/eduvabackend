package com.eduva.eduva.repository;

import com.eduva.eduva.model.QuestionGrading;
import com.eduva.eduva.model.QuestionImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionImageRepository extends JpaRepository<QuestionImage, Long> {
}
