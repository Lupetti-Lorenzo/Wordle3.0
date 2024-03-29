
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SecretWordSessionManager extends Thread {
    private final ScheduledExecutorService executor;
    private final SecretWordSession session; // sessione corrente
    public static int wordDuration; // durata della sessione in secondi
    // codici dei background per creare le risposte alle parole
    private static final String GREEN_BACKGROUND = "\u001B[42m";
    private static final String YELLOW_BACKGROUND = "\u001B[43m";
    private static final String ANSI_END = "\u001B[0m";

    public SecretWordSessionManager(int seconds) {
        session = new SecretWordSession();
        wordDuration = seconds;
        // ogni tot secondi parte e cambia la secretword, sersessionmap e activewordtime initialDelay = 0 almeno parte anche subito
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new SecretWordChanger(session), 0, wordDuration, TimeUnit.SECONDS);
    }

    // usata per sapere se una parola è valida
    public boolean wordExists(String word) {
        try (final RandomAccessFile words = new RandomAccessFile("words.txt", "r")) {
            long start = 0;
            long end = words.length();
            String wordRidden;
            while (start <= end) {
                long mid = ((start + end) / 2);
                mid = mid - mid % 11;
                words.seek(mid);
                wordRidden = words.readLine();
                if (wordRidden.compareTo(word) == 0)
                    return true;
                else if (wordRidden.compareTo(word) < 0)
                    start = mid + 10;
                else
                    end = mid - 10;
            }
            return false;
        } catch (FileNotFoundException e) {
            System.out.println("[SERVER] Errore nell'analisi del file delle parole: file non trovato.\n" + e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.out.println("[SERVER] Errore nell'analisi del file delle parole.\n" + e.getMessage());
            throw new RuntimeException(e);
        }
    }



    // funzione prende la parola da provare,
    // restituisce la parola colorata in base alla parola segreta
    public String getWordHint(String word) {
        StringBuilder hints = new StringBuilder();
        String secWord = this.getSecretWord();
        for (int i = 0; i < secWord.length(); i++) {
            char currChar = word.charAt(i);
            String newChar;
            if (secWord.charAt(i) == currChar) { // lettera nella posizione giusta
                newChar = GREEN_BACKGROUND + currChar + ANSI_END;
            }
            else if (secWord.indexOf(currChar) != -1) { // lettera c'è ma non nella posizione i
                newChar = YELLOW_BACKGROUND + currChar + ANSI_END;
            }
            else  { // lettera non ce
                newChar = String.valueOf(currChar);
            }
            hints.append(newChar);
        }
        return hints.toString();
    }

    // come getWordHint ma al posto delle lettere mette _, usata per la notifica
    public String getWordHintBG(String word) {
        StringBuilder hintsBG = new StringBuilder();
        String secWord = this.getSecretWord();
        for (int i = 0; i < secWord.length(); i++) {
            char currChar = word.charAt(i);
            String newChar;
            if (secWord.charAt(i) == currChar) { // lettera nella posizione giusta
                newChar = GREEN_BACKGROUND + "_" + ANSI_END;
            }
            else if (secWord.indexOf(currChar) != -1) { // lettera ce ma non nella posizione i
                newChar = YELLOW_BACKGROUND + "_" + ANSI_END;
            }
            else  { // lettera non ce
                newChar = "_";
            }
            hintsBG.append(newChar);
        }
        return hintsBG.toString();
    }

    // crea una nuova sessione di un utente e la ritona al chiamante
    public UserSession newUserSession(String username) {
        session.usersSessionMap.put(username, new UserSession(username));
        return this.getUserSession(username);
    }

    public UserSession getUserSession(String username) {
        return session.usersSessionMap.get(username);
    }

    // rimuovi una sessione dalla mappa, usata alla logout se non ho vinto o mandato 12 parole
    public void removeUserSession(String username) {
        session.usersSessionMap.remove(username);
    }

    // se l'utente non nella mappa(mai entrato in questa sessione) oppure non ha finito, allora puó giocare
    public boolean canPlay(String username) {
        return !session.usersSessionMap.containsKey(username) ||
                !session.usersSessionMap.get(username).hasFinished();
    }

    public String getSecretWord() {
        return session.secretWord.toString();
    }

    public boolean isSecretWord(String word) {
        return word.equals(this.getSecretWord());
    }

    public String getWordDurationDate() { return this.session.wordDurationDate.toString(); }

    // usata dal ServerTerminationHandler per chiudere questo thread che cambia parole ogni tot tempo
    public void stopSWSM() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2000, TimeUnit.MILLISECONDS))
                executor.shutdownNow();
        }
        catch (InterruptedException e) {executor.shutdownNow();}
        System.out.println("[SERVER] SecretWordSessionManager terminato.");
    }

    // thread che cambia la parola segreta ogni tot tempo
    private static class SecretWordChanger extends Thread {
        private final SecretWordSession session;
        public SecretWordChanger(SecretWordSession session) {
            this.session = session;
        }

        @Override
        public void run() {
            // nuova parola
            session.secretWord = new StringBuilder(getRandomWord());
            // nuova sessione di utenti
            session.usersSessionMap = new ConcurrentHashMap<>();
            // nuovo identificativo della parola, il range di tempo espresso in datainizio-datafine
            session.wordDurationDate = new StringBuilder();
            // mi salvo l'intervallo di tempo in cui questa parola è attiva come range di date
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date today = Calendar.getInstance().getTime(); // data di adesso
            Calendar calendar = Calendar.getInstance(); // calcolo datafine
            calendar.setTime(today);
            calendar.add(Calendar.SECOND, wordDuration); // data fine parola segreta (data di adesso + durata)
            // aggiorno l'intervallo formattato
            session.wordDurationDate.append(df.format(today)).append(" - ").append(df.format(calendar.getTime()));
            System.out.println("[SERVER] Nuova parola: " + session.secretWord + ", durerà " +  session.wordDurationDate);
        }

        private static String getRandomWord() {
            try {
                RandomAccessFile words = new RandomAccessFile("words.txt", "r");
                long random = (long) (Math.random() * words.length());
                random = random - random % 11;
                words.seek(random);
                String secretWord = words.readLine();
                words.close();
                return secretWord;
            } catch (IOException e) {
                System.out.println("[SERVER] Errore nell'analisi del file delle parole");
                throw new RuntimeException(e);
            }
        }
    }
}
