package com.eduva.eduva.repository;

import com.eduva.eduva.model.CourseData;
import com.eduva.eduva.model.RubricData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RubricRepository extends JpaRepository<RubricData, Long> {
}

