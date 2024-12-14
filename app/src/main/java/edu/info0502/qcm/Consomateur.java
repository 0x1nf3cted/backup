package edu.info0502.qcm;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

// public class Client {
//     private static final String QCM_QUEUE = "QCM_QUEUE";
//     private static final String RESPONSE_QUEUE = "RESPONSE_QUEUE";
//     private static final String USERS_FILE = "src/main/resources/users.json";

//     public static void main(String[] args) throws IOException, TimeoutException {
//         Scanner scanner = new Scanner(System.in);
//         System.out.println("1. S'enregistrer\n2. Se connecter");
//         int choice = scanner.nextInt();
//         scanner.nextLine(); // Consommer la nouvelle ligne

//         if (choice == 1) {
//             System.out.print("Entrez le nom d'utilisateur : ");
//             String username = scanner.nextLine();
//             System.out.print("Entrez le mot de passe : ");
//             String password = scanner.nextLine();
//             Server.registerUser(username, password);
//             return;
//         } else if (choice == 2) {
//             System.out.print("Entrez le nom d'utilisateur : ");
//             String username = scanner.nextLine();
//             System.out.print("Entrez le mot de passe : ");
//             String password = scanner.nextLine();

//             if (!Server.loginUser(username, password)) {
//                 System.out.println("Nom d'utilisateur ou mot de passe invalide. Sortie...");
//                 return;
//             }
//         } else {
//             System.out.println("Choix invalide. Sortie...");
//             return;
//         }

//         ConnectionFactory factory = new ConnectionFactory();
//         factory.setHost("localhost"); 
//         factory.setPort(5672);
//         factory.setUsername("guest"); 
//         factory.setPassword("guest"); 

//         try (Connection connection = factory.newConnection();
//              Channel channel = connection.createChannel()) {

//             // declaration des files d'attente
//             channel.queueDeclare(QCM_QUEUE, false, false, false, null);
//             channel.queueDeclare(RESPONSE_QUEUE, false, false, false, null);

//             System.out.println("En attente du QCM du serveur...");

//             // reception du QCM et envoi des réponses
//             DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//                 try {
//                     String qcmJson = new String(delivery.getBody());
//                     ObjectMapper mapper = new ObjectMapper();
//                     Map<String, Object> qcm = mapper.readValue(qcmJson, Map.class);
//                     List<Map<String, Object>> questions = (List<Map<String, Object>>) qcm.get("questions");

//                     if (questions == null || questions.isEmpty()) {
//                         System.out.println("Aucune question reçue. Le QCM est vide.");
//                         return;
//                     }

//                     Map<String, Integer> responses = new HashMap<>();
//                     for (Map<String, Object> question : questions) {
//                         String questionId = (String) question.get("question_id");
//                         String questionText = (String) question.get("text");
//                         List<String> choices = (List<String>) question.get("choices");

//                         System.out.println("\n" + questionText);
//                         for (int i = 0; i < choices.size(); i++) {
//                             System.out.println((i + 1) + ". " + choices.get(i));
//                         }

//                         int response = -1;
//                         while (response < 1 || response > choices.size()) {
//                             System.out.print("Votre réponse (numéro) : ");
//                             if (scanner.hasNextInt()) {
//                                 response = scanner.nextInt();
//                                 if (response < 1 || response > choices.size()) {
//                                     System.out.println("Veuillez entrer un numéro valide entre 1 et " + choices.size() + ".");
//                                 }
//                             } else {
//                                 System.out.println("Entrée invalide. Veuillez entrer un numéro.");
//                                 scanner.next();
//                             }
//                         }
//                         responses.put(questionId, response);
//                     }

//                     String responsesJson = mapper.writeValueAsString(responses);
//                     channel.basicPublish("", RESPONSE_QUEUE, null, responsesJson.getBytes());
//                     System.out.println("\nRéponses envoyées au serveur avec succès.");

//                 } catch (Exception e) {
//                     System.err.println("Erreur lors de la réception du QCM ou de l'envoi des réponses : " + e.getMessage());
//                     e.printStackTrace();
//                 }
//             };

//             channel.basicConsume(QCM_QUEUE, true, deliverCallback, consumerTag -> { });

//             // Réception du score
//             DeliverCallback scoreCallback = (consumerTag, delivery) -> {
//                 try {
//                     String scoreJson = new String(delivery.getBody());
//                     ObjectMapper mapper = new ObjectMapper();
//                     Map<String, Object> scoreMessage = mapper.readValue(scoreJson, Map.class);
//                     int score = (int) scoreMessage.get("score");
//                     System.out.println("\nScore final : " + score);
//                     System.exit(0);
//                 } catch (IOException e) {
//                     System.err.println("Erreur lors de la réception du score : " + e.getMessage());
//                     e.printStackTrace();
//                 }
//             };

//             channel.basicConsume(RESPONSE_QUEUE, true, scoreCallback, consumerTag -> { });

