# Magic Lockscreen Android — APK autonome

Cette version ne dépend plus de Vercel.

L'application Android lit directement l'URL Inject/WikiTest, extrait la valeur JSON ou texte, cherche une photo Wikipédia et applique la photo en fond d'écran verrouillé.

## Champs dans l'app

- URL source Inject : exemple `https://11z.co/_w/15999/selection`
- Clé JSON : `value` ou `selection`
- Langue Wikipédia : `fr` ou `en`
- Intervalle : conseillé 3 à 5 secondes
- Durée : conseillé 10 minutes

## Fonctionnement

1. Sauvegarder
2. Tester la valeur Inject
3. Appliquer la photo maintenant
4. Activer l'écoute avant le tour

Aucun serveur et aucun token ne sont nécessaires.
