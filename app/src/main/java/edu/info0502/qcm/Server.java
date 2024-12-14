package edu.info0502.qcm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

class NullKeySerializer extends StdSerializer<Object> {

    public NullKeySerializer() {
        this(null);
    }

    public NullKeySerializer(Class<Object> t) {
        super(t);
    }

    @Override
    public void serialize(Object nullKey, JsonGenerator jsonGenerator, SerializerProvider unused)
            throws IOException {
        jsonGenerator.writeString("null");
    }
}

public class Server {

    private static final String REGISTRATION_QUEUE = "registration_queue";
    private static final String LOGIN_QUEUE = "login_queue";
    private static final String QCM_LIST_QUEUE = "qcm_list_queue";
    private static final String QCM_QUEUE = "qcm_queue";
    private static final String ANSWER_QUEUE = "answer_queue";
    private static final String SCORE_QUEUE = "score_queue";
    private static final String USERS_FILE = "users.json";
    private static final String QCM_FILE = "qcm.json";
    private static Map<String, String> users = new HashMap<>();
    private static List<String> availableQCMs = new ArrayList<>();

    public static void main(String[] args) {
        try {
            loadUsers();
            loadQCMs();

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(5672);
            factory.setUsername("moussa");
            factory.setPassword("moussa");

            try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {

                channel.queueDeclare(REGISTRATION_QUEUE, false, false, false, null);
                channel.queueDeclare(LOGIN_QUEUE, false, false, false, null);
                channel.queueDeclare(QCM_LIST_QUEUE, false, false, false, null);
                channel.queueDeclare(QCM_QUEUE, false, false, false, null);
                channel.queueDeclare(ANSWER_QUEUE, false, false, false, null);
                channel.queueDeclare(SCORE_QUEUE, false, false, false, null);

                setUpConsumers(channel);

                System.out.println("Server is waiting for messages...");
                Thread.sleep(Long.MAX_VALUE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setUpConsumers(Channel channel) throws IOException {
        // Registration consumer
        channel.basicConsume(REGISTRATION_QUEUE, true, (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());
            ObjectMapper mapper = new ObjectMapper();
            mapper.getSerializerProvider().setNullKeySerializer(new NullKeySerializer());

            try {
                Map<String, String> registrationData = mapper.readValue(message, Map.class);
                String username = registrationData.get("username");
                String password = registrationData.get("password");

                if (username == null || username.trim().isEmpty()) {
                    String response = createJsonResponse("error", "Username cannot be null or empty");
                    channel.basicPublish("", REGISTRATION_QUEUE, null, response.getBytes());
                    return;
                }

                if (users.containsKey(username)) {
                    String response = createJsonResponse("error", "User already exists");
                    channel.basicPublish("", REGISTRATION_QUEUE, null, response.getBytes());
                } else {
                    users.put(username, password);
                    saveUsers();
                    String response = createJsonResponse("success", "Registration successful");
                    channel.basicPublish("", REGISTRATION_QUEUE, null, response.getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
                String response = createJsonResponse("error", "Registration failed: " + e.getMessage());
                try {
                    channel.basicPublish("", REGISTRATION_QUEUE, null, response.getBytes());
                } catch (IOException ioE) {
                    ioE.printStackTrace();
                }
            }
        }, consumerTag -> {
        });

        // Login consumer
        channel.basicConsume(LOGIN_QUEUE, true, (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());
            ObjectMapper mapper = new ObjectMapper();
            mapper.getSerializerProvider().setNullKeySerializer(new NullKeySerializer());

            try {
                Map<String, String> loginData = mapper.readValue(message, Map.class);
                String username = loginData.get("username");
                String password = loginData.get("password");

                if (username == null || username.trim().isEmpty()) {
                    String response = createJsonResponse("error", "Username cannot be null or empty");
                    channel.basicPublish("", LOGIN_QUEUE, null, response.getBytes());
                    return;
                }

                if (users.containsKey(username) && users.get(username).equals(password)) {
                    String response = createJsonResponse("success", "Login successful");
                    channel.basicPublish("", LOGIN_QUEUE, null, response.getBytes());
                } else {
                    String response = createJsonResponse("error", "Invalid username or password");
                    channel.basicPublish("", LOGIN_QUEUE, null, response.getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
                String response = createJsonResponse("error", "Login failed: " + e.getMessage());
                try {
                    channel.basicPublish("", LOGIN_QUEUE, null, response.getBytes());
                } catch (IOException ioE) {
                    ioE.printStackTrace();
                }
            }
        }, consumerTag -> {
        });

        // QCM list consumer
        channel.basicConsume(QCM_QUEUE, true, (consumerTag, delivery) -> {
            String selectedQcmTitle = new String(delivery.getBody());
            ObjectMapper mapper = new ObjectMapper();
            mapper.getSerializerProvider().setNullKeySerializer(new NullKeySerializer());

            try {
                // Load the selected QCM
                InputStream inputStream = ClassLoader.getSystemResourceAsStream("qcm.json");
                if (inputStream != null) {
                    QCM qcm = mapper.readValue(inputStream, QCM.class);
                    if (qcm.getTitle().equals(selectedQcmTitle)) {
                        // Send the QCM to the client
                        String qcmJson = mapper.writeValueAsString(qcm);
                        channel.basicPublish("", QCM_QUEUE, null, qcmJson.getBytes());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, consumerTag -> {
        });

        // Other consumers remain the same...
    }

    private static String createJsonResponse(String status, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", status);
        response.put("message", message);
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.getSerializerProvider().setNullKeySerializer(new NullKeySerializer());
            return mapper.writeValueAsString(response);
        } catch (IOException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    private static void loadUsers() {
        try {
            File file = new File(USERS_FILE);
            if (file.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.getSerializerProvider().setNullKeySerializer(new NullKeySerializer());
                Map<String, String> loadedUsers = mapper.readValue(file, Map.class);

                users = new HashMap<>();
                for (Map.Entry<String, String> entry : loadedUsers.entrySet()) {
                    if (entry.getKey() != null && !entry.getKey().trim().isEmpty()) {
                        users.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveUsers() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.getSerializerProvider().setNullKeySerializer(new NullKeySerializer());

            Map<String, String> cleanUsers = new HashMap<>();
            for (Map.Entry<String, String> entry : users.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().trim().isEmpty()) {
                    cleanUsers.put(entry.getKey(), entry.getValue());
                }
            }

            mapper.writeValue(new File(USERS_FILE), cleanUsers);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadQCMs() {
        try {
            // Try to load using ClassLoader
            InputStream inputStream = ClassLoader.getSystemResourceAsStream("qcm.json");

            if (inputStream != null) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.getSerializerProvider().setNullKeySerializer(new NullKeySerializer());

                // If your JSON contains a single QCM
                QCM qcm = mapper.readValue(inputStream, QCM.class);

                // Or if your JSON contains an array of QCMs:
                // List<QCM> qcms = mapper.readValue(inputStream, 
                //     mapper.getTypeFactory().constructCollectionType(List.class, QCM.class));
                if (qcm.getTitle() != null && !qcm.getTitle().trim().isEmpty()) {
                    availableQCMs.add(qcm.getTitle());
                    System.out.println("QCM loaded: " + qcm.getTitle());
                }
            } else {
                System.out.println("qcm.json not found in resources.");
            }
        } catch (IOException e) {
            System.err.println("Error loading QCM: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int calculateScore(String answersJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.getSerializerProvider().setNullKeySerializer(new NullKeySerializer());
            Map<String, Integer> studentAnswers = mapper.readValue(answersJson, Map.class);

            File qcmFile = new File(QCM_FILE);
            if (!qcmFile.exists()) {
                System.out.println("QCM file not found.");
                return 0;
            }

            List<Map<String, Object>> qcmQuestions = mapper.readValue(qcmFile, List.class);
            int score = 0;

            for (Map<String, Object> question : qcmQuestions) {
                String questionId = (String) question.get("question_id");
                if (questionId != null) {
                    int correctAnswer = (int) question.get("correct_choice");
                    Integer studentAnswer = studentAnswers.get(questionId);

                    if (studentAnswer != null && studentAnswer == correctAnswer) {
                        score++;
                    }
                }
            }

            return score;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
