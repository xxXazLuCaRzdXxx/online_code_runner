package com.coderunner;

import lombok.Data;

@Data
public class SubmissionRequest {
    private String code;
    private String language;
    private String input; 
}