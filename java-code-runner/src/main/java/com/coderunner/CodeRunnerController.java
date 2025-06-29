package com.coderunner;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*") // Allows all frontend origins, adjust for production
public class CodeRunnerController {

    private final DockerExecutionService dockerExecutionService;
    private final LanguageConfig languageConfig;

    public CodeRunnerController(DockerExecutionService dockerExecutionService, LanguageConfig languageConfig) {
        this.dockerExecutionService = dockerExecutionService;
        this.languageConfig = languageConfig;
    }

    @PostMapping("/run")
    public ResponseEntity<?> runCode(@RequestBody SubmissionRequest submissionRequest) {
        if (submissionRequest.getCode() == null || submissionRequest.getLanguage() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Field 'code' and 'language' are required."));
        }
        if (!languageConfig.getSupportedLanguages().containsKey(submissionRequest.getLanguage())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Unsupported language: " + submissionRequest.getLanguage()));
        }

        try {
            SubmissionResponse response = dockerExecutionService.executeCode(submissionRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while running the code."));
        }
    }
}