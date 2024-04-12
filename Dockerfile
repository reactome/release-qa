ARG REPO_DIR=/opt/release-qa


# ===== stage 1 =====
FROM maven:3.6.3-openjdk-11 AS setup-env

ARG REPO_DIR

WORKDIR ${REPO_DIR}

COPY . .

SHELL ["/bin/bash", "-c"]

# run lint if container started
ENTRYPOINT []

CMD mvn -B -q checkstyle:check | \
    grep -i --color=never '\.java\|failed to execute goal' > lint.log && \
    exit 1 || \
    exit 0


# ===== stage 2 =====
FROM setup-env AS build-jar

RUN mvn clean package


# ===== stage 3 =====
FROM eclipse-temurin:11-jre-focal

ARG REPO_DIR

ARG TARGET_DIR=${REPO_DIR}/target

WORKDIR ${REPO_DIR}/target

COPY --from=build-jar ${TARGET_DIR}/QA_SkipList ./QA_SkipList/
COPY --from=build-jar ${TARGET_DIR}/release-qa-*-exec.jar .
COPY --from=build-jar ${TARGET_DIR}/resources ./resources/
