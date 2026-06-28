# Déploiement GitHub + Vercel + APK

## 1. Mettre sur GitHub

Mets le contenu complet du dossier `magic-lockscreen-android/` dans un dépôt GitHub :

```txt
magic-lockscreen-android/
├── .github/workflows/android-debug-apk.yml
├── web/
├── android/
├── README.md
└── GITHUB_DEPLOY.md
```

Important : il faut envoyer le contenu du dossier, pas le ZIP tel quel.

## 2. Déployer le backend sur Vercel

Dans Vercel :

- importer le dépôt GitHub ;
- sélectionner le framework Next.js ;
- définir le Root Directory sur `web` ;
- ajouter la variable d’environnement :

```txt
MAGIC_CONFIG_SECRET=une-cle-longue-et-privee-a-changer
```

Puis déployer.

## 3. Générer l’APK Android avec GitHub Actions

Dans GitHub :

1. Va dans l’onglet `Actions`.
2. Choisis `Build Android Debug APK`.
3. Clique `Run workflow`.
4. Attends la fin du build.
5. Ouvre le build terminé.
6. Télécharge l’artefact `Magic-Lockscreen-Android-debug-apk`.
7. Dans le ZIP téléchargé, récupère `app-debug.apk`.
8. Installe cet APK sur ton téléphone Android.

## 4. Configurer l’app Android

Dans l’app Android :

```txt
Backend base URL : https://ton-projet.vercel.app
Token : le token généré par le dashboard web
Intervalle : 2 secondes
Durée : 10 minutes
```

Appuie d’abord sur `Tester maintenant`, puis sur `Activer l’écoute`.

## 5. Test sans Inject

Dans le dashboard web, teste avec :

```txt
https://ton-projet.vercel.app/api/mock?selection=Tom%20Cruise
```

Clé JSON :

```txt
selection
```
