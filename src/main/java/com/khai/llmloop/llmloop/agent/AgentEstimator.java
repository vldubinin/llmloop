package com.khai.llmloop.llmloop.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.khai.llmloop.llmloop.entity.Context;
import com.khai.llmloop.llmloop.entity.Estimation;
import com.khai.llmloop.llmloop.util.JsonUtil;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.List;

@AllArgsConstructor
public class AgentEstimator {

    private VertexAI vertexAI;
    private String modelName;
    private String estimationCriteria;


    public <DateType extends Context> List<Estimation> estimate(List<DateType> dataForEstimation) throws IOException {
        String systemPrompt = estimationCriteria +
                "\n" +
                "You **must** use these criteria for your analysis, but do **not** show them in the final output. Each criterion is internally rated on a strict 1-5 scale:\n" +
                "* **5 = Flawless.** Meets the criterion perfectly.\n" +
                "* **4 = Good.** Has a minor, trivial issue.\n" +
                "* **3 = Passable.** Has clear flaws but gets the main idea across.\n" +
                "* **2 = Poor.** Needs major revision; fails the criterion in a significant way.\n" +
                "* **1 = Unusable.** Completely incorrect or missing.\n" +
                "\n" +
                "### Output Field Calculation\n" +
                "\n" +
                "1.  `id`: Must **exactly** match the `id` from the input object.\n" +
                "2.  `score`: **(int)**. Calculated as the arithmetic mean of your 8 internal ratings (from 1 to 5), rounded to the nearest whole number.\n" +
                "    * **PUNITIVE RULE:** If **any** single criterion is rated **1 or 2**, the final `score` **cannot be higher than 3**, regardless of the average.\n" +
                "3.  `recommendationForImprovement`: **(String)**. One, single, most high-priority and actionable piece of advice to improve the data, based on the *most severe flaw* (the criterion with the lowest score).\n" +
                "    * If the final score is 4, you *must* still point out the minor flaw that prevented a 5.\n" +
                "    * If the final score is 5, and only then, use: \"None. Meets all criteria.\"" +
                "\n" +
                "### Output Format\n" +
                "\n" +
                "You must provide your response **exclusively in the format of a JSON array**.\n" +
                "\n" +
                "**Example Output Format:**\n" +
                "```json\n" +
                "[\n" +
                "  {\n" +
                "    \"id\": 2,\n" +
                "    \"score\": 5,\n" +
                "    \"recommendationForImprovement\": \"None. Excellent.\"\n" +
                "  }\n" +
                "]";
        Content systemInstruction = ContentMaker.fromMultiModalData(systemPrompt);
        GenerativeModel model = new GenerativeModel(modelName, vertexAI).withSystemInstruction(systemInstruction);

        String userPrompt = JsonUtil.toJson(dataForEstimation);
        GenerateContentResponse response = model.generateContent(userPrompt);

        String result = ResponseHandler.getText(response).replace("```json", "");
        return JsonUtil.toObject(result, new TypeReference<List<Estimation>>() {
        });
    }
}
