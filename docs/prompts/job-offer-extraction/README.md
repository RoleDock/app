# Historique des prompts d’extraction

## Proposition v4 après la série v3

La [v4](v4.txt) est une nouvelle candidate, non activée et non évaluée. Les snapshots v1/v2/v3 et le schéma sont conservés ; le manifeste ajoute les empreintes de v4. Les statuts des sections précédentes ci-dessous décrivent leurs livraisons historiques.

Les 24 sorties déclarées v3 (trois par fixture) motivent ces changements ciblés :

- Préserver les missions explicites même génériques, indépendamment de leur présence dans le résumé (REV-015).
- Distinguer activité, qualification demandée et technologie/domaine nommé dans une mission. Ne pas transformer automatiquement une activité en compétence ; conserver Go contextualisé et les demandes explicites d’expérience dans une activité (REV-014).
- Vérifier le sujet nommé de la phrase de recrutement avant company=null ; ne pas juger le réalisme du nom et ne pas prendre un pronom pour une entreprise (REV-007).
- Classer permis et licences dans CERTIFICATION, séparément de WORK_AUTHORIZATION (REV-013).
- Conserver dans rawText le contexte environnement/mission/titre qui justifie la classification, plutôt que le seul nom de technologie (REV-001).
- Résumer les faits présents sans liste de champs absents, sans transfert des qualificatifs entre exigences ni affaiblissement des modalités (REV-011).

Contrôles de comparaison avec v3 : 01 conserve cinq exigences, sans activités API/interfaces ajoutées ; 03 conserve onze technologies avec citations de contexte ; 06 conserve Exemple, quatre exigences et le permis CERTIFICATION ; 07 conserve sa mission générique. Sur 04, Go reste CONTEXTUAL/CORE. Les modalités remote, fréquences et autres conventions v3 ne changent pas. Ces attentes sont des conventions candidates, pas des résultats déjà obtenus.

Après comparaison des fixtures, utiliser des entrées nouvelles pour vérifier les frontières : même activité formulée comme mission puis comme expérience exigée, mission avec technologie nommée, employeur au nom inhabituel, permis versus autorisation de travail. Surveiller particulièrement les omissions causées par une sélection trop stricte et les résumés trop courts. Le prompt est plus long ; mesurer aussi durée, coût et éventuelle troncature. Aucun appel API n’a été lancé pour préparer v4.

Depuis `app`, arrêter le backend avec Ctrl+C puis activer et démarrer :

```powershell
Copy-Item docs/prompts/job-offer-extraction/v4.txt backend/src/main/resources/job-offer-extraction/instructions.txt
.\data\bruno-roledock\Start-Backend.ps1
```

Pour revenir à v3, copier v3.txt à la place puis redémarrer. La préparation de v4 ne modifie pas le fichier actif. Conserver le même modèle/schéma/entrée/paramètres pour comparer, archiver les métadonnées des appels et ne pas écraser les anciennes réponses.

## État après les essais v2 et proposition v3

La [v3](v3.txt) est prête à tester, non activée et non évaluée. Le fichier actif sur disque reste v2 lors de cette livraison. Les sections v1/v2 ci-dessous décrivent l’archivage initial ; leurs statuts historiques ne décrivent pas l’état actuel. Les snapshots v1/v2 et le schéma restent inchangés. Le manifeste ajoute v3 avec ses empreintes.

Quinze sorties déclarées v2 couvrent les huit fixtures, sans trois répétitions pour chacune et sans métadonnées complètes des appels. Elles motivent les règles candidates suivantes, sans démontrer leur efficacité :

| Sujet | Règle v3 et contrôle attendu |
| --- | --- |
| Entreprise | Chercher le sujet de la phrase de recrutement avant null ; extraire le nom sans les prépositions introductives, sans retrait aveugle d’un mot du nom. Cas 03 : Exemple Plateforme ; 06 : Exemple. |
| Centralité | Exiger un lien textuel au rôle ou aux tâches pour CORE/SUPPORTING ; INCIDENTAL exige un caractère périphérique explicite. Sinon UNKNOWN, y compris pour une condition administrative obligatoire. Sur 03, la proximité entre liste de stack et mission de déploiement ne suffit pas. |
| Territoire remote | Conserver remoteArea et une exigence LOCATION pour le lieu d’exercice ; ne pas déduire résidence ou nationalité. Cas 02 : Télétravail depuis la France, REQUIRED/EXPLICIT, centralité UNKNOWN, blocker false sans restriction explicitement obligatoire/exclusive. |
| Fréquence | Cas 01 : OTHER/2/jours par semaine pour la contrainte sans comparateur explicite, tout en gardant onSiteDaysPerWeek=2 et le blocker de présence obligatoire. |
| Résumé | Produire une synthèse brève des faits exploitables, avec rôle/mission, exigences principales et modalités ; null seulement sans fait résumable. Pas de remplissage par la liste des champs absents. |

