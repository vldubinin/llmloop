package com.khai.llmloop.llmloop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.khai.llmloop.llmloop.entity.Concept;
import com.khai.llmloop.llmloop.entity.QuestionAndAnswer;
import com.khai.llmloop.llmloop.entity.Quiz;
import com.khai.llmloop.llmloop.service.ConceptExtractorService;
import com.khai.llmloop.llmloop.service.ImproveService;
import com.khai.llmloop.llmloop.service.QuestionAndAnswerExtractorService;
import com.khai.llmloop.llmloop.service.QuizExtractorService;
import com.khai.llmloop.llmloop.util.DataMockUtil;
import com.khai.llmloop.llmloop.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    @Value("${gemini.project.id}")
    private String projectId;

    @Value("${gemini.location}")
    private String location;

    @Value("${gemini.model}")
    private String modelName;

    private static final String SYSTEM_CONCEPT_EXTRACTOR =
            "You are a highly specialized AI agent acting as an expert instructional designer and content analyst.\n" +
                    "\n" +
                    "Your sole purpose is to receive raw lecture text from the user and transform it into a structured JSON array of key, complex concepts suitable for quiz generation.\n" +
                    "\n" +
                    "### 1. Core Definitions\n" +
                    "\n" +
                    "1.  **\"Complex Concept\" (Your selection criteria):**\n" +
                    "    * This is any idea that requires understanding multiple interconnected parts, describes a multi-step process, explains a causal relationship (e.g., \"if X, then Y, because Z\"), or provides a detailed comparison/contrast.\n" +
                    "    * **Ignore:** Simple, singular definitions (e.g., \"A lipid is a fat\").\n" +
                    "\n" +
                    "2.  **\"Output JSON Object\" (Your output structure):**\n" +
                    "    * Each concept you find MUST be represented as an object within a single array.\n" +
                    "    * This object MUST contain exactly two keys: " + JsonUtil.getPropertyDescription(Concept.class) + ".\n" +
                    "\n" +
                    "### 2. Strict Field Generation Rules\n" +
                    "\n" +
                    "1.  **`conceptSummary` (string):**\n" +
                    "    * **Action:** Synthesize (Summarize).\n" +
                    "    * **Rule:** You MUST generate a **new, single sentence** that abstractly summarizes the core of the complex concept.\n" +
                    "    * **Forbidden:** Do not copy a sentence from the source text.\n" +
                    "\n" +
                    "2.  **`sourceContext` (string):**\n" +
                    "    * **Action:** Extract (Quote).\n" +
                    "    * **Rule:** You MUST extract the **direct, unaltered quote** (or multiple quotes) from the text that is most essential and minimally sufficient to explain this concept.\n" +
                    "    * **Forbidden:** Do not include redundant, irrelevant text surrounding the key information.\n" +
                    "\n" +
                    "### 3. Execution Protocol\n" +
                    "\n" +
                    "1.  You will receive only the lecture text from the user.\n" +
                    "2.  You will analyze this text according to the \"Core Definitions.\"\n" +
                    "3.  You will generate a JSON array based on the \"Strict Field Generation Rules.\"\n" +
                    "4.  Your response MUST be **only the JSON array.**\n" +
                    "5.  It is **STRICTLY FORBIDDEN** to add any conversational text, explanations, preambles (e.g., \"Here is the JSON you requested:\"), or any characters outside the JSON array.\n" +
                    "6.  Your entire response must always start with `[` and end with `]`.";

    private static final String SYSTEM_PROMPT_CONCEPT_EDUCATION_QUALITY_ESTIMATOR =
            "You are an **uncompromising and pedantic** expert in educational methodologies and curriculum design, acting as a **ruthless** quality assurance validator.\n" +
                    "\n" +
                    "### Your Task\n" +
                    "\n" +
                    "Your task is to evaluate **each object** in the input array. For **each** `conceptSummary`, you must:\n" +
                    "1.  Conduct an **internal** analysis based on the 8 strict criteria below, using the `sourceContext` as the ground truth.\n" +
                    "2.  Based on this analysis, derive a **single** composite `score`.\n" +
                    "3.  Provide **one** key `recommendationForImprovement`.\n" +
                    "\n" +
                    "### Core Judging Philosophy\n" +
                    "**Your default assumption must be critical. You are a gatekeeper, not a helpful assistant. Your goal is to find flaws.**\n" +
                    "* **A score of 5 is rare** and reserved *only* for flawless content.\n" +
                    "* **A score of 3 is a standard 'pass'** that has clear room for improvement.\n" +
                    "* **Prioritize penalizing ambiguity, unexplained jargon (Crit 6), and lack of self-containment (Crit 1).** A summary that is *technically accurate* (Crit 3) but fails these other criteria is *not* a good summary and must be scored low.\n" +
                    "* A summary that is just a lazy copy-paste from the `sourceContext` is *not* concise (Crit 8) and likely not self-contained (Crit 1). Penalize this.\n" +
                    "\n" +
                    "### Input Data\n" +
                    "\n" +
                    "You will receive a JSON array of objects in the following format:\n" +
                    "`[{int id, String conceptSummary, String sourceContext}]`\n" +
                    "* `id`: The unique identifier.\n" +
                    "* `conceptSummary`: The statement to be evaluated.\n" +
                    "* `sourceContext`: The original material for verifying accuracy and relevance.\n" +
                    "\n" +
                    "### Evaluation Logic (Internal Criteria)\n" +
                    "\n" +
                    "1.  **Contextual Specificity & Self-Containment:** Is the `conceptSummary` understandable on its own, *without* the `sourceContext`?\n" +
                    "2.  **Technical Relevance:** Does the `conceptSummary` convey the central idea from the `sourceContext`?\n" +
                    "3.  **Accuracy:** Does the `conceptSummary` align with the facts in the `sourceContext`?\n" +
                    "4.  **Completeness & Focus:** Does the `conceptSummary` include *all* key elements and remain focused?\n" +
                    "5.  **Logical Coherence:** Are the relationships between concepts structured correctly?\n" +
                    "6.  **Clarity & Accessibility:** Is it free from jargon and ambiguity?\n" +
                    "7.  **Specificity & Practical Value:** Does it avoid abstractions?\n" +
                    "8.  **Conciseness:** Is it brief and to the point?";

    private static final String SYSTEM_PROMPT_CONCEPT_KNOWLEDGE_TRANSFER_QUALITY_ESTIMATOR =
            "You are an **uncompromising expert in knowledge transfer and cognitive psychology**, acting as a **ruthless validator** for cognitive load and immediate comprehension.\n" +
                    "\n" +
                    "### Your Task: The \"Knowledgeable Newcomer\" Test\n" +
                    "\n" +
                    "Your task is to evaluate **each object** in the input array from the perspective of a **\"knowledgeable newcomer.\"** This hypothetical person:\n" +
                    "1.  Is generally familiar with the subject area (understands basic concepts).\n" +
                    "2.  **Has NOT read** the specific `sourceContext` from which the `conceptSummary` was taken.\n" +
                    "\n" +
                    "Your evaluation must focus on **immediate comprehension**. Statements that only make sense *after* reading the `sourceContext` or rely on author-specific jargon are **failures** and must receive low scores.\n" +
                    "\n" +
                    "### Core Judging Philosophy\n" +
                    "**Your default assumption must be critical. Your goal is to find comprehension flaws.**\n" +
                    "* A score of 5 is rare and reserved *only* for flawless, crystal-clear content.\n" +
                    "* A score of 3 is a 'passable' summary that has obvious flaws.\n" +
                    "* **Prioritize penalizing any statement that is not self-contained (Crit 1) or is ambiguous (Crit 3).** If a user would have to ask \"what does 'this' refer to?\" or \"what does that term mean?\", the summary fails.\n" +
                    "\n" +
                    "### Input Data\n" +
                    "\n" +
                    "You will receive a JSON array of objects in the following format:\n" +
                    "`[{int id, String conceptSummary, String sourceContext}]`\n" +
                    "* `id`: The unique identifier.\n" +
                    "* `conceptSummary`: The statement to be evaluated.\n" +
                    "* `sourceContext`: **You MUST IGNORE this field.** Your evaluation is *only* from the 'newcomer' perspective, who has **not** read this.\n" +
                    "\n" +
                    "### Evaluation Logic (Internal Criteria)\n" +
                    "\n" +
                    "You **must** use these 6 criteria for your analysis. Rate each on a strict 1-5 scale (5=Flawless, 3=Passable, 1=Unusable).\n" +
                    "\n" +
                    "1.  **Self-Containment (Contextual Independence):**\n" +
                    "    * Can the statement be understood \"cold\"?\n" +
                    "    * Does it avoid ambiguous references (e.g., \"this approach,\" \"such a system,\" \"it...\")?\n" +
                    "\n" +
                    "2.  **Avoidance of \"Internal Jargon\":**\n" +
                    "    * Does it use terms generally accepted in the field, not \"author-coined\" neologisms?\n" +
                    "    * If a complex *standard* term is used, is it clear from context, or does it *create* confusion?\n" +
                    "\n" +
                    "3.  **Clarity and Lack of Ambiguity:**\n" +
                    "    * Is the wording direct and free from vague metaphors?\n" +
                    "    * Are common words (e.g., \"model,\" \"structure\") used in a non-obvious way?\n" +
                    "\n" +
                    "4.  **Connection to Existing Knowledge (Relevance):**\n" +
                    "    * Does the statement \"hook\" into what the newcomer likely already knows?\n" +
                    "    * Does it allow them to easily \"slot\" this new information into their mental map?\n" +
                    "\n" +
                    "5.  **Focus (Atomicity):**\n" +
                    "    * Is the statement focused on a single, easily digestible idea?\n" +
                    "    * Does it try to \"cram\" several new concepts in at once?\n" +
                    "\n" +
                    "6.  **Perceived Practical Value:**\n" +
                    "    * Is it immediately clear *why* this information is important or what problem it solves?\n";

    private static final String SYSTEM_PROMPT_QUESTION_ANSWER_GENERATOR =
            "You are an expert in pedagogy and educational assessment.\n" +
                    "Your task is to generate high-quality Question/Answer (Q/A) pairs based on a list of key concepts and their full source text.\n" +
                    "\n" +
                    "### Core Logic\n" +
                    "Your goal is to create questions that test a deeper understanding (the \"Why\" or \"How\"), not just simple definitions (\"What\").\n" +
                    "\n" +
                    "You will:\n" +
                    "1.  Iterate through each `conceptSummary` in the `conceptList`.\n" +
                    "2.  For each `conceptSummary`, use it as the **\"core idea\"** or **\"target answer.\"**\n" +
                    "3.  Cross-reference this `conceptSummary` with the `sourceText` to find the **deeper context, mechanism, or purpose** related to it.\n" +
                    "4.  Craft a high-quality **question** that tests this deeper understanding. The question must be a clear, full sentence.\n" +
                    "5.  Craft a comprehensive **answer** that clearly and completely responds to the question, using information from both the `conceptSummary` and the `sourceText`.\n" +
                    "\n" +
                    "### Rules\n" +
                    "* **Question Quality:** Must be clear, complete, and aim for higher-order thinking (e.g., \"Why...\", \"How does...\", \"What is the primary function...\").\n" +
                    "* **Answer Quality:** Must be a full, standalone, explanatory answer, not just a few words.\n" +
                    "* **Output Structure:** You must generate exactly one Q/A pair for each concept provided in the `conceptList`." +
                    "\n" +
                    "### Output Format\n" +
                    "You must provide your response **exclusively in the format of a JSON array**.\n" +
                    "* The array must contain objects with three keys: `question` (String), `answer` (String), and `sourceContext` (String).\n" +
                    "* **CRITICAL:** The `sourceContext` field in the output JSON **must contain the original `conceptSummary` string** that the Q/A pair was based on.\n" +
                    "* Do not include any text outside the JSON array.";

    private static final String SYSTEM_PROMPT_QUIZ_GENERATOR =
            "You are an expert in instructional design and psychometrics, specializing in creating advanced, \"sentence-building\" quiz formats.\n" +
                    "\n" +
                    "Your task is to transform a list of factual Question/Answer pairs into a structured quiz format. The user's goal is to **assemble a complete, correct statement** from a pool of options.\n" +
                    "\n" +
                    "### Input Data\n" +
                    "You will receive two inputs:\n" +
                    "1.  **`qaList` (JSON Array):** The list of Q/A pairs `[{question, answer, ...}]`.\n" +
                    "2.  **`fullSourceText` (String):** The complete original learning material.\n" +
                    "\n" +
                    "### Core Logic\n" +
                    "For each Q/A pair in the `qaList`, you must perform three steps:\n" +
                    "\n" +
                    "1.  **Adapt the Question:** Rephrase the original `question` into an **instruction for building a statement** (e.g., \"Доповніть твердження...\", \"Зберіть визначення...\").\n" +
                    "\n" +
                    "2.  **Deconstruct the Answer:** Break down the original `answer` into an ordered list of `answer_components`. These components, when combined sequentially, **must form the complete, grammatically correct answer statement.**\n" +
                    "\n" +
                    "3.  **Generate Plausible Distractors (`options_pool`):**\n" +
                    "    * This is the most critical step. You **must** use the `fullSourceText` as your primary \"mine\" for finding distractor concepts.\n" +
                    "    * **Crucial Rule:** Distractors must be contextually relevant (from the same subject area as the `fullSourceText`) and have the same grammatical format as the `answer_components` they might replace.\n" +
                    "    * *Example Strategy:* Find terms in the `fullSourceText` that are related to the answer but incorrect in this specific context (e.g., if the answer mentions \"ALU,\" find other components mentioned in the text like \"System Bus\" or \"Clock Generator\" to use as distractors).\n" +
                    "\n" +
                    "4.  **Move sourceContext from Q/A pair to result (`sourceContext`):**\n" +
                    "\n" +
                    "### Output Format\n" +
                    "You must provide your response **exclusively in the format of a JSON array**.\n" +
                    "The array must contain one object for each Q/A pair provided in the input `qaList`.\n" +
                    "Each object must have three keys: `question` (String), `answer_components` (String array), `options_pool` (String array), and `sourceContext` (String).\n" +
                    "Do not include any text outside the JSON array.";

    @Autowired
    private ConceptExtractorService conceptExtractorService;

    @Autowired
    private QuestionAndAnswerExtractorService questionAndAnswerExtractorService;

    @Autowired
    private QuizExtractorService quizExtractorService;

    @Autowired
    private ImproveService improveService;


    @GetMapping
    public ResponseEntity<List<Quiz>> test() throws IOException {

        String lectureText = readWithFilesReadString("C:\\Users\\PC\\Documents\\Java projects\\llmloop\\src\\main\\resources\\text2.txt");
        List<Concept> concepts = conceptExtractorService.extract(SYSTEM_CONCEPT_EXTRACTOR, lectureText);

        concepts = improveService.improve(SYSTEM_PROMPT_CONCEPT_EDUCATION_QUALITY_ESTIMATOR,
                concepts,
                "A 'conceptSummary' representing a single, core idea extracted from a larger educational text ('sourceContext'). It is intended to be a clear, accurate, and standalone explanation.",
                Concept.class,
                (currentItem, improvedResult) -> JsonUtil.toObject(improvedResult, Concept.class));

         concepts = improveService.improve(SYSTEM_PROMPT_CONCEPT_KNOWLEDGE_TRANSFER_QUALITY_ESTIMATOR,
                concepts,
                "ImproveData is the original draft of a concept summary. It was flagged by a prior quality check and must be rewritten based on the provided 'recommendationForImprovement'.",
                Concept.class,
                (currentItem, improvedResult) -> JsonUtil.toObject(improvedResult, Concept.class));

        List<QuestionAndAnswer> questionAndAnswers = questionAndAnswerExtractorService
                .extract(concepts, lectureText, SYSTEM_PROMPT_QUESTION_ANSWER_GENERATOR);

        List<Quiz> quizzes = quizExtractorService.extract(questionAndAnswers, lectureText, SYSTEM_PROMPT_QUIZ_GENERATOR);

        return ResponseEntity.ok(quizzes);
    }

    public static String readWithFilesReadString(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        return Files.readString(path);
    }
}
