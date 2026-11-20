FROM eclipse-temurin:21-jre
COPY target/pod-sandbox.jar /app/pod-sandbox.jar
ENTRYPOINT ["java", "-cp", "/app/pod-sandbox.jar", "dev.podsandbox.controller.ControllerMain"]
