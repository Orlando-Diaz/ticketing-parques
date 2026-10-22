# Imagen en dos etapas: la primera compila con Maven, la segunda solo lleva el JRE y el jar (imagen final pequena).

# --- Etapa 1: compilar ---
FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /app
# Primero solo el pom: mientras no cambien las dependencias, Docker reutiliza esta capa y no las descarga de nuevo
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Los tests ya corren en CI (y localmente con mvn test); aqui solo se empaqueta
RUN mvn -q -B package -DskipTests

# --- Etapa 2: ejecutar ---
FROM eclipse-temurin:21-jre
WORKDIR /app
# No se ejecuta como root: si alguien explotara la app, tendria menos permisos dentro del contenedor
RUN useradd --system --no-create-home --shell /usr/sbin/nologin ticketing
COPY --from=compilacion /app/target/ticketing-parques-*.jar app.jar
USER ticketing
EXPOSE 8080
# Las credenciales (DB_*, JWT_SECRET, ADMIN_PASSWORD) llegan como variables de entorno, nunca dentro de la imagen
ENTRYPOINT ["java", "-jar", "app.jar"]
