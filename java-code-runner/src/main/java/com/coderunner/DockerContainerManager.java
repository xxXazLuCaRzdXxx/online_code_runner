package com.coderunner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DockerContainerManager {

    private final DockerClient dockerClient;
    private final LanguageConfig languageConfig;
    private final Map<String, String> containerPool = new ConcurrentHashMap<>();

    public DockerContainerManager(DockerClient dockerClient, LanguageConfig languageConfig) {
        this.dockerClient = dockerClient;
        this.languageConfig = languageConfig;
    }

    @PostConstruct
    private void initializeContainerPool() {
        System.out.println("Initializing container pool...");
        String hostTempDir = System.getProperty("java.io.tmpdir");

        languageConfig.getSupportedLanguages().forEach((lang, details) -> {
            try {
                System.out.println("Pulling image for " + lang + ": " + details.getImage());
                dockerClient.pullImageCmd(details.getImage()).start().awaitCompletion();

                HostConfig hostConfig = new HostConfig()
                        .withAutoRemove(true)
                        .withMemory(128L * 1024 * 1024)
                        .withCpuPeriod(100000L)
                        .withCpuQuota(50000L)
                        .withBinds(new Bind(hostTempDir, new Volume("/tmp")));

                CreateContainerResponse container = dockerClient.createContainerCmd(details.getImage())
                        .withHostConfig(hostConfig)
                        .withTty(true)
                        .withCmd("/bin/sh")
                        .exec();

                dockerClient.startContainerCmd(container.getId()).exec();
                containerPool.put(lang, container.getId());
                System.out.println("Started container for " + lang + " with ID: " + container.getId());
            } catch (Exception e) {
                System.err.println("Failed to initialize container for " + lang);
                e.printStackTrace();
            }
        });
    }

    public String getContainerId(String language) {
        return containerPool.get(language);
    }

    @PreDestroy
    private void cleanup() {
        System.out.println("Stopping all containers...");
        containerPool.values().forEach(containerId -> {
            try {
                System.out.println("Stopping container: " + containerId);
                dockerClient.stopContainerCmd(containerId).exec();
            } catch (Exception e) {
                System.err.println("Could not stop container " + containerId + ". It might have been auto-removed.");
            }
        });
    }
}