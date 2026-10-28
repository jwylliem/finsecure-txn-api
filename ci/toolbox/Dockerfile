# tools container
FROM maven:3.10.0-eclipse-temurin-21-noble
ARG TARGETARCH=arm64
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip git jq gettext-base less \
 && rm -rf /var/lib/apt/lists/*
RUN ARCH=$([ "$TARGETARCH" = "arm64" ] && echo aarch64 || echo x86_64) \
 && curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-${ARCH}-2.38.5.zip" -o /tmp/aws.zip \
 && unzip -q /tmp/aws.zip -d /tmp && /tmp/aws/install && rm -rf /tmp/aws /tmp/aws.zip
RUN curl -fsSL -o /usr/local/bin/kubectl "https://dl.k8s.io/release/v1.35.10/bin/linux/${TARGETARCH}/kubectl" \
 && chmod +x /usr/local/bin/kubectl \
 && curl -fsSL "https://get.helm.sh/helm-v3.22.1-linux-${TARGETARCH}.tar.gz" | tar xz -C /tmp \
 && mv /tmp/linux-${TARGETARCH}/helm /usr/local/bin/helm \
 && curl -fsSL "https://github.com/eksctl-io/eksctl/releases/download/v0.231.0/eksctl_Linux_${TARGETARCH}.tar.gz" | tar xz -C /usr/local/bin
ENV AWS_PAGER=""
WORKDIR /work
CMD ["sleep", "infinity"]
