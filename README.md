# 🧟 ZombieWaves — Plugin Minecraft

> **Survivez aux vagues des morts-vivants !** ZombieWaves est un plugin de
> **survie par vagues** pour Minecraft **1.21**, inspiré du mode **Zombies
> de Call of Duty**. Affrontez des vagues toujours plus dures de zombies,
> de squelettes et de husks, gagnez de l'or et équipez-vous dans la boutique
> pour repousser toujours plus loin vos limites — seul ou en équipe.


---

## 🆕 Nouveautés de la v1.5.0

- **🎛️ Arènes hautement personnalisables** — chaque arène possède désormais
  ses propres réglages : joueurs min/max (`/wave setminplayers`, `/wave setmaxplayers`),
  formule de vagues (`/wave setarenawaves <arène> <base> <incrément>`) et
  **liste de mobs** (`/wave setarenamobs <arène> zombie,skeleton,...`). Tout est
  optionnel : par défaut, chaque arène reprend les valeurs globales de `config.yml`.
- **⚖️ Boutique à prix dynamiques** — les prix de la boutique sont désormais
  calculés automatiquement à partir de l'or réellement gagné par kill
  (`kills:` dans `config.yml` au lieu de prix fixes) : rééquilibrez vos mobs
  et les prix suivent, sans retouche manuelle.

- **🧠 Sélection de mobs pondérée** — le spawn n'est plus tiré au hasard uniforme :
  le poids de spawn de chaque type est combiné à sa puissance (santé × dégâts,,
  si bien que les vagues restent équilibrées et variées à mesure que la difficulté monte.

- **🪙 Or attribué avec précision** — la récompense en or utilise désormais le type
  de mob exact avec lequel il a été généré (plus de devinette basée sur l'apparence,
  même si deux types partagent le même `EntityType`).
- **🚪 Items de lobby fiables** — la barrière **quitter l'arène** (pour tout le monde)
  et le diamant **force-start** (admin, retourné à la case 0 avec garde-fou anti-drop))
  sont identifiés par des données persistantes : clics et chutes accidentelles
  sont gérés proprement.

- **🧹 Nettoyage renforcé** — un joueur qui quitte ou se déconnecte (en lobby
  comme en pleine partie) est retiré proprement : plus de joueur fantôme,
  et si le dernier joueur quitte, la partie s'arrête et l'arène se réinitialise
  immédiatement pour une nouvelle partie.


---

## ✨ Fonctionnalités

| Fonctionnalité | Description |
|------------------|-------------|
| 🌊 **Système de vagues** | Survivez à des vagues de mobs de plus en plus difficiles |
| 🗺️ **Système d'arènes** | Créez des arènes personnalisées (points de spawn, limites) |
| 🎮 **Système de lobby** | Rejoignez une arène avec compte à rebours, jusqu'à 20 joueurs |
| 🖥️ **GUI d'arènes** | Interface en jeu pour rejoindre, spectater ou choisir une arène aléatoire |
| 👀 **Mode spectateur** | Observez une partie en cours sans perturber les joueurs |
| 🎛️ **Arènes personnalisables** | Joueurs min/max, formule de vagues et liste de mobs par arène |
| 🚪 **Téléportation auto** | Lobby avant la partie, retour à la sortie après |
| 🎲 **Spawn aléatoire** | Les mobs apparaissent à des endroits et types aléatoires |
| 📈 **Difficulté évolutive** | Santé, dégâts et vitesse augmentent à chaque vague |
| 🪙 **Système d'or** | Gagnez de l'or en tuant des mobs |
| 🛒 **Boutique** | Achetez armes, armures et améliorations (prix dynamiques équilibrés) |
| 📋 **Scoreboard** | Panneau latéral: vague, kills, or, mobs restants |

## 🎮 Mobs supportés
- **Zombie** (60 % de spawn) — ennemi de base
- **Squelette** (25 % de spawn) — attaquant à distance
- **Husk** (15 % de spawn) — variante résistante

## 📥 Installation

1. Téléchargez le dernier JAR depuis [Releases](../../releases)
2. Placez-le dans le dossier `plugins/` de votre serveur
3. Redémarrez le serveur

