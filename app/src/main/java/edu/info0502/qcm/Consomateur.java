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
            usineConnexion.setHost("10.11.18.72");
            usineConnexion.setPort(5672);
            usineConnexion.setUsername("consomateur");
            usineConnexion.setPassword("consomateur");

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
