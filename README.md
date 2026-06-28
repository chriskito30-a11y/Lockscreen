# Magic Lockscreen Android

Alternative Android au principe de `lockscreen-three.vercel.app` :

- une URL Inject / WikiTest fournit une sélection ou révélation ;
- un backend Next.js lit cette URL, extrait la valeur, cherche une image Wikipédia/Wikimedia, puis génère un fond d’écran ;
- une app Android native écoute le backend et applique automatiquement l’image sur l’écran verrouillé avec `WallpaperManager.FLAG_LOCK`.

> Important : une PWA ou un site web seul ne peut pas changer le fond d’écran verrouillé Android. L’app Android est indispensable.

## Structure

```txt
magic-lockscreen-android/
├── web/       Backend + dashboard Next.js
└── android/   App Android Kotlin / Jetpack Compose
```

## Fonctionnement global

```txt
Inject / WikiTest
    ↓
URL /selection
    ↓
Backend Next.js
    ↓
Extraction de la révélation
    ↓
Recherche image Wikipédia/Wikimedia
    ↓
Génération PNG fond d’écran
    ↓
App Android en mode écoute
    ↓
Fond d’écran verrouillé mis à jour
```

## Sécurité

L’URL Inject réelle n’est pas stockée dans l’app Android. Le dashboard crée un `token` chiffré côté serveur avec `MAGIC_CONFIG_SECRET`. L’app Android n’utilise que :

```txt
https://ton-domaine.vercel.app/api/value?token=...
https://ton-domaine.vercel.app/api/wallpaper?token=...
```

Le token permet quand même d’accéder au résultat tant qu’il circule. Ne le publie pas.

## Installation backend

```bash
cd web
npm install
cp .env.example .env.local
npm run dev
```

Dans `.env.local`, mets une valeur longue et secrète :

```txt
MAGIC_CONFIG_SECRET=change-moi-avec-une-valeur-tres-longue-et-privee
```

Ouvre ensuite :

```txt
http://localhost:3000
```

## Déploiement Vercel

1. Crée un projet Vercel depuis le dossier `web/`.
2. Ajoute la variable d’environnement `MAGIC_CONFIG_SECRET`.
3. Déploie.
4. Ouvre le dashboard et crée une configuration.
5. Copie le `token` dans l’app Android.

## Test sans Inject

Utilise l’endpoint mock du backend :

```txt
https://ton-domaine.vercel.app/api/mock?selection=Tom%20Cruise
```

Dans le dashboard :

- URL source : cette URL mock ;
- clé JSON : `selection` ;
- langue Wikipédia : `fr`, puis fallback automatique `en`.

## Installation app Android

1. Ouvre le dossier `android/` dans Android Studio.
2. Laisse Android Studio synchroniser Gradle.
3. Branche ton téléphone Android.
4. Build & Run.
5. Dans l’app :
   - Backend base URL : `https://ton-domaine.vercel.app`
   - Token : token généré par le dashboard
   - Intervalle : 2 secondes recommandé
   - Durée : 10 minutes recommandé
6. Appuie sur `Tester maintenant`, puis `Activer l’écoute`.

## Mode prestation conseillé

```txt
1. Ouvrir Magic Lockscreen Android
2. Activer l’écoute 10 min / 2 sec
3. Verrouiller le téléphone
4. Le spectateur fait la sélection Inject
5. L’app détecte le changement
6. Le fond d’écran verrouillé se met à jour
7. Réveiller le téléphone pour révéler
```

## Notes Android

- L’écoute continue utilise un foreground service avec notification persistante silencieuse.
- Android impose cette notification pour autoriser une tâche active écran verrouillé.
- Certaines marques peuvent demander de désactiver l’optimisation batterie pour une fiabilité parfaite.
- Sur Android 13+, l’app demande l’autorisation de notification.
- L’app utilise `WallpaperManager.FLAG_LOCK`, donc vise le fond d’écran verrouillé.

## Endpoints

```txt
POST /api/scan
POST /api/config
GET  /api/value?token=...
GET  /api/value/:token
GET  /api/wallpaper?token=...
GET  /api/wallpaper/:token
GET  /api/mock?selection=Tom%20Cruise
```

## Limites du MVP

- Pas de compte utilisateur.
- Pas de base de données.
- Pas d’édition avancée de templates.
- Token chiffré stateless : pratique pour Vercel, mais à garder privé.
- Certaines pages Wikipédia n’ont pas d’image exploitable : le backend génère alors un fond texte.
