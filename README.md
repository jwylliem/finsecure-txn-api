# FinSecure Txn API

Capstone project - Jonathan Wylliem

Spring Boot API for validate transactions. Built and deployed by Jenkins to Amazon EKS.

## Endpoints

- `GET /health` - return status and version
- `POST /validate` - validate a transaction (header `X-API-Key` needed)

Rules: id, fromAccount, toAccount are required, amount must be positive and not more than the limit (5000), currency must be SGD or USD.
Valid return 200, invalid return 422 with the errors.

## How to run

Build and test (in toolbox container):

```
./mvnw verify
```

Run with docker:

```
docker build -t finsecure-txn-api:1.0.0 .
docker run -p 8080:8080 -e API_KEY=test finsecure-txn-api:1.0.0
curl localhost:8080/health
```

## Folders

- `src/` - code and tests
- `k8s/` - deployment, configmap, hpa and pdb, `k8s/bootstrap/` namespace, service and jenkins service account
- `ci/` - docker compose for Jenkins, SonarQube and toolbox
- `monitoring/` - prometheus stack values, alert rules, dashboard
- `infra/` - eksctl cluster file

## Pipeline

Checkout, Build, Test, Package, Security Checks (SonarQube, Trivy dependency, Trivy secret), Container Build, Image Scan, Push, Deploy, Smoke Test.
To rollback run "Build with Parameters" and put the old tag in `ROLLBACK_TO`.

## Logs and troubleshooting

Logs are JSON (ECS format), so can use jq to filter.

```
# last 100 lines of all pods
kubectl -n finsecure logs -l app=txn-api --tail=100 --prefix
# only invalid transaction
kubectl -n finsecure logs -l app=txn-api --tail=500 | jq -r 'select(.message|test("is invalid")) | ."@timestamp" + " " + .message'
# only warning and error
kubectl -n finsecure logs -l app=txn-api --tail=500 | jq 'select(.log.level == "WARN" or .log.level == "ERROR")'
# logs of a pod that crash (previous container)
kubectl -n finsecure logs <pod> --previous
```

When an alert come:

1. `TxnApiHighInvalidRate` - check the invalid logs above, see which error is most common (wrong currency, over limit) and which client send it.
2. `TxnApiPodsUnavailable` - `kubectl -n finsecure get pods` and `kubectl -n finsecure describe pod <pod>`, look at events (OOMKilled, probe failed, image pull error). If it start after a deploy, rollback with `ROLLBACK_TO`.
3. `TxnApiHighLatency` - check CPU panel and `kubectl -n finsecure get hpa`, if already 4 pods and CPU still high, raise maxReplicas or the CPU limit.
