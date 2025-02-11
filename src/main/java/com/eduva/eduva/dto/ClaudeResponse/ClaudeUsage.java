package com.eduva.eduva.dto.ClaudeResponse;

public class ClaudeUsage {
    private int cache_creation_input_tokens;
    private int cache_read_input_tokens;
    private int input_tokens;
    private int output_tokens;

    public int getCache_creation_input_tokens() {
        return cache_creation_input_tokens;
    }

    public void setCache_creation_input_tokens(int cache_creation_input_tokens) {
        this.cache_creation_input_tokens = cache_creation_input_tokens;
    }

    public int getCache_read_input_tokens() {
        return cache_read_input_tokens;
    }

    public void setCache_read_input_tokens(int cache_read_input_tokens) {
        this.cache_read_input_tokens = cache_read_input_tokens;
    }

    public int getInput_tokens() {
        return input_tokens;
    }

    public void setInput_tokens(int input_tokens) {
        this.input_tokens = input_tokens;
    }

    public int getOutput_tokens() {
        return output_tokens;
    }

    public void setOutput_tokens(int output_tokens) {
        this.output_tokens = output_tokens;
    }
}
