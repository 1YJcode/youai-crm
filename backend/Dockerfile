FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
COPY .m2 /root/.m2/repository
COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY backend/src backend/src
RUN mvn -B -s /root/.m2/settings.xml -f backend/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/backend/target/youai-crm-backend-*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
