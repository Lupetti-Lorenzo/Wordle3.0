# Wordle3.0

Progetto in java per laboratorio 3

## Istruzioni di compilazione e avvio

### Server

Andare nella cartella ./WordleServer.
Le configurazioni possono essere modificate nel file ./server.properties, e sono:
serverport: porta su cui attivare l’applicazione
servermulticast: indirizzo ip del gruppo multicast
portamulticast: porta del gruppo multicast
wordduration: tempo tra una parola segreta e la prossima
Compilazione: javac -cp ".:./gson-2.8.2.jar" *.java 
Esecuzione: java -cp ".:./gson-2.8.2.jar" WordleServerMain    
Esecuzione jar: java -jar WordleServer.jar
Se è stato attivato con successo stamperà sul terminale: [SERVER] WordleServer started on port ___ e la parola segreta iniziale.

### Client

Andare nella cartella ./WordleClient.
Le configurazioni possono essere modificate nel file ./client.properties, e sono:
server: hostname o ip del server
portaserver: porta del server
servermulticast: indirizzo ip del gruppo multicast
portamulticast: porta del gruppo multicast
Compilazione: javac *.java
Esecuzione: java WordleClientMain  
Esecuzione jar: java -jar WordleClient.jar
Da avviare dopo l’ attivazione del server, se eseguito con successo e connesso al server stamperà sul terminale: Benvenuto a Wordle e il menu iniziale.
