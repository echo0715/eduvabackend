package com.eduva.eduva.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/file")
@CrossOrigin(
        origins = {"http://localhost:3000"},
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowCredentials = "true"
)

public class FileProcessController {
    @PostMapping("/problemToStandard")
    public List<String> problemToStandard(@RequestParam("rubric") List<MultipartFile> rubricFiles,
                                          @RequestParam("problemSet") List<MultipartFile> problemSetFiles) {

        // Debugging uploaded files
        long startTime = System.currentTimeMillis();
        rubricFiles.forEach(file -> System.out.println("Rubric file: " + file.getOriginalFilename()));
        problemSetFiles.forEach(file -> System.out.println("Problem Set file: " + file.getOriginalFilename()));
        long endTime = System.currentTimeMillis();
        System.out.println("Execution time: " + (endTime - startTime) + "ms");
        List<String> dummyStrings = List.of(
                "DummyString1",
                "DummyString2",
                "DummyString3",
                "DummyString4",
                "DummyString5"
        );


        return dummyStrings;
    }

    @PostMapping("/testing")
    public String testing(@RequestBody String input) {
        // Process the input and return a response
        return "Received: " + input;
    }
}
