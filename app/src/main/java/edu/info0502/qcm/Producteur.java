package edu.info0502.qcm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class Producteur {

    private static final String FILE_ATTENTE_QCM = "QCM_QUEUE";
    private static final String FILE_REPONSES = "RESPONSE_QUEUE";

    public static void main(String[] args) {
        ConnectionFactory usineConnexion = new ConnectionFactory();
        usineConnexion.setHost("10.11.18.72");
        usineConnexion.setPort(5672);
        usineConnexion.setUsername("producteur");
        usineConnexion.setPassword("producteur");

        try (Connection connexion = usineConnexion.newConnection(); Channel canal = connexion.createChannel()) {

            canal.queueDeclare(FILE_ATTENTE_QCM, false, false, false, null);
            canal.queueDeclare(FILE_REPONSES, false, false, false, null);

            String cheminFichierQCM = "src/main/resources/qcm.json";
            String contenuQCM = new String(Files.readAllBytes(Paths.get(cheminFichierQCM)));
            System.out.println("Serveur prêt. Envoi du QCM...");

            canal.basicPublish("", FILE_ATTENTE_QCM, null, contenuQCM.getBytes());

            DeliverCallback gestionnaireReponses = (tag, message) -> {
                String contenuReponses = new String(message.getBody());

                ObjectMapper convertisseur = new ObjectMapper();
                Map<String, Object> qcm = convertisseur.readValue(contenuQCM, Map.class);
                Map<String, Object> reponses = convertisseur.readValue(contenuReponses, Map.class);

                int score = calculerScore(qcm, reponses);
                Map<String, Object> messageScore = Map.of("score", score);
                String jsonScore = convertisseur.writeValueAsString(messageScore);

                canal.basicPublish("", FILE_REPONSES, null, jsonScore.getBytes());
            };

            canal.basicConsume(FILE_REPONSES, true, gestionnaireReponses, tag -> {
            });
            Thread.sleep(600000);

        } catch (IOException | TimeoutException | InterruptedException ex) {
            System.err.println("Erreur serveur : " + ex.getMessage());
        }
    }

    private static int calculerScore(Map<String, Object> qcm, Map<String, Object> reponses) {
        int score = 0;
        List<Map<String, Object>> questions = (List<Map<String, Object>>) qcm.get("questions");
        for (Map<String, Object> question : questions) {
            String idQuestion = (String) question.get("question_id");
            int choixCorrect = (int) question.get("correct_choice");
            int reponseUtilisateur = (int) reponses.getOrDefault(idQuestion, -1);
            if (reponseUtilisateur == choixCorrect) {
                score++;
            }
        }
        return score;
    }
}
class GestionUtilisateurs {

    private static final String FICHIER_UTILISATEURS = "users.json";

    public static synchronized void ajouterUtilisateur(String nom, String motDePasse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, String>> listeUtilisateurs = chargerUtilisateurs();

            // Check if the username already exists
            for (Map<String, String> utilisateur : listeUtilisateurs) {
                if (utilisateur.get("username").equals(nom)) {
                    System.out.println("Ce nom d'utilisateur est déjà pris.");
                    return;
                }
            }

            // Add the new user
            listeUtilisateurs.add(Map.of("username", nom, "password", motDePasse));
            sauvegarderUtilisateurs(listeUtilisateurs);
            System.out.println("Inscription réussie !");
        } catch (IOException ex) {
            System.err.println("Erreur lors de l'enregistrement : " + ex.getMessage());
        }
    }

    public static synchronized boolean validerConnexion(String nom, String motDePasse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, String>> listeUtilisateurs = chargerUtilisateurs();

            // Validate user credentials
            for (Map<String, String> utilisateur : listeUtilisateurs) {
                if (utilisateur.get("username").equals(nom) && utilisateur.get("password").equals(motDePasse)) {
                    return true;
                }
            }
        } catch (IOException ex) {
            System.err.println("Erreur lors de la lecture des utilisateurs : " + ex.getMessage());
        }
        return false;
    }

    private static List<Map<String, String>> chargerUtilisateurs() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File fichier = new File(FICHIER_UTILISATEURS);

        if (fichier.exists()) {
            System.out.println("Chargement des utilisateurs depuis : " + fichier.getAbsolutePath());
            try (Reader reader = new InputStreamReader(new FileInputStream(fichier), StandardCharsets.UTF_8)) {
                return mapper.readValue(reader, mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            } catch (MismatchedInputException e) {
                System.err.println("Le fichier des utilisateurs est vide ou mal formé. Initialisation d'une liste vide.");
                return new ArrayList<>();
            }
        } else {
            System.out.println("Fichier des utilisateurs introuvable. Initialisation d'une liste vide.");
            return new ArrayList<>();
        }
    }

    private static void sauvegarderUtilisateurs(List<Map<String, String>> listeUtilisateurs) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File fichier = new File(FICHIER_UTILISATEURS);

        // Ensure the parent directory exists
        File parentDir = fichier.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Write the user list to the file in UTF-8
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(fichier), StandardCharsets.UTF_8)) {
            mapper.writeValue(writer, listeUtilisateurs);
        }
    }


}