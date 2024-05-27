package com.example.JChan;

import lombok.Data;

@Data
public class Reply {
    private String id;
    private String content;

    public Reply(String id, String content) {
        this.id = id;
        this.content = content;
    }

    // getters and setters
}
