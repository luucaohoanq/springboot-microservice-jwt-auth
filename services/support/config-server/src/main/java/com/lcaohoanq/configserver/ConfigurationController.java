package com.lcaohoanq.configserver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ConfigurationController {

    @Value("${spring.cloud.config.server.native.searchLocations:classpath:/configurations/}")
    private String configurationsPath;

    @GetMapping("/applications")
    public ResponseEntity<List<String>> getApplications() {
        try {
            Resource resource = new ClassPathResource("configurations");
            File folder = resource.getFile();

            List<String> apps = Arrays.stream(Objects.requireNonNull(folder.listFiles()))
                .filter(File::isFile)
                .filter(f -> f.getName().endsWith(".yml") || f.getName().endsWith(".yaml"))
                .map(f -> f.getName().replaceAll("\\.(yml|yaml)$", ""))
                .filter(name -> !name.equals("application")) // Exclude base application.yml
                .sorted()
                .collect(Collectors.toList());

            return ResponseEntity.ok(apps);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }
}
