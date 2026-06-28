# Déploiement GitHub Actions — Modulys Magic Lock

Cette version est 100 % Android autonome : pas de Vercel, pas de backend, pas d'API Google.

## Relancer la génération APK

Depuis Termux, à la racine du dépôt :

```bash
git status
git add .
git commit -m "Add Peek mode and remove Google images"
git push
```

GitHub Actions relance ensuite le workflow `Build Modulys Magic Lock APK`.

## Artefact attendu

Dans l'onglet Actions du dépôt, télécharger :

```text
Modulys-Magic-Lock-v3.apk
```

La signature stable est conservée avec `android/modulys-magic-lock.keystore`, donc l'APK peut mettre à jour une installation précédente sans désinstallation.
