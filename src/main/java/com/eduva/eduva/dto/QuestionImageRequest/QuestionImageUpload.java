package com.eduva.eduva.dto.QuestionImageRequest;

import lombok.Data;

@Data
public class QuestionImageUpload {
    private String key;
    private String questionId;
    private String url;
}
