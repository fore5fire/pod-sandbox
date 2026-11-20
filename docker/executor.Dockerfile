FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y python3 python3-pip && rm -rf /var/lib/apt/lists/*
RUN mkdir /workspace
COPY target/pod-sandbox.jar /app/pod-sandbox.jar
ENTRYPOINT ["java", "-cp", "/app/pod-sandbox.jar", "dev.podsandbox.executor.ExecutorMain"]
