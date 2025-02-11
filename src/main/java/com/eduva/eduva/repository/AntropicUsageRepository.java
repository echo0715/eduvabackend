package com.eduva.eduva.repository;

import com.eduva.eduva.model.AnthropicUsage;
import com.eduva.eduva.model.AssignmentData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AntropicUsageRepository extends JpaRepository<AnthropicUsage, Long> {
}
