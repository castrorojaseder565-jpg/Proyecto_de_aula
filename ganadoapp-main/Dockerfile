FROM eclipse-temurin:21-jre
ARG JAR_FILE=target/ganadoapp-1.0.0.jar
COPY ${JAR_FILE} app_bovicontrol.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app_bovicontrol.jar"]
