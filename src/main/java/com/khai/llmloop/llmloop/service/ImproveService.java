package com.khai.llmloop.llmloop.service;

import com.google.cloud.vertexai.VertexAI;
import com.khai.llmloop.llmloop.agent.AgentEstimator;
import com.khai.llmloop.llmloop.agent.AgentImprover;
import com.khai.llmloop.llmloop.entity.Context;
import com.khai.llmloop.llmloop.entity.Estimation;
import com.khai.llmloop.llmloop.function.ImproveProcessor;
import com.khai.llmloop.llmloop.util.DataUtil;
import com.khai.llmloop.llmloop.util.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImproveService {
    private static final int MAX_TRY_NUMBER = 1;
    private static final int MIN_SCORE_FOR_ACCEPT = 5;
    private static final int MIN_SCORE_FOR_IMMPROVE = 3;

    @Value("${gemini.project.id}")
    private String projectId;

    @Value("${gemini.location}")
    private String location;

    @Value("${gemini.model}")
    private String modelName;

    public <DataType extends Context> List<DataType> improve(String estimationPrompt, List<DataType> dataForImprove,
                                                             String dataDescription, Class<DataType> clazz,
                                                             ImproveProcessor improveProcessor) throws IOException {
        try (VertexAI vertexAI = new VertexAI(this.projectId, this.location)) {
            AgentEstimator agentEstimator = new AgentEstimator(vertexAI, modelName, estimationPrompt);
            AgentImprover agentImprover = new AgentImprover(vertexAI, modelName);
            return improve(clazz, dataForImprove, dataDescription, improveProcessor, MAX_TRY_NUMBER, agentImprover, agentEstimator);
        }
    }

    private <DataType extends Context> List<DataType> improve(Class<DataType> clazz,
                                                              List<DataType> dataForImprove,
                                                              String dataDescription,
                                                              ImproveProcessor improveProcessor,
                                                              int tryNumber,
                                                              AgentImprover agentImprover,
                                                              AgentEstimator agentEstimator) throws IOException {
        List<DataType> finalData = new ArrayList<>();

        List<Estimation> estimations = agentEstimator.estimate(dataForImprove);
        for (Estimation estimation : estimations) {
            DataType data = DataUtil.findById(dataForImprove, estimation.id());
            if (data == null) {
                throw new NullPointerException("Data with id " + estimation.id() + " not found");
            }

            if (estimation.score() >= MIN_SCORE_FOR_ACCEPT) {
                finalData.add(data);
            } else if (tryNumber > 0 && estimation.score() >= MIN_SCORE_FOR_IMMPROVE) {
                String improvedDataJson = agentImprover.improve(
                        dataDescription, estimation.recommendationForImprovement(), JsonUtil.toJson(data),
                        JsonUtil.getPropertyDescription(clazz));

                DataType improvedData = (DataType) improveProcessor.apply(data, improvedDataJson);
                improvedData.setSourceContext(data.getSourceContext());

                finalData.addAll(improve(clazz, List.of(improvedData),
                        dataDescription, improveProcessor, tryNumber - 1,
                        agentImprover, agentEstimator));
            }
        }
        return finalData;
    }
}
