# Horaires de prière et Adhan

## Sources et statut des données

- Hors Tunisie, l'application conserve son fournisseur AlAdhan et son calcul personnalisé 18°.
- En Tunisie, elle utilise l'API publique documentée AlAdhan avec la méthode `18` (« Tunisia »),
  les coordonnées de la ville et le fuseau `Africa/Tunis`.
- Il s'agit d'une **estimation calculée**, pas d'un horaire officiel. L'API AlAdhan avertit elle-même
  que ses résultats peuvent différer des autorités ou mosquées locales.
- Le calendrier public AlMosaly a servi uniquement à la comparaison manuelle. Il n'est ni scrapé,
  ni embarqué, ni présenté comme une API. Aucune API publique et documentée d'AlMosaly ou de
  Tunisie Numérique n'a été identifiée pendant cette mise à jour.
- Chaque ligne de prière offre un ajustement manuel de ±1 minute, sauvegardé par ville et par prière.
  Les valeurs réseau brutes sont mises en cache par ville et par date avant application de l'ajustement.

Comparaison du 13 octobre 2026 à Monastir : le calendrier public AlMosaly affichait Fajr 04:57,
Dhuhr 12:10, Asr 15:16, Maghrib 17:46 et Isha 19:08. La table fournie pour validation différait
d'une minute pour Fajr et Isha. Ces valeurs ne sont pas codées en dur.

## Limites Android

- L'Adhan est désactivé par défaut et ne joue qu'après activation explicite.
- Une alarme exacte est utilisée si Android accorde l'autorisation; sinon l'alarme est approximative.
- Le service foreground `mediaPlayback` n'existe que pendant la lecture et affiche une notification
  permettant de l'arrêter. Il demande l'audio focus et s'arrête en cas d'interruption.
- Les alarmes sont réarmées au redémarrage et aux changements de date, heure, fuseau et permission.
  Le receiver de démarrage ne lance jamais de lecture.
- Android ne garantit rien après « Forcer l'arrêt ». Les restrictions batterie/OEM et le refus des
  notifications ou des alarmes exactes peuvent également retarder ou masquer le comportement.
- L'audio `res/raw/adhan.mp3` existant est local; sa licence doit être confirmée par le propriétaire
  du projet avant distribution. Aucun nouveau fichier audio tiers n'a été ajouté.
