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
- `k8s/` - deployment and configmap, `k8s/bootstrap/` namespace, service and jenkins service account
- `ci/` - docker compose for Jenkins, SonarQube and toolbox
- `monitoring/` - prometheus stack values, alert rule, dashboard
- `infra/` - eksctl cluster file

## Pipeline

Checkout, Build, Test, Package, Security Checks (SonarQube, Trivy dependency, Trivy secret), Container Build, Image Scan, Push, Deploy, Smoke Test.
To rollback run "Build with Parameters" and put the old tag in `ROLLBACK_TO`.

## Logs

```
kubectl -n finsecure logs -l app=txn-api --tail=100
```
