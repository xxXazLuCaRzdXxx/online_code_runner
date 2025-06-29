package com.coderunner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class DockerExecutionService {

    private final DockerClient dockerClient;
    private final DockerContainerManager containerManager;
    private final LanguageConfig languageConfig;

    public DockerExecutionService(DockerClient dockerClient, DockerContainerManager containerManager, LanguageConfig languageConfig) {
        this.dockerClient = dockerClient;
        this.containerManager = containerManager;
        this.languageConfig = languageConfig;
    }

    public SubmissionResponse executeCode(SubmissionRequest request) throws IOException, InterruptedException {
        String containerId = containerManager.getContainerId(request.getLanguage());
        if (containerId == null) {
            throw new IllegalArgumentException("Container for language " + request.getLanguage() + " is not available.");
        }

        LanguageConfig.LanguageDetails details = languageConfig.getSupportedLanguages().get(request.getLanguage());
        String scriptFileName = "script." + details.getExt();
        Path hostTempDir = Path.of(System.getProperty("java.io.tmpdir"));

        Path scriptFilePath = hostTempDir.resolve(scriptFileName);
        Path inputFilePath = hostTempDir.resolve("input.txt");

        try {
            Files.writeString(scriptFilePath, request.getCode(), StandardCharsets.UTF_8);
            if (request.getInput() != null) {
                Files.writeString(inputFilePath, request.getInput(), StandardCharsets.UTF_8);
            } else {
                Files.writeString(inputFilePath, "", StandardCharsets.UTF_8);
            }
            
            // THE FIX: Create a shell command that uses input redirection '<'
            // This reads from /tmp/input.txt and pipes it into the script's standard input.
            String command = String.format("%s /tmp/%s < /tmp/input.txt", String.join(" ", details.getCmd()), scriptFileName);
            String[] executionCmd = new String[]{"sh", "-c", command};

            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withCmd(executionCmd)
                    .exec();

            final StringBuilder stdout = new StringBuilder();
            final StringBuilder stderr = new StringBuilder();
            
            ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame frame) {
                    if (frame.getStreamType() == com.github.dockerjava.api.model.StreamType.STDOUT) {
                        stdout.append(new String(frame.getPayload()));
                    } else {
                        stderr.append(new String(frame.getPayload()));
                    }
                }
            };
            
            long startTime = System.nanoTime();
            
            boolean completed = dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(callback)
                .awaitCompletion(5, TimeUnit.SECONDS);

            long endTime = System.nanoTime();
            double executionTimeMs = (endTime - startTime) / 1_000_000.0;
            
            String status = "Completed";
            if (!completed) {
                status = "Timeout";
                stderr.append("\nError: Code execution timed out after 5 seconds.");
            } else if (stderr.length() > 0) {
                status = "Error";
            }

            return SubmissionResponse.builder()
                .stdout(stdout.toString().trim())
                .stderr(stderr.toString().trim())
                .executionTimeMs(executionTimeMs)
                .status(status)
                .build();

        } finally {
             Files.deleteIfExists(scriptFilePath);
             Files.deleteIfExists(inputFilePath);
        }
    }
}