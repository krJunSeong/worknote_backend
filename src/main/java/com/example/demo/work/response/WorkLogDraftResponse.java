package com.example.demo.work.response;

public class WorkLogDraftResponse {

    private String title;
    private String content;
    private String recognizedText;

    public WorkLogDraftResponse() {
    }

    public WorkLogDraftResponse(
            String title,
            String content,
            String recognizedText
    ) {
        this.title = title;
        this.content = content;
        this.recognizedText = recognizedText;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRecognizedText() {
        return recognizedText;
    }

    public void setRecognizedText(String recognizedText) {
        this.recognizedText = recognizedText;
    }
}
