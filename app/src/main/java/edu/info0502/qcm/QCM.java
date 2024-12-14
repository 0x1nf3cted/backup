package edu.info0502.qcm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QCM implements Serializable {

    private static final long serialVersionUID = 1L;
    private String id;
    private String title;
    private String description;
    private List<Question> questions;
    private int timeLimit; // en minutes
    private Date creationDate;

    public QCM() {
        this.questions = new ArrayList<>();
        this.creationDate = new Date();
    }

    @JsonCreator
    public QCM(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("timeLimit") int timeLimit
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.timeLimit = timeLimit;
        this.questions = new ArrayList<>();
        this.creationDate = new Date();
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public int getTimeLimit() {
        return timeLimit;
    }
 

    public Date getCreationDate() {
        return creationDate;
    }

    public void addQuestion(Question question) {
        questions.add(question);
    }

    public void removeQuestion(Question question) {
        questions.remove(question);
    }

    public int getTotalPoints() {
        return questions.stream()
                .mapToInt(Question::getPoints)
                .sum();
    }

    @Override
    public String toString() {
        return String.format("QCM[id=%s, title=%s, questions=%d]",
                id, title, questions.size());
    }
}

// Question.java
// QCMResponse.java
class QCMResponse {

    private String userId;
    private String qcmId;
    private Map<String, Integer> answers;
    private Date submissionTime;
    private int score;

    public QCMResponse(String userId, String qcmId, Map<String, Integer> answers) {
        this.userId = userId;
        this.qcmId = qcmId;
        this.answers = answers;
        this.submissionTime = new Date();
    }

    // Getters et Setters
    public String getUserId() {
        return userId;
    }

    public String getQcmId() {
        return qcmId;
    }

    public Map<String, Integer> getAnswers() {
        return answers;
    }

    public Date getSubmissionTime() {
        return submissionTime;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}

// QCMManager.java
class QCMManager {

    private Map<String, QCM> qcms;
    private Map<String, List<QCMResponse>> responses;

    public QCMManager() {
        this.qcms = new HashMap<>();
        this.responses = new HashMap<>();
        initializeDefaultQCMs();
    }

    private void initializeDefaultQCMs() {
        // Création d'un QCM exemple
        QCM qcm = new QCM("QCM1", "Introduction à Java",
                "QCM sur les bases de Java", 30);

        Question q1 = new Question(
                "Q1",
                "Quelle est la différence entre '==' et 'equals()' en Java?",
                Arrays.asList(
                        "Ils sont identiques",
                        "'==' compare les références, 'equals()' compare le contenu",
                        "'==' compare le contenu, 'equals()' compare les références",
                        "Aucune différence"
                ),
                1, // Index de la bonne réponse (commence à 0)
                2, // Points
                "En Java, '==' compare les références mémoire tandis que 'equals()' compare le contenu des objets."
        );

        Question q2 = new Question(
                "Q2",
                "Qu'est-ce qu'une classe abstraite en Java?",
                Arrays.asList(
                        "Une classe qui ne peut pas être instanciée",
                        "Une classe qui n'a pas de méthodes",
                        "Une classe qui ne peut pas être héritée",
                        "Une classe qui n'a que des attributs statiques"
                ),
                0,
                2,
                "Une classe abstraite est une classe qui ne peut pas être instanciée directement et peut contenir des méthodes abstraites."
        );

        qcm.addQuestion(q1);
        qcm.addQuestion(q2);
        qcms.put(qcm.getId(), qcm);
    }

    public QCM getQCM(String qcmId) {
        return qcms.get(qcmId);
    }

    public void addQCM(QCM qcm) {
        qcms.put(qcm.getId(), qcm);
    }

    public void submitResponse(QCMResponse response) {
        responses.computeIfAbsent(response.getUserId(), k -> new ArrayList<>())
                .add(response);
        calculateScore(response);
    }

    private void calculateScore(QCMResponse response) {
        QCM qcm = qcms.get(response.getQcmId());
        if (qcm == null) {
            return;
        }

        int totalScore = 0;
        Map<String, Integer> answers = response.getAnswers();

        for (Question question : qcm.getQuestions()) {
            Integer userAnswer = answers.get(question.getId());
            if (userAnswer != null && question.isCorrect(userAnswer)) {
                totalScore += question.getPoints();
            }
        }

        response.setScore(totalScore);
    }

    public List<QCMResponse> getUserResponses(String userId) {
        return responses.getOrDefault(userId, new ArrayList<>());
    }

    public Map<String, Integer> getUserScores(String userId) {
        Map<String, Integer> scores = new HashMap<>();
        List<QCMResponse> userResponses = responses.getOrDefault(userId, new ArrayList<>());

        for (QCMResponse response : userResponses) {
            scores.put(response.getQcmId(), response.getScore());
        }

        return scores;
    }

    // Convertir un QCM en JSON pour l'envoi
    public String qcmToJson(QCM qcm) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(qcm);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public QCM jsonToQcm(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, QCM.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
