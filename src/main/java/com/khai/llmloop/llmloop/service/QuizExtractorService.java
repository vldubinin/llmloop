package com.khai.llmloop.llmloop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.khai.llmloop.llmloop.entity.QuestionAndAnswer;
import com.khai.llmloop.llmloop.entity.Quiz;
import com.khai.llmloop.llmloop.util.DataUtil;
import com.khai.llmloop.llmloop.util.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class QuizExtractorService {

    @Value("${gemini.project.id}")
    private String projectId;

    @Value("${gemini.location}")
    private String location;

    @Value("${gemini.model}")
    private String modelName;

    public List<Quiz> extract(List<QuestionAndAnswer> questionAndAnswers, String fullContext, String systemPrompt) throws IOException {
        try (VertexAI vertexAI = new VertexAI(this.projectId, this.location)) {
            return extract(questionAndAnswers, fullContext, systemPrompt, vertexAI);
        }
    }

    private List<Quiz> extract(List<QuestionAndAnswer> questionAndAnswers, String fullContext, String systemPrompt, VertexAI vertexAI) throws IOException {
        Content systemInstruction = ContentMaker.fromMultiModalData(systemPrompt);
        GenerativeModel model = new GenerativeModel(modelName, vertexAI).withSystemInstruction(systemInstruction);

        String userPrompt = "qaList: " + JsonUtil.toJson(questionAndAnswers) + "\n" + "fullSourceText: " + fullContext;

        GenerateContentResponse response = model.generateContent(userPrompt);
        String result = ResponseHandler.getText(response);
        List<Quiz> quizzes = JsonUtil.toObject(result, new TypeReference<List<Quiz>>() {});
        return DataUtil.initIds(quizzes);
    }

}