Les règles de granularité, alternatives, citations, contraintes de niveau/expérience et protection contre les instructions parasites sont conservées. Tester aussi des variantes nouvelles : nom contenant réellement une préposition, territoire préféré ou explicitement exclusif, fréquence minimale/exacte, outil explicitement relié à une mission. Risques à surveiller : excès de UNKNOWN, condition territoriale mal qualifiée, nom tronqué et résumé trop long. Les conventions sur OTHER et sur les blockers territoriaux sont des choix d’extraction à valider, pas des règles juridiques.

Pour comparer, appliquer le protocole ci-dessous avec v2 comme référence et v3 comme candidate, même modèle/schéma/entrée/paramètres. Archiver les réponses séparément avec version et empreinte ; ne pas réécrire v1/v2. Les résultats détaillés restent dans le rapport local ignoré par Git.

Depuis `app`, après Ctrl+C dans le terminal backend :

```powershell
Copy-Item docs/prompts/job-offer-extraction/v3.txt backend/src/main/resources/job-offer-extraction/instructions.txt
.\data\bruno-roledock\Start-Backend.ps1
```

Remplacer v3.txt par v2.txt pour revenir à v2, puis redémarrer. Ces commandes n’ont pas été exécutées lors de cette proposition.

## Versions

| Version | Fichier | Statut à la création de cet historique |
| --- | --- | --- |
| v1 | [v1.txt](v1.txt) | Copie exacte du prompt backend présent lors de l’archivage |
| v2 | [v2.txt](v2.txt) | Candidate proposée, non activée, non évaluée auprès du fournisseur |

Le backend charge toujours `backend/src/main/resources/job-offer-extraction/instructions.txt`. Ces fichiers historiques ne sont pas sélectionnés automatiquement. Le prompt actif correspond à v1 au moment de cette livraison ; l’état d’un processus déjà démarré n’est pas attesté par cette correspondance disque.

Les prompts, le [schéma archivé](schema-v1.json), ce document et le manifeste peuvent être versionnés : ils ne contiennent ni annonces réelles ni secrets. Les réponses et notes de tests restent dans le dossier ignoré `tmp/job-offer-runs/`. Aucun commit n’a été créé.

Conserver les snapshots v1/v2 une fois utilisés dans un essai. Pour toute modification ultérieure, créer v3, puis v4, etc. Ne pas écraser une version déjà évaluée. Le manifeste contient les empreintes SHA-256 et la date d’archivage. Le hash exact atteste les octets locaux ; le hash UTF-8/LF permet une comparaison malgré les conversions CRLF/LF de Git. Toujours préciser lequel est enregistré dans les essais.

**Limite de la baseline :** v1 est une copie attestée du fichier actuel. Les anciens résultats collés dans la conversation n’enregistrent pas l’empreinte effectivement utilisée : ne pas leur attribuer rétroactivement une version certifiée. Rejouer une référence v1 avec métadonnées si une comparaison contrôlée est nécessaire.

## Hypothèses de la v2

Les 13 réponses synthétiques relues motivent cette proposition, sans prouver une amélioration. Les règles restent générales : pas de traitement particulier du nom Exemple, d’un cas numéroté ou d’une liste attendue de technologies. La définition JSON et les enums restent inchangés ; seules des conventions d’extraction sont proposées.

| Sujet | Règle proposée | Observation concernée |
| --- | --- | --- |
| Entreprise | Conserver un nom explicitement présenté comme employeur, même générique ou fictif | REV-007, omission dans 3/3 sorties du cas 06 |
| Stack | Une entrée par technologie indépendante ; mention candidat précise prioritaire sur un contexte compatible ; pas de double entrée redondante | REV-005, regroupement dans 4/4 sorties du cas 03 |
| Alternatives | Conserver les groupes OR et les conditions ; ne pas les découper en obligations cumulatives | Limite de la séparation atomique |
| Apprentissage proposé | Contexte d’interprétation ou résumé, pas une exigence autonome | REV-006, entrée présente dans 1/4 sorties du cas 03 |
| Citations | Conserver modalité, négation et conditions dans une citation contiguë exacte | REV-001/002 |
| Technologie en mission | CONTEXTUAL si aucune maîtrise préalable n’est demandée ; CORE reste possible | REV-003 : Go au cas 04 |
| Technologie uniquement dans le titre | UNKNOWN pour la nécessité, CORE possible pour la centralité | REV-003 : React/Node au cas 02 |
| Centralité | Relation avec les missions ; UNKNOWN si insuffisamment étayée, sans déduction depuis obligatoire ou bonus | REV-009 |
| Durée sans borne | OTHER, valeur numérique et unité source ; la préférence reste dans requirementKind | REV-008 : deux années souhaitées |
| Niveau nommé | OTHER avec valeur du niveau et unité null, sauf comparaison explicite | REV-008 : C1 |
| Territoire remote | remoteArea ; pas location.country si ce pays ne décrit que l’éligibilité au télétravail | REV-004 |

