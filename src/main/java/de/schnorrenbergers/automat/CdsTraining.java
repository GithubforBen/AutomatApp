package de.schnorrenbergers.automat;

/**
 * Probelauf, aus dem install.sh das Startarchiv des Automaten erzeugt.
 * <p>
 * Nach der PIN-Eingabe verbringt der Automat die meiste Zeit damit, die vielen
 * Klassen von Hibernate und Spring zu laden und zu prüfen - das Entschlüsseln
 * der Datenbank selbst dauert nur einen Bruchteil. Mit
 * {@code -XX:ArchiveClassesAtExit} speichert die JVM diese Klassen nach diesem
 * Lauf in einem Archiv, das der echte Start dann nur noch einblendet
 * ({@code -XX:SharedArchiveFile}). Läuft in einem leeren Ordner, legt dort eine
 * Wegwerf-Datenbank an und braucht Port 8000.
 */
public class CdsTraining {
    public static void main(String[] args) throws Exception {
        Main.warmUpForClassArchive();
        System.exit(0);
    }
}
