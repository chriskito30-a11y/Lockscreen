# Modulys Magic Lock — option Google Images

Ce patch ajoute un choix de source image dans l’APK autonome :

- Auto : Google Images si configuré, sinon Wikipédia
- Wikipédia uniquement
- Google Images

Google Images utilise l’API officielle Google Programmable Search / Custom Search JSON API.
Il faut renseigner dans l’app :

- Google API Key
- Google Search Engine ID / CX

Sans ces deux champs, le mode Auto reste fonctionnel avec Wikipédia.

## Exemple d’URL Inject générique

https://11z.co/_w/MON_ID/selection

## Installation du patch

```bash
cd ~/Lockscreen
unzip -o /storage/emulated/0/Download/lockscreen_android_google_images_patch.zip -d .

git add android/app/src/main/java/fr/magiclockscreen/android/MagicPrefs.java \
        android/app/src/main/java/fr/magiclockscreen/android/MagicStandaloneClient.java \
        android/app/src/main/java/fr/magiclockscreen/android/MainActivity.java \
        README_GOOGLE_IMAGES.md

git commit -m "Add Google Images provider to standalone APK"
git push
```

Puis relancer GitHub Actions pour générer l’APK.
