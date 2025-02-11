package com.eduva.eduva.dto.ClaudeResponse;

import java.util.List;

public class ClaudeResponseBody {
    private String id;
    private String type;
    private String model;
    private String role;
    private List<ClaudeContentItem> content;
    private ClaudeUsage usage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<ClaudeContentItem> getContent() {
        return content;
    }

    public void setContent(List<ClaudeContentItem> content) {
        this.content = content;
    }

    public ClaudeUsage getUsage() {
        return usage;
    }

    public void setUsage(ClaudeUsage usage) {
        this.usage = usage;
    }
}
