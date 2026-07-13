# Multi-stage build pour optimiser la taille de l'image
FROM maven:3.8.4-openjdk-17 AS build

# Définir le répertoire de travail
WORKDIR /app

# Copier le fichier pom.xml
COPY pom.xml .

# Télécharger les dépendances (cache layer)
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Compiler l'application
RUN mvn clean package -DskipTests

# Stage de production
FROM openjdk:17-jre-slim

# Créer un utilisateur non-root
RUN groupadd -r afra7kom && useradd -r -g afra7kom afra7kom

# Définir le répertoire de travail
WORKDIR /app

# Créer le répertoire pour les uploads
RUN mkdir -p /app/uploads && chown -R afra7kom:afra7kom /app

# Copier le JAR depuis le stage de build
COPY --from=build /app/target/afra7kom-backend-*.jar app.jar

# Changer les permissions
RUN chown afra7kom:afra7kom app.jar

# Passer à l'utilisateur non-root
USER afra7kom

# Exposer le port
EXPOSE 8080

# Variables d'environnement par défaut
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]

















