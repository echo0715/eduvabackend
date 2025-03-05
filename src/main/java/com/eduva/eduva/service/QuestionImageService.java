package com.eduva.eduva.service;

import com.eduva.eduva.dto.ClaudeResponse.ClaudeQuestionInfo;
import com.eduva.eduva.dto.QuestionImageRequest.QuestionImageUpload;
import com.eduva.eduva.model.AssignmentData;
import com.eduva.eduva.model.QuestionImage;
import com.eduva.eduva.repository.AssignmentRepository;
import com.eduva.eduva.repository.QuestionImageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuestionImageService {
    @Autowired
    private QuestionImageRepository questionImageRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

//    public List<QuestionImage> saveQuestionImages(List<QuestionImageUpload> questionImagesUpload, Long assignmentId) throws JsonProcessingException {
//
//        List<QuestionImage> questionImages = new ArrayList<>();
//        for (QuestionImageUpload questionImageUpload : questionImagesUpload) {
//            QuestionImage questionImage = new QuestionImage();
//            questionImage.setAssignmentId(assignmentId);
//            questionImage.setQuestionId(questionImageUpload.getQuestionId());
//            questionImage.setImageKey(questionImageUpload.getKey());
//            questionImage.setUrl(questionImageUpload.getUrl());
//
//            questionImages.add(questionImage);
//        }
//        List<QuestionImage> savedQuestionImages = questionImageRepository.saveAll(questionImages);
//        Optional<AssignmentData> assignmentOptional = assignmentRepository.findById(assignmentId);
//        if (assignmentOptional.isPresent()) {
//            AssignmentData assignment = assignmentOptional.get();
//            assignment.setRubricFinished(true);
//
//            ObjectMapper objectMapper = new ObjectMapper();
//            String jsonRubricContent = assignment.getRubricContent();
//            List<ClaudeQuestionInfo> retrievedQuestions = objectMapper.readValue(jsonRubricContent,
//                    new TypeReference<List<ClaudeQuestionInfo>>() {});
//
//            Map<String, String> questionIdToImageKeyMap = new HashMap<>();
//            for (QuestionImage questionImage : savedQuestionImages) {
//                questionIdToImageKeyMap.put(questionImage.getQuestionId(), questionImage.getImageKey());
//            }
//
//            // Update each question's rubric with the corresponding image key
//            for (ClaudeQuestionInfo question : retrievedQuestions) {
//                String questionId = question.getQuestionId();
//                if (questionIdToImageKeyMap.containsKey(questionId)) {
//                    question.setRubric(questionIdToImageKeyMap.get(questionId));
//
//                }
//            }
//
//            // Serialize the updated questions back to JSON
//            String updatedRubricContent = objectMapper.writeValueAsString(retrievedQuestions);
//            assignment.setRubricContent(updatedRubricContent);
//
//            assignmentRepository.save(assignment);
//
//        }
//
//        return savedQuestionImages;
//    }

    public List<QuestionImage> saveQuestionImages(List<QuestionImageUpload> questionImagesUpload, Long assignmentId) throws JsonProcessingException {

        List<QuestionImage> questionImages = new ArrayList<>();
        for (QuestionImageUpload questionImageUpload : questionImagesUpload) {
            QuestionImage questionImage = new QuestionImage();
            questionImage.setAssignmentId(assignmentId);
            questionImage.setQuestionId(questionImageUpload.getQuestionId());
            questionImage.setImageKey(questionImageUpload.getKey());
            questionImage.setUrl(questionImageUpload.getUrl());

            questionImages.add(questionImage);
        }
        List<QuestionImage> savedQuestionImages = questionImageRepository.saveAll(questionImages);
        Optional<AssignmentData> assignmentOptional = assignmentRepository.findById(assignmentId);
        if (assignmentOptional.isPresent()) {
            AssignmentData assignment = assignmentOptional.get();
            assignment.setRubricFinished(true);

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonRubricContent = assignment.getRubricContent();
            List<ClaudeQuestionInfo> retrievedQuestions = objectMapper.readValue(jsonRubricContent,
                    new TypeReference<List<ClaudeQuestionInfo>>() {});

            // Create a map of question IDs to lists of image keys
            Map<String, List<String>> questionIdToImageKeysMap = new HashMap<>();
            for (QuestionImage questionImage : savedQuestionImages) {
                String questionId = questionImage.getQuestionId();
                String imageKey = questionImage.getImageKey();

                // If the question ID is not in the map, add a new list
                if (!questionIdToImageKeysMap.containsKey(questionId)) {
                    questionIdToImageKeysMap.put(questionId, new ArrayList<>());
                }

                // Add the image key to the list for this question ID
                questionIdToImageKeysMap.get(questionId).add(imageKey);
            }

            // Update each question's rubric with the corresponding image keys, joined by semicolons
            for (ClaudeQuestionInfo question : retrievedQuestions) {
                String questionId = question.getQuestionId();
                if (questionIdToImageKeysMap.containsKey(questionId)) {
                    List<String> imageKeys = questionIdToImageKeysMap.get(questionId);

                    // Get the existing rubric value (might be null)
                    String existingRubric = question.getRubric();

                    // Join all image keys with semicolons
                    String joinedImageKeys = String.join(";", imageKeys);

                    // If there's an existing rubric value, append the new keys
                    if (existingRubric != null && !existingRubric.isEmpty()) {
                        question.setRubric(existingRubric + ";" + joinedImageKeys);
                    } else {
                        question.setRubric(joinedImageKeys);
                    }
                }
            }

            // Serialize the updated questions back to JSON
            String updatedRubricContent = objectMapper.writeValueAsString(retrievedQuestions);
            assignment.setRubricContent(updatedRubricContent);

            assignmentRepository.save(assignment);
        }

        return savedQuestionImages;
    }
}
