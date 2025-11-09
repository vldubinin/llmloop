package com.khai.llmloop.llmloop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.khai.llmloop.llmloop.entity.Concept;
import com.khai.llmloop.llmloop.entity.QuestionAndAnswer;
import com.khai.llmloop.llmloop.util.DataUtil;
import com.khai.llmloop.llmloop.util.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class QuestionAndAnswerExtractorService {

    @Value("${gemini.project.id}")
    private String projectId;

    @Value("${gemini.location}")
    private String location;

    @Value("${gemini.model}")
    private String modelName;

    public List<QuestionAndAnswer> extract(List<Concept> targetConcept, String fullContext, String systemPrompt) throws IOException {
        try (VertexAI vertexAI = new VertexAI(this.projectId, this.location)) {
            DataUtil.cleanContexts(targetConcept);
            return extract(targetConcept, fullContext, systemPrompt, vertexAI);
        }
    }

    private List<QuestionAndAnswer> extract(List<Concept> targetConcept, String fullContext, String systemPrompt, VertexAI vertexAI) throws IOException {
        Content systemInstruction = ContentMaker.fromMultiModalData(systemPrompt);
        GenerativeModel model = new GenerativeModel(modelName, vertexAI).withSystemInstruction(systemInstruction);

        String userPrompt =
                "Here is the data for Q/A generation:\n" +
                        "\n" +
                        "### conceptList (JSON Array)" +
                        "\n" +
                        JsonUtil.toJson(targetConcept) +
                        "\n" +
                        "### sourceText (Full Learning Material)" +
                        "\n" +
                        fullContext;
        GenerateContentResponse response = model.generateContent(userPrompt);
        String result = ResponseHandler.getText(response);
        List<QuestionAndAnswer> questionAndAnswers = JsonUtil.toObject(result, new TypeReference<List<QuestionAndAnswer>>() {
        });
        return DataUtil.initIds(questionAndAnswers);
    }

}
