package com.coderunner;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmissionResponse {
    private String stdout;
    private String stderr;
    private double executionTimeMs;
    private String status;
}