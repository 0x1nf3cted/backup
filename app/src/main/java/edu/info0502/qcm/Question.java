package edu.info0502.qcm;

import java.util.ArrayList;
import java.util.List;

public class Question {

    private String id;
    private String text;
    private List<String> options;
    private int correctAnswer;
    private int points;
    private String explanation;

    // Add this default constructor
    public Question() {
        this.options = new ArrayList<>();
    }

    // Your existing constructor
    public Question(String id, String text, List<String> options,
            int correctAnswer, int points, String explanation) {
        this.id = id;
        this.text = text;
        this.options = new ArrayList<>(options);
        this.correctAnswer = correctAnswer;
        this.points = points;
        this.explanation = explanation;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public void setCorrectAnswer(int correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectAnswer() {
        return correctAnswer;
    }

    public int getPoints() {
        return points;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getQuestionText() {
        return text;
    }

    public boolean isCorrect(int answer) {
        return answer == correctAnswer;
    }

    @Override
    public String toString() {
        return String.format("Question[id=%s, text=%s]", id, text);
    }
}
