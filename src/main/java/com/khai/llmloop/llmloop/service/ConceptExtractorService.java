package com.khai.llmloop.llmloop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.khai.llmloop.llmloop.entity.Concept;
import com.khai.llmloop.llmloop.util.DataUtil;
import com.khai.llmloop.llmloop.util.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ConceptExtractorService {

    @Value("${gemini.project.id}")
    private String projectId;

    @Value("${gemini.location}")
    private String location;

    @Value("${gemini.model}")
    private String modelName;

    public List<Concept> extract(String systemPrompt, String context) throws IOException {
        try (VertexAI vertexAI = new VertexAI(this.projectId, this.location)) {
            return extract(context, systemPrompt, vertexAI);
        }
    }

    private List<Concept> extract(String context, String systemPrompt, VertexAI vertexAI) throws IOException {
        Content systemInstruction = ContentMaker.fromMultiModalData(systemPrompt);
        GenerativeModel model = new GenerativeModel(modelName, vertexAI).withSystemInstruction(systemInstruction);

        GenerateContentResponse response = model.generateContent(context);
        String result = ResponseHandler.getText(response);
        List<Concept> conceptList =  JsonUtil.toObject(result, new TypeReference<List<Concept>>() {});
        return DataUtil.initIds(conceptList);
    }
}
