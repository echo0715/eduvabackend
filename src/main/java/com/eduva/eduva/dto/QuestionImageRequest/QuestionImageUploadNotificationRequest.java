package com.eduva.eduva.dto.QuestionImageRequest;

import lombok.Data;

import java.util.List;

@Data
public class QuestionImageUploadNotificationRequest {
    private Long assignmentId;
    private List<QuestionImageUpload> uploadResults;
}
