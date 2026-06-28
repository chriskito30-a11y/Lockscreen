package fr.magiclockscreen.android;

public class MagicResult {
    public final String value;
    public final String hash;
    public final String imageUrl;
    public final String title;

    public MagicResult(String value, String hash, String imageUrl, String title) {
        this.value = value;
        this.hash = hash;
        this.imageUrl = imageUrl;
        this.title = title;
    }
}
