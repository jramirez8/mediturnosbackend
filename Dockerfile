FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew clean bootJar -x test

RUN JAR_FILE=$(find build/libs -type f -name "*.jar" ! -name "*plain.jar" | head -n 1) && cp "$JAR_FILE" app.jar


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080

CMD ["java", "-Xms128m", "-Xmx512m", "-XX:+UseSerialGC", "-jar", "app.jar"]