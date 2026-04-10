# 📌 Projet Robi – Interpréteur graphique distribué

## 👥 Auteurs
Chemangui_Youssef__Moufid_Yahya__Bachagha_Radia__DIALLO_Diamilatou_Assura  

---

## 📦 Contenu rendu

Le projet est organisé en plusieurs exercices progressifs :

### 🔹 Exercice 1 à 3
Mise en place des bases :
- Manipulation d’objets graphiques (`GElement`, `GRect`, etc.)
- Premières commandes :
  - déplacement (`Translate`)
  - modification de couleur (`SetColor`)

---

### 🔹 Exercice 4
Implémentation du cœur du projet :
- Interpréteur basé sur des S-expressions (`SNode`)
- Mise en place du pattern **Command**
- Gestion d’un environnement (`Environment`)
- Introduction de la classe `Reference`

---

### 🔹 Exercice 5 (partiel)
- Ajout de commandes supplémentaires :
  - création (`NewElement`)
  - suppression (`DelElement`)
- Amélioration de l’interpréteur
- Interface graphique client (`ClientIHM`)
- Communication client / serveur fonctionnelle

⚠️ La partie **Robibot (machine à états)** n’a pas été implémentée.

---

## ⚙️ Éléments techniques importants

### 🧠 Architecture

#### 🔸 Pattern Command
Chaque instruction est représentée par une classe implémentant `Command`.

Exemples :
- `Translate`
- `SetColor`
- `AddElement`
- `DelElement`

---

#### 🔸 Interpréteur
La classe `Interpreter` :
- Analyse les S-expressions (`SNode`)
- Associe chaque instruction à une commande
- Exécute dynamiquement les actions

---

#### 🔸 Environnement
- `Environment` : stockage des variables
- `Reference` : encapsulation des objets manipulés

---

#### 🔸 Instanciation dynamique
Utilisation de la réflexion Java :

```java
getDeclaredConstructor().newInstance();