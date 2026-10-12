# Matrice Eisenhower – widget Android

Widget per la Home con la matrice di Eisenhower (4 quadranti), stile iOS "vetro", semitrasparente e ridimensionabile.

## Come ottenere l'APK con GitHub (senza Android Studio)
1. Crea un repository su GitHub e carica **il contenuto** della cartella (compresa `.github`).
2. Vai nella scheda **Actions** > **Build APK** (parte da sola a ogni push; oppure **Run workflow**).
3. A build finita (3-5 minuti) apri l'esecuzione e scarica l'artefatto **MatriceEisenhower-apk** (zip con `app-debug.apk`).
4. Copia l'APK sul telefono e installalo (consenti "installa da origini sconosciute").
Se il build fallisce, apri il log del passaggio "Compila l'APK" e incollamelo.

## Oppure con Android Studio
1. Installa **Android Studio** (versione recente) e apri la cartella `EisenhowerWidget`.
2. Attendi la sincronizzazione di Gradle (serve internet la prima volta; JDK 17 incluso in Android Studio).
3. Collega il telefono (Android 12 o superiore, debug USB attivo) e premi **Run ▶**, oppure
   **Build > Build APK(s)** e installa `app/build/outputs/apk/debug/app-debug.apk`.
4. Sulla Home: tieni premuto > **Widget** > **Matrice di Eisenhower**.

## Uso
- **Spunta**: tocca il cerchio o il testo di un'attività.
- **Elimina**: tocca la ✕ (compare quando il quadrante è abbastanza largo).
- **Aggiungi**: tocca "+" nell'intestazione del quadrante → si apre una mini finestra sopra la Home (l'app non si apre).
- **Ridimensiona**: tieni premuto il widget e trascina i bordi; testo e righe si adattano.
- **Modifiche mirate** (rinomina, sposta di quadrante, elimina): apri l'app o tocca l'ingranaggio nel widget.
- **Impostazioni** (nell'app): trasparenza, tema scuro/chiaro, dimensione testo, avviso promemoria
  (ogni giorno a un orario, oppure ogni N ore tra le 8 e le 22, con "Rimanda di 1 ora").

## Note
- Android 12+ (minSdk 31). Per gli avvisi concedi il permesso notifiche.
- Gli avvisi usano allarmi non esatti: possono slittare di qualche minuto in risparmio energetico.
- Al primo avvio compaiono alcune attività di esempio: eliminale dal widget o dall'app.
