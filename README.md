# Modulys Magic Lock

APK Android native autonome, sans serveur Vercel, sans backend et sans API Google.

L'application lit directement une URL texte/JSON de type Inject/WikiTest, par exemple :

```text
https://11z.co/_w/MON_ID/selection
```

Elle permet de scanner les clés JSON disponibles, de choisir la clé à écouter, puis d'appliquer automatiquement une image sur l'écran verrouillé Android.

## Modes disponibles

### Wikipédia

- lit la valeur depuis l'URL source ;
- cherche une image Wikipédia ;
- applique l'image en plein écran sur le lockscreen.

### Peek

- l'utilisateur choisit une image locale depuis le téléphone ;
- l'URI est sauvegardée localement avec permission persistante quand Android le permet ;
- l'utilisateur place et redimensionne une zone de texte avec le doigt ;
- la valeur lue depuis l'URL est dessinée localement sur l'image ;
- le rendu final est appliqué au lockscreen ;
- aucune API externe n'est utilisée pour l'image de base.

## Options Peek

- position X/Y de la zone texte ;
- largeur / hauteur de la zone texte ;
- taille du texte ;
- couleur du texte ;
- opacité de 0 à 100 % ;
- gras ;
- italique ;
- alignement gauche / centre / droite ;
- ombre.

## Build APK

Le workflow GitHub Actions `android-debug-apk.yml` compile l'APK et publie l'artefact :

```text
Modulys-Magic-Lock-v3.apk
```

La signature stable `modulys-magic-lock.keystore` est conservée pour permettre la mise à jour de l'APK sans désinstallation.
