package com.eduva.eduva.dto;

import java.util.List;
import java.util.Map;

public class QuestionIdsWithCacheFiles {
    private List<String> questionIds;
    private List<Map<String, Object>> processedFiles;

    public QuestionIdsWithCacheFiles(List<String> questionIds, List<Map<String, Object>> processedFiles) {
        this.questionIds = questionIds;
        this.processedFiles = processedFiles;
    }

    public List<String> getQuestionIds() {
        return questionIds;
    }

    public List<Map<String, Object>> getProcessedFiles() {
        return processedFiles;
    }
}
