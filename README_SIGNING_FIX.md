# Correctif signature APK

Ce patch ajoute une clé de signature locale `android/modulys-magic-lock.keystore` et configure Gradle pour signer tous les APK avec la même clé.

But : permettre les mises à jour APK sans désinstaller l'ancienne version.

Important : l'installation précédente signée avec une autre clé devra être désinstallée une dernière fois. Les APK générés après ce patch pourront ensuite se mettre à jour normalement.
