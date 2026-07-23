# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x mvnw \
	&& ./mvnw -q -B -DskipTests package \
	&& mv target/poketcg-api-*.jar /app/app.jar

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/* \
	&& groupadd --system spring \
	&& useradd --system --gid spring --home-dir /app --shell /usr/sbin/nologin spring \
	&& mkdir -p /data \
	&& chown spring:spring /data

COPY --from=build --chown=spring:spring /app/app.jar /app/app.jar

USER spring
EXPOSE 8080

ENV SPRING_DATASOURCE_URL=jdbc:h2:file:/data/poketcg;AUTO_SERVER=TRUE

HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=3 \
	CMD curl -fsS http://localhost:8080/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
