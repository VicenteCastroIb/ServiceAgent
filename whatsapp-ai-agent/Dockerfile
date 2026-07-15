# Build multi-stage: compila con Maven Wrapper + JDK 21, corre con JRE 21
# (imagen final liviana, sin Maven ni el JDK completo). Ver DEPLOY.md para
# las variables de entorno que hay que cargar en Railway/Render.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cachear las dependencias en su propia capa: solo se re-descargan si
# pom.xml/mvnw cambian, no en cada cambio de código fuente.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src ./src
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS run
WORKDIR /app

# Usuario no-root: buena práctica para contenedores en producción.
RUN useradd --system --create-home appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

# Railway/Render inyectan PORT - application.properties ya lo respeta
# (server.port=${PORT:8080}).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