//             synchronized (Client.class) {
//                 try {
//                     Client.class.wait();
//                 } catch (InterruptedException e) {
//                     System.err.println("Erreur lors de l'attente de la réception du score : " + e.getMessage());
//                     e.printStackTrace();
//                 }
//             }

//         } catch (IOException | TimeoutException e) {
//             System.err.println("Erreur de connexion : " + e.getMessage());
//             e.printStackTrace();
//         }
//     }
// }



public class Consomateur {
    private static final String FILE_ATTENTE_QCM = "QCM_QUEUE";
    private static final String FILE_REPONSES = "RESPONSE_QUEUE";

    public static void main(String[] args) {
        try (Scanner lecteur = new Scanner(System.in)) {
            System.out.println("Choisissez une option :\n1. Créer un compte\n2. Se connecter");
            int option = lecteur.nextInt();
            lecteur.nextLine();

            if (option == 1) {
                System.out.print("Saisissez un nom d'utilisateur : ");
                String utilisateur = lecteur.nextLine();
                System.out.print("Saisissez un mot de passe : ");
                String motDePasse = lecteur.nextLine();
                GestionUtilisateurs.ajouterUtilisateur(utilisateur, motDePasse);
                return;
            } else if (option == 2) {
                System.out.print("Nom d'utilisateur : ");
                String utilisateur = lecteur.nextLine();
                System.out.print("Mot de passe : ");
                String motDePasse = lecteur.nextLine();

                if (!GestionUtilisateurs.validerConnexion(utilisateur, motDePasse)) {
                    System.out.println("Nom d'utilisateur ou mot de passe incorrect. Fin de programme.");
                    return;
                }
            } else {
                System.out.println("Option invalide. Fin de programme.");
                return;
            }

            ConnectionFactory usineConnexion = new ConnectionFactory();
            usineConnexion.setHost("localhost");
            usineConnexion.setPort(5672);
            usineConnexion.setUsername("guest");
            usineConnexion.setPassword("guest");

            try (Connection connexion = usineConnexion.newConnection();
                 Channel canal = connexion.createChannel()) {

                canal.queueDeclare(FILE_ATTENTE_QCM, false, false, false, null);
                canal.queueDeclare(FILE_REPONSES, false, false, false, null);

                System.out.println("En attente de questions...");

                DeliverCallback gestionnaireQCM = (tag, message) -> {
                    try {
                        String contenuQCM = new String(message.getBody());
                        ObjectMapper convertisseur = new ObjectMapper();
                        Map<String, Object> qcm = convertisseur.readValue(contenuQCM, Map.class);
                        List<Map<String, Object>> questions = (List<Map<String, Object>>) qcm.get("questions");

                        if (questions == null || questions.isEmpty()) {
                            System.out.println("Le QCM est vide. Fin de traitement.");
                            return;
                        }

                        Map<String, Integer> reponses = new HashMap<>();
                        for (Map<String, Object> question : questions) {
                            System.out.println("\n" + question.get("text"));
                            List<String> choix = (List<String>) question.get("choices");

                            for (int i = 0; i < choix.size(); i++) {
                                System.out.println((i + 1) + ". " + choix.get(i));
                            }

                            int reponse;
                            do {
                                System.out.print("Votre choix : ");
                                while (!lecteur.hasNextInt()) {
                                    System.out.println("Veuillez entrer un numéro valide.");
                                    lecteur.next();
                                }
                                reponse = lecteur.nextInt();
                            } while (reponse < 1 || reponse > choix.size());

                            reponses.put((String) question.get("question_id"), reponse);
                        }

                        String jsonReponses = convertisseur.writeValueAsString(reponses);
                        canal.basicPublish("", FILE_REPONSES, null, jsonReponses.getBytes());
                        System.out.println("Réponses transmises au serveur.");

                    } catch (Exception ex) {
                        System.err.println("Erreur lors du traitement du QCM : " + ex.getMessage());
                    }
                };

                canal.basicConsume(FILE_ATTENTE_QCM, true, gestionnaireQCM, tag -> {});

                DeliverCallback gestionnaireScore = (tag, message) -> {
                    try {
                        String contenuScore = new String(message.getBody());
                        ObjectMapper convertisseur = new ObjectMapper();
                        Map<String, Object> donneesScore = convertisseur.readValue(contenuScore, Map.class);
                        System.out.println("\nVotre score : " + donneesScore.get("score"));
                        System.exit(0);
                    } catch (Exception ex) {
                        System.err.println("Erreur lors de la réception du score : " + ex.getMessage());
                    }
                };

                canal.basicConsume(FILE_REPONSES, true, gestionnaireScore, tag -> {});

                synchronized (Consomateur.class) {
                    Consomateur.class.wait();
                }

            } catch (IOException | TimeoutException | InterruptedException ex) {
                System.err.println("Erreur de communication : " + ex.getMessage());
            }

        } catch (Exception ex) {
            System.err.println("Erreur inattendue : " + ex.getMessage());
        }
    }
}
