# Use an official JRE as a base image
FROM eclipse-temurin:21-jre-alpine

# Add a volume for temporary files
VOLUME /tmp

# Copy the JAR file from the target directory
COPY ./leadsgen-0.0.1-SNAPSHOT.jar app.jar

# Expose the port the application will run on
EXPOSE 8080

# Allow setting additional Java options through an environment variable
ENV JAVA_OPTS=""

# Run the Spring Boot application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app.jar"]
