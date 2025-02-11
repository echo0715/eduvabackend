package com.eduva.eduva.dto.ClaudeResponse;

public class ClaudeContentItem {
    private String type;
    private String name;       // "grading_tool"
    private String tool;       // "tool_use"
    private ClaudeInputData input;   // null if content.type != "tool_use"
    private String text;       // used when content.type == "text"

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public ClaudeInputData getInput() {
        return input;
    }

    public void setInput(ClaudeInputData input) {
        this.input = input;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
