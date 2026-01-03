# 🚀 Multithreaded Chat Server & Admin API (Java 11)

![Java](https://img.shields.io/badge/Java-11-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![JUnit 5](https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

## 📌 Présentation
Ce projet est une démonstration technique de maîtrise des concepts **Java Core**. Il s'agit d'un serveur de chat multithreadé capable de gérer plusieurs connexions simultanées via des **Sockets TCP**, tout en exposant une API de monitoring en temps réel via un serveur HTTP natif.

L'architecture a été conçue selon les principes **SOLID** et **Clean Architecture** afin de garantir un découplage total entre la couche réseau (Infrastructure) et la logique métier (Domaine).



---

## 🛠️ Maîtrise Technique & Concepts Démontrés

### ☕ Java 11 & Standard Enterprise
* **Inférence de type (`var`) :** Utilisation pour un code plus lisible et moderne.
* **API HttpClient :** Consommation asynchrone de l'API de monitoring.
* **HttpServer natif :** Exposition d'endpoints de statut sans framework externe.
* **API Stream & Lambdas :** Traitement fonctionnel des collections d'utilisateurs et filtrage des messages.

### 🧵 Concurrence & Multithreading
* **ExecutorService :** Gestion d'un pool de threads optimisé pour les connexions entrantes.
* **Thread-Safety :** Utilisation de `ConcurrentHashMap` et de structures de données synchronisées pour prévenir les conditions de course (*race conditions*).

### 🏗️ Architecture & Design Patterns
* **Clean Architecture :** Séparation des responsabilités (Domain, Infrastructure, Core).
* **Command Pattern :** Système de commandes extensible (`/quit`, `/list`) facilitant l'ajout de nouvelles fonctionnalités sans modifier le code existant (**Open/Closed Principle**).
* **Dependency Injection :** Injection manuelle pour un code testable et modulaire.

---

## 🚀 Installation et Utilisation

### Prérequis
* **JDK 11** ou supérieur.
* **Maven 3.6+**.

### 1. Compilation
```bash
mvn clean compile