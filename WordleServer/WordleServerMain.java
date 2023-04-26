

import java.io.*;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class WordleServerMain {
    // Percorso del file di configurazione del server.
    public static final String configFile = "./server.properties";
    // Hasmap contente i dati degli utenti che tengo nel file json
    public static HashMap<String, UserData> usersMap = new HashMap<>();

    // Parametri di configurazione - porta sul quale il server sta in ascolto, indirizzo e porta del multicast e la durata di una parola segreta(in secondi)
    public static int SERVERPORT;
    public static int PORTAMULTICAST;
    public static String SERVERMULTICAST;
    public static int WORDDURATION;


    public static void main(String[] args) {
        // Leggo il file di configurazione, aggiorna indirizzoserver poratserver, servermulticast e portamulticast
        if (!readConfig()) {
            System.out.println("[SERVER] Errore nella lettura dei parametri di configurazione");
            return;
        }
        //all'avvio del server, deserializzo il file usersData.json e popolo la mappa con i dati degli utenti
        usersMap = UsersDataJsonWriter.getINSTANCE().readJsonMap();
        if (usersMap == null) usersMap = new HashMap<>();

        // parte il SecretWordSessionManager
        // oggetto per interagire con la sessione che passo ai ClientHandlers e al sessionManager
        SecretWordSessionManager wordSessionManager = new SecretWordSessionManager(WORDDURATION);

        //faccio partire il server - thread pool per gestire piu client contemporaneamente
        try (ServerSocket server = new ServerSocket(SERVERPORT)) {
            // Apro il socket verso il canale multicast
            MulticastSocket ms = new MulticastSocket();
            // pool di thread per gestire gli utenti
            ExecutorService pool = Executors.newCachedThreadPool();
            System.out.println("[SERVER] WordleServer started on port " + SERVERPORT);
            // registro il termination handler per il server, in modo da chiudere anche threadpool e wordsessionmenager
            Runtime.getRuntime().addShutdownHook(new ServerTerminationHandler(2000, pool, server, wordSessionManager, ms));
            //loop di accettazione sulla threadpool
            while (true) {
                pool.execute(new ClientHandler(server.accept(), usersMap, wordSessionManager, ms));
                System.out.println("[SERVER] Nuovo client arrivato");
            }
        } catch (IOException e) {
            // ci pensa il termination handler
        }
    }
    /**
     * Metodo per leggere il file di configurazione del client.
     * @throws FileNotFoundException se il file non e' presente
     * @throws IOException se qualcosa non va in fase di lettura
     */
    public static boolean readConfig() {
        try {
            InputStream input = new FileInputStream(configFile);
            Properties prop = new Properties();
            prop.load(input);
            SERVERPORT = Integer.parseInt(prop.getProperty("serverport"));
            SERVERMULTICAST = prop.getProperty("servermulticast");
            PORTAMULTICAST = Integer.parseInt(prop.getProperty("portamulticast"));
            WORDDURATION = Integer.parseInt(prop.getProperty("wordduration"));
            input.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}