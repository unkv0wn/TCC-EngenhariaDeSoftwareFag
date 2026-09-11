# ── build stage ──────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Nada de `dependency:go-offline` aqui — o Flyway declara backends opcionais (S3,
# Google Cloud Storage) que a gente não usa, mas que o go-offline insiste em
# resolver mesmo assim (árvores enormes de Guava/gRPC/Netty). O `mvn package` de
# baixo já resolve exatamente o necessário, sem esse desperdício.
COPY pom.xml .
COPY src ./src
# spring-boot-maven-plugin deixa o .jar.original (pre-repackage) do lado do .jar
# executável final — removido aqui pra sobrar só um arquivo pro COPY --from de baixo.
RUN mvn -B clean package -DskipTests && rm -f target/*.jar.original

# ── runtime stage ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/target/routewise-backend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