## 🎯 Configuration rapide (Admin)

1. **Définissez le lobby et la sortie** (où les joueurs apparaissent/repartent):
   ```
   /wave setlobby         # À votre position (lobby global)
   /wave setexit          # À votre position (point de retour)
   ```

2. **Créez une arène:**
   ```
   /wave createarena monmap
   ```

3. **Définissez le lobby de l'arène** (optionnel — remplace le global):
   ```
   /wave setlobby monmap   # Définit le lobby spécifique à l'arène
   ```

4. **Définissez le spawn de l'arène** (où vont les joueurs au début):
   ```
   /wave setspawn monmap    # À votre position
   ```

5. **Définissez les limites de l'arène** (optionnel):
   ```
   /wave setpos1 monmap    # Regardez le coin 1
   /wave setpos2 monmap    # Regardez le coin 2
   ```

6. **Ajoutez des points de spawn** (où apparaissent les mobs):
   ```
   /wave addspawn monmap   # Regardez l'emplacement, répétez
   ```

## 🎮 Commandes Joueur

| Commande | Description |
|-----------|-------------|
| `/wave join <arène>` | Rejoindre une arène (téléportation vers le lobby) |
| `/wave leave` | Quitter l'arène (retour à la sortie) |
| `/wave arenas` | Ouvre le GUI de sélection des arènes |
| `/wave spectate <arène>` | Spectater une partie en cours |
| `/wave unspectate` | Quitter le mode spectateur |
| `/wave status` | Afficher l'état du jeu/lobby |
| `/wave shop` | Ouvrir la boutique |
| `/wave gold` | Vérifier votre solde d'or |

## 🛠 Commandes Admin

| Commande | Description |
|----------|-------------|
| `/wave createarena <nom>` | Créer une nouvelle arène |
| `/wave deletearena <nom>` | Supprimer une arène |
| `/wave infoarena <nom>` | Afficher les détails d'une arène |
| `/wave selectarena <nom>` | Sélectionner l'arène active |
| `/wave setlobby [arène]` | Définir le lobby (global ou par arène) |
| `/wave setspawn <arène>` | Définir le point de spawn du jeu |
| `/wave setexit` | Définir le point de sortie |
| `/wave setpos1 <arène>` | Définir le coin 1 |
| `/wave setpos2 <arène>` | Définir le coin 2 |
| `/wave addspawn <arène>` | Ajouter un point de spawn de mobs |
| `/wave removespawn <arène>` | Retirer un point de spawn |
| `/wave setminplayers <arène> <n>` | Définir le nombre minimum de joueurs (0 = valeur globale) |
| `/wave setmaxplayers <arène> <n>` | Définir le nombre maximum de joueurs (0 = valeur globale) |
| `/wave setarenamobs <arène> [types]` | Définir les mobs autorisés dans l'arène (liste vide = tous les types) |
| `/wave setarenawaves <arène> <base> <inc>` | Définir la formule de mobs par vague de l'arène (-1 = valeur globale) |
| `/wave reload` | Recharger la configuration |
| `/wave setwave <n>` | Définir la vague courante |
| `/wave forcewave` | Lancer de force la prochaine vague |
| `/wave stop` | Arrêter la partie |

## 🔑 Permissions

- `zombiewaves.admin` — toutes les commandes admin
- `zombiewaves.shop` — accès à la boutique

## 🎯 Comment ça marche

1. **Rejoignez:** `/wave join monmap`
2. **Téléportation au lobby** à l'emplacement de lobby de l'arène
3. **Attendez les joueurs** (min 1 par défaut, max 20 — solo supporté, réglable par arène)
4. **Compte à rebours** démarre automatiquement
5. **La partie démarre:** les joueurs sont téléportés au spawn du jeu
6. **Survivez aux vagues** de zombies, de squelettes et de husks
7. **Quittez à tout moment:** `/wave leave` vous ramène à la sortie

## 🔨 Compilation

```bash
mvn clean package
```

Le JAR se trouve dans `target/ZombieWaves-1.5.0.jar`