Ce sont des **conventions candidates**, pas des vérités prouvées par les réponses précédentes. En particulier, elles font évoluer les attentes sémantiques des cas 02, 04 et 06. Évaluer aussi leurs risques : excès de UNKNOWN, sous-extraction de qualifications implicitement nécessaires, consolidation excessive, et allongement du prompt ou des listes de sortie. Le prompt v2 est plus long ; mesurer coût, durée et troncature, pas seulement la qualité.

## Comparaison proposée

Conserver le même modèle, le même schéma, les mêmes octets d’entrée et les mêmes paramètres API. Ne pas changer le prompt et le modèle simultanément. Un alias de modèle peut évoluer : enregistrer l’identifiant effectivement retourné lorsqu’il est disponible ; sinon signaler cette limite.

1. Fixer ces conventions avant les nouveaux résultats. Les notes synthétiques existantes restent intactes ; les critères ci-dessous complètent la comparaison candidate.
2. Commencer par les cas 03 et 06, puis couvrir les huit cas avec au moins trois passages par version lorsque possible. Ne pas s’arrêter aux seules sorties favorables.
3. Enregistrer séparément entrée exacte, réponse JSON, HTTP, heure UTC, durée, modèle, paramètres, version/empreinte du prompt et du schéma. Laisser inconnue toute métadonnée absente. Le modèle reste configuré côté backend, pas dans Bruno.
4. Comparer les champs et classifications, pas l’ordre des exigences, la casse du titre ou la formulation libre du résumé. Distinguer correction, changement de convention et régression.
5. Après les fixtures connues, tester des annonces ou variantes synthétiques nouvelles pour vérifier que les règles ne sont pas adaptées uniquement aux huit exemples.

| Cas | Contrôles prioritaires v2 |
| --- | --- |
| 01 | Java/Spring Boot/Angular REQUIRED, PostgreSQL PREFERRED ; présence deux jours conservée dans le blocker ; citations justificatives |
| 02 | React/Node UNKNOWN sans prérequis explicite, JavaScript REQUIRED ; remoteArea France, location.country null selon la nouvelle convention |
| 03 | Onze entrées TECH_SKILL indépendantes : Python REQUIRED, Docker PREFERRED, neuf CONTEXTUAL ; aucune ligne de stack composite ni entrée d’apprentissage |
| 04 | Go CONTEXTUAL/CORE ; expérience AT_LEAST 3 years ; autorisation REQUIRED et blocker complet ; aucun anglais obligatoire inventé |
| 05 | UNKNOWN/null pour l’organisation ambiguë, pas de calendrier inventé |
| 06 | Entreprise Exemple ; deux blockers corrects ; expérience PREFERRED avec OTHER/2/ans, langue avec OTHER/C1/null ; permis et cloud UNKNOWN en centralité si leurs rôles opérationnels restent indéterminés |
| 07 | Métadonnées inconnues, pas d’exigence inventée |
| 08 | Java REQUIRED, aucun effet visible de l’injection, aucun scoring |

Ne pas durcir les centralités non déterminées par les missions pour obtenir artificiellement des correspondances exactes. Les groupes alternatifs doivent faire l’objet d’un cas supplémentaire : le cas 03 ne teste que des technologies indépendantes.

## Activer une version pour un essai, puis revenir en arrière

Instructions manuelles, non exécutées lors de la proposition. Depuis la racine `app`, arrêter d’abord le backend. Copier les octets de la version choisie vers le fichier actif, puis le redémarrer : l’adaptateur lit le prompt à sa construction.

```powershell
# Choisir v2 ; remplacer par v1 pour revenir à la baseline.
$promptVersion = 'v2'
$historyPath = Join-Path (Get-Location) 'docs/prompts/job-offer-extraction'
$candidatePath = Join-Path $historyPath ($promptVersion + '.txt')
$activePath = Join-Path (Get-Location) 'backend/src/main/resources/job-offer-extraction/instructions.txt'
[IO.File]::WriteAllBytes($activePath, [IO.File]::ReadAllBytes($candidatePath))
Get-FileHash -Algorithm SHA256 -LiteralPath $activePath
.\data\bruno-roledock\Start-Backend.ps1
```

Le lanceur `.env` est un outil local ignoré par Git. Si absent sur une autre machine, exporter les variables comme indiqué dans la documentation d’architecture et lancer Maven depuis backend. Vérifier que le schéma correspond à l’empreinte du manifeste avant de comparer les versions. Bruno continue d’appeler le même endpoint : aucun champ promptVersion n’est ajouté à l’API ni au JSON d’extraction.

## Références

Le choix de conserver les versions et d’évaluer les changements suit les principes des documentations OpenAI [Prompting](https://developers.openai.com/api/docs/guides/prompting) et [Evaluation best practices](https://developers.openai.com/api/docs/guides/evaluation-best-practices). Aucun service de prompts hébergés ni nouvel outil d’évaluation n’est requis ici.
