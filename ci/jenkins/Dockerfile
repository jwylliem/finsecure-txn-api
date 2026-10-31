FROM docker:29.1-cli AS docker
FROM aquasec/trivy:0.76.1 AS trivy

FROM jenkins/jenkins:2.580.2-lts-jdk21
ARG TARGETARCH=arm64
USER root
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
 && curl -fsSL -o /usr/local/bin/kubectl "https://dl.k8s.io/release/v1.35.10/bin/linux/${TARGETARCH}/kubectl" \
 && chmod +x /usr/local/bin/kubectl
COPY --from=docker /usr/local/bin/docker /usr/local/bin/docker
COPY --from=trivy /usr/local/bin/trivy /usr/local/bin/trivy

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
COPY casc.yaml /usr/share/jenkins/ref/casc.yaml
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false" \
    CASC_JENKINS_CONFIG=/usr/share/jenkins/ref/casc.yaml
USER jenkins
