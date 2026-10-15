FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && for attempt in 1 2 3 4 5; do \
         ./mvnw -B -q -DskipTests dependency:go-offline && break; \
         status=$?; \
         if [ "$attempt" -eq 5 ]; then exit "$status"; fi; \
         sleep $((attempt * 5)); \
       done

COPY src src
RUN for attempt in 1 2 3 4 5; do \
      ./mvnw -B -q -DskipTests package && break; \
      status=$?; \
      if [ "$attempt" -eq 5 ]; then exit "$status"; fi; \
      sleep $((attempt * 5)); \
    done

FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/*.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
