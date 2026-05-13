# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
RUN apk add --no-cache maven
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8443
ENTRYPOINT ["sh", "-c", "rm -f /app/keystore.p12 && keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore /app/keystore.p12 -validity 3650 -storepass ${SSL_PASSWORD} -keypass ${SSL_PASSWORD} -dname 'CN=localhost, OU=Filmer, O=Filmer, L=Riyadh, ST=Riyadh, C=SA' && java -jar app.jar"]
