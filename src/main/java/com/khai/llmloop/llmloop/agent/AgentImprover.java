package com.khai.llmloop.llmloop.agent;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import lombok.AllArgsConstructor;

import java.io.IOException;

@AllArgsConstructor
public class AgentImprover {
    private VertexAI vertexAI;
    private String modelName;

    public String improve(String dataDescription, String recommendationForImprovement, String dataToImprove, String responseFormat) throws IOException {
        String systemPrompt =
                "You are an expert content refiner and editor. Your task is to rewrite and improve a piece of content (`improveData`) based on a specific recommendation.\n" +
                "\n" +
                "Follow these steps precisely:\n" +
                "1.  First, use the `dataDescription` to understand the full context, purpose, and type of content you are editing.\n" +
                "2.  Next, use the `recommendationForImprovement` as a strict set of instructions. You must address all points in this recommendation.\n" +
                "3.  Finally, rewrite the original `improveData` to apply these improvements.\n" +
                "\n" +
                "Your response must **only** contain the new, improved content.\n" +
                "Do not add any preamble, explanation, or conversational text (e.g., \"Here is the corrected content:\").\n" +
                "The response must strictly adhere to this format: " + responseFormat;

        String userPrompt =
                "**DataDescription:**\n" +
                dataDescription +"\n" +
                "\n" +
                "**RecommendationForImprovement:**\n" +
                recommendationForImprovement + "\n" +
                "\n" +
                "**DataToImprove:**\n" +
                dataToImprove;

        Content systemInstruction = ContentMaker.fromMultiModalData(systemPrompt);
        GenerativeModel model = new GenerativeModel(modelName, vertexAI).withSystemInstruction(systemInstruction);
        GenerateContentResponse response = model.generateContent(userPrompt);
        return ResponseHandler.getText(response);
    }
}
