package com.khai.llmloop.llmloop.entity;

import lombok.Data;

import java.util.List;

@Data
public class Report {
    List<String> concepts;
    List<String> improvedEducationConcepts;
    List<String> improvedKnowledgeConcepts;
    List<QuestionAndAnswer> questionAndAnswers;
    List<QuestionAndAnswer> improvedQuestionAndAnswers;
    List<Quiz> quizzes;
    List<Quiz> improvedQuizzes;
}
