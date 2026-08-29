# 🧟 ZombieWaves — Plugin Minecraft

> **Survivez aux vagues des morts-vivants !** ZombieWaves est un plugin de
> **survie par vagues** pour Minecraft **1.21**, inspiré du mode **Zombies
> de Call of Duty**. Affrontez des vagues toujours plus dures de zombies,
> de squelettes et de husks, gagnez de l'or et équipez-vous dans la boutique
> pour repousser toujours plus loin vos limites — seul ou en équipe
> (jusqu'à 20 joueurs).

---

## 🆕 Nouveautés de la v1.4.3

- **🖥️ GUI de sélection des arènes** — la commande `/wave arenas` ouvre désormais
  une interface graphique en jeu : cliquez sur une arène pour la rejoindre,
  sur une partie en cours pour la **spectater**, ou sur le bouton ⭐ pour être
  envoyé dans une arène aléatoire (plusieurs pages supportées !).
- **👀 Mode spectateur** — regardez une partie en cours sans interférer :
  `/wave spectate <arène>` ou cliquez sur l'arène concernée dans le GUI.

  Vous êtes téléportés près de l'arène en mode spectateur, avec une barrière
  pour quitter (`/wave unspectate`); un garde-fou vous ramène à l'arène
  si vous vous en éloignez trop.
- **🛡️ Anti-démarrage de partie simultanée** — si une autre partie est déjà
  en cours sur le serveur, les joueurs en lobby sont prévenus et la partie
  démarre automatiquement dès que celle-ci se termine (plus de partie "fantôme"
  qui ne se lance jamais).
- **🧹 Nettoyage à la déconnexion** — un joueur qui se déconnecte en lobby
  n'y reste plus bloqué : ses données sont nettoyées proprement, il peut
  se reconnecter et rejouer immédiatement.

---

## ✨ Fonctionnalités

| Fonctionnalité | Description |
|------------------|-------------|
| 🌊 **Système de vagues** | Survivez à des vagues de mobs de plus en plus difficiles |
| 🗺️ **Système d'arènes** | Créez des arènes personnalisées (points de spawn, limites) |
| 🎮 **Système de lobby** | Rejoignez une arène avec compte à rebours, jusqu'à 20 joueurs |
| 🖥️ **GUI d'arènes** | Interface en jeu pour rejoindre, spectater ou choisir une arène aléatoire |
| 👀 **Mode spectateur** | Observez une partie en cours sans perturber les joueurs |
| 🚪 **Téléportation auto** | Lobby avant la partie, retour à la sortie après |
| 🎲 **Spawn aléatoire** | Les mobs apparaissent à des endroits et types aléatoires |
| 📈 **Difficulté évolutive** | Santé, dégâts et vitesse augmentent à chaque vague |
| 🪙 **Système d'or** | Gagnez de l'or en tuant des mobs |
| 🛒 **Boutique** | Achetez armes, armures et améliorations |
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
3. **Attendez les joueurs** (1+ pour démarrer, max 20 — solo supporté)
4. **Compte à rebours** démarre automatiquement
5. **La partie démarre:** les joueurs sont téléportés au spawn du jeu
6. **Survivez aux vagues** de zombies, de squelettes et de husks
7. **Quittez à tout moment:** `/wave leave` vous ramène à la sortie

## 🔨 Compilation

```bash
mvn clean package
```

Le JAR se trouve dans `target/ZombieWaves-1.4.3.jar`
