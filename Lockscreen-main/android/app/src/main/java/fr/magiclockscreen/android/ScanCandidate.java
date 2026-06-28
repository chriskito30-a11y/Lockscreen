package fr.magiclockscreen.android;

public class ScanCandidate {
    public final String path;
    public final String value;

    public ScanCandidate(String path, String value) {
        this.path = path;
        this.value = value;
    }

    @Override
    public String toString() {
        if (path == null || path.isEmpty()) return "Texte brut → " + value;
        return path + " → " + value;
    }
}
