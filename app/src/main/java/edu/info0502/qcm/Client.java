package edu.info0502.qcm;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class Client {

    private static final String QCM_QUEUE = "qcm_queue";
    private static final String ANSWER_QUEUE = "answer_queue";
    private static final String SCORE_QUEUE = "score_queue";
    private static final String REGISTRATION_QUEUE = "registration_queue";
    private static final String LOGIN_QUEUE = "login_queue";
    private static final Scanner scanner = new Scanner(System.in);
    private static final CountDownLatch latch = new CountDownLatch(1);

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("client");
        factory.setPassword("client");

        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {

            channel.queueDeclare(QCM_QUEUE, false, false, false, null);
            channel.queueDeclare(ANSWER_QUEUE, false, false, false, null);
            channel.queueDeclare(SCORE_QUEUE, false, false, false, null);
            channel.queueDeclare(REGISTRATION_QUEUE, false, false, false, null);
            channel.queueDeclare(LOGIN_QUEUE, false, false, false, null);

            displayMenu(channel);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private static void displayMenu(Channel channel) throws IOException, InterruptedException {
        System.out.println("\nWelcome to the QCM Client!");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Choose QCM");
        System.out.println("4. Exit");

        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        switch (choice) {
            case 1:
                if (registerUser(channel)) {
                    System.out.println("Registration successful!");
                    displayMenu(channel);
                }
                break;
            case 2:
                if (loginUser(channel)) {
                    System.out.println("Login successful!");
                    chooseQCM(channel);
                }
                break;
            case 3:
                chooseQCM(channel);
                break;
            case 4:
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option. Please try again.");
                displayMenu(channel);
        }
    }

    private static boolean registerUser(Channel channel) throws IOException, InterruptedException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Username cannot be empty!");
            return false;
        }

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        if (password.isEmpty()) {
            System.out.println("Password cannot be empty!");
            return false;
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializerProvider().setNullKeySerializer(new NullKeySerializer());
        Map<String, String> registrationData = new HashMap<>();
        registrationData.put("username", username);
        registrationData.put("password", password);
        String registrationJson = mapper.writeValueAsString(registrationData);

        channel.basicPublish("", REGISTRATION_QUEUE, null, registrationJson.getBytes());
        System.out.println("Registration request sent. Waiting for response...");

        CountDownLatch registrationLatch = new CountDownLatch(1);

        DeliverCallback registrationHandler = (consumerTag, delivery) -> {
            try {
                String responseJson = new String(delivery.getBody());
                Map<String, String> response = mapper.readValue(responseJson, Map.class);
                String status = response.get("status");
                String message = response.get("message");

                System.out.println(message);
                registrationLatch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                registrationLatch.countDown();
            }
        };

        channel.basicConsume(REGISTRATION_QUEUE, true, registrationHandler, consumerTag -> {
        });
        registrationLatch.await();

        return true;
    }

    private static boolean loginUser(Channel channel) throws IOException, InterruptedException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();  // Read the username
        System.out.print("Enter password: ");
        String password = scanner.nextLine();  // Read the password

        if (password.isEmpty()) {  // Just in case password is empty
            System.out.println("Password cannot be empty!");
            return false;
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", username);
        loginData.put("password", password);
        String loginJson = mapper.writeValueAsString(loginData);

        // Send login data to server
        channel.basicPublish("", LOGIN_QUEUE, null, loginJson.getBytes());
        System.out.println("Login request sent. Waiting for response...");

        CountDownLatch loginLatch = new CountDownLatch(1);
        DeliverCallback loginHandler = (consumerTag, delivery) -> {
            try {
                String responseJson = new String(delivery.getBody());
                Map<String, String> response = mapper.readValue(responseJson, Map.class);
                String status = response.get("status");
                String message = response.get("message");

                System.out.println(message);
                loginLatch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                loginLatch.countDown();
            }
        };
        channel.basicConsume(LOGIN_QUEUE, true, loginHandler, consumerTag -> {
        });
        loginLatch.await();

        return true;
    }

    // Choose QCM to participate in
    private static void chooseQCM(Channel channel) throws IOException, InterruptedException {
        System.out.println("Requesting QCM list from the server...");

        // First, send a request for the QCM list
        channel.basicPublish("", QCM_QUEUE, null, "request_qcm_list".getBytes());

        // Then set up consumer to receive the response
        channel.basicConsume(QCM_QUEUE, true, (consumerTag, delivery) -> {
            try {
                String qcmListJson = new String(delivery.getBody());
                ObjectMapper mapper = new ObjectMapper();
                List<String> qcmList = mapper.readValue(qcmListJson, List.class);

                System.out.println("\nAvailable QCMs:");
                for (int i = 0; i < qcmList.size(); i++) {
                    System.out.println((i + 1) + ". " + qcmList.get(i));
                }

                System.out.print("\nSelect a QCM (1-" + qcmList.size() + "): ");
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                if (choice >= 1 && choice <= qcmList.size()) {
                    String selectedQcm = qcmList.get(choice - 1);
                    // Request the selected QCM
                    channel.basicPublish("", QCM_QUEUE, null, selectedQcm.getBytes());
                } else {
                    System.out.println("Invalid selection!");
                    latch.countDown();
                }

            } catch (Exception e) {
                System.err.println("Error processing QCM list: " + e.getMessage());
                latch.countDown();
            }
        }, consumerTag -> {
        });
    }

    // Handle the QCM received from the server
    private static DeliverCallback qcmHandler(Channel channel) {
        return (consumerTag, delivery) -> {
            try {
                String qcmJson = new String(delivery.getBody());
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> qcm = mapper.readValue(qcmJson, Map.class);

                Map<String, Integer> answers = processQuestions(qcm);
                if (answers != null && !answers.isEmpty()) {
                    String answersJson = mapper.writeValueAsString(answers);
                    channel.basicPublish("", ANSWER_QUEUE, null, answersJson.getBytes());
                    System.out.println("Answers sent, waiting for score...");
                } else {
                    System.out.println("No answers to submit.");
                    latch.countDown();
                }
            } catch (Exception e) {
                System.err.println("Error processing QCM: " + e.getMessage());
                latch.countDown();
            }
        };
    }

    // Process the questions and collect the answers from the user
    private static Map<String, Integer> processQuestions(Map<String, Object> qcm) {
        Map<String, Integer> answers = new HashMap<>();
        List<Map<String, Object>> questions = (List<Map<String, Object>>) qcm.get("questions");

        for (Map<String, Object> question : questions) {
            boolean validAnswer = false;
            int attempts = 0;
            final int MAX_ATTEMPTS = 3;

            while (!validAnswer && attempts < MAX_ATTEMPTS) {
                try {
                    System.out.println("\n" + question.get("text"));
                    List<String> choices = (List<String>) question.get("choices");

                    for (int i = 0; i < choices.size(); i++) {
                        System.out.println((i + 1) + ". " + choices.get(i));
                    }

                    System.out.print("Your answer (1-" + choices.size() + "): ");
                    int answer = scanner.nextInt();
                    scanner.nextLine(); // Consume newline

                    if (answer >= 1 && answer <= choices.size()) {
                        answers.put((String) question.get("question_id"), answer);
                        validAnswer = true;
                    } else {
                        System.out.println("Please enter a number between 1 and " + choices.size());
                        attempts++;
                    }
                } catch (Exception e) {
                    System.err.println("Error processing input: " + e.getMessage());
                    return answers;
                }
            }

            if (!validAnswer) {
                System.out.println("Too many invalid attempts. Moving to next question...");
            }
        }

        return answers;
    }
}
