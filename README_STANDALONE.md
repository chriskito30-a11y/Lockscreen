# Modulys Magic Lock — APK autonome

Version Android autonome, sans Vercel et sans token.

## Fonctionnement

1. Coller l'URL Inject / WikiTest, par exemple `https://11z.co/_w/MON_ID/selection`.
2. Appuyer sur **Scanner l’URL**.
3. Choisir la donnée détectée, par exemple `value → Bob Marley`.
4. Sauvegarder.
5. Appuyer sur **Appliquer la photo maintenant** ou **Activer l’écoute**.

L'application lit l'URL directement depuis le téléphone, cherche la photo Wikipédia, puis applique la photo en fond d'écran verrouillé.

## Build GitHub Actions

L'action génère un artefact nommé :

`Modulys-Magic-Lock-v2`

APK à installer :

`Modulys-Magic-Lock-v2.apk`

## Plus besoin de Vercel

Le dossier `web/` peut rester dans le dépôt, mais l'APK autonome n'en dépend plus.
