#!/usr/bin/env bash
# create kubeconfig for jenkins
# usage: ci/make-kubeconfig.sh [api-server-url]
set -euo pipefail
NS=finsecure
SERVER="${1:-$(kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}')}"
CA=$(kubectl -n "$NS" get secret jenkins-deployer-token -o jsonpath='{.data.ca\.crt}')
TOKEN=$(kubectl -n "$NS" get secret jenkins-deployer-token -o jsonpath='{.data.token}' | base64 -d)

mkdir -p ci/secrets
umask 077
cat > ci/secrets/kubeconfig <<KCFG
apiVersion: v1
kind: Config
clusters:
  - name: finsecure
    cluster:
      server: ${SERVER}
      certificate-authority-data: ${CA}
users:
  - name: jenkins-deployer
    user:
      token: ${TOKEN}
contexts:
  - name: jenkins-deployer@finsecure
    context:
      cluster: finsecure
      user: jenkins-deployer
      namespace: ${NS}
current-context: jenkins-deployer@finsecure
KCFG
echo "wrote ci/secrets/kubeconfig for ${SERVER}"
