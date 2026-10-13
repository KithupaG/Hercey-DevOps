# Hercey

A Java web application built with Jakarta Servlet, Hibernate ORM, and Jersey REST API.

## Tech Stack

- **Java 17** + **Maven** (WAR packaging)
- **Tomcat Embedded 10.1** (servlet container)
- **Jakarta Servlet 6.1** + **Jersey 3.1** (REST)
- **Hibernate ORM 6.1** + **MySQL**
- **OpenPDF**, **Jakarta Mail**, **GSON**

## Build & Run

```bash
mvn clean package
mvn tomcat7:run-war
```

## Cloud Tunnel
```cloudflared tunnel --config C:\Users\nimut\.cloudflared\config.yml run hercey
```bash
# Kubernetes Fleet GitOps Deployment Repository

This repository implements a GitOps-based deployment pipeline for the **JBoss Application (`jboss-app`)** using **Rancher Fleet**, **Kustomize**, and **K3s**. It manages multi-environment deployments (`staging` and `prod`) from a single centralized repository.

---

## Architecture & Tech Stack

* **Orchestration / GitOps:** Rancher Fleet (`fleet.yaml`)
* **Configuration Management:** Kustomize (Base and Environment Overlays)
* **Kubernetes Platform:** K3s Single-Node Cluster
* **Application:** JBoss Enterprise / WildFly Container (`jboss-app`)

---

## Repository Structure

```text
.
├── apps/
│   └── jboss-app/
│       ├── fleet.yaml               # Fleet multi-target configuration
│       ├── base/                    # Shared Kubernetes manifests
│       │   ├── deployment.yaml
│       │   ├── service.yaml
│       │   └── kustomization.yaml
│       └── overlays/                # Environment-specific configurations
│           ├── staging/
│           │   └── kustomization.yaml
│           └── prod/
│               └── kustomization.yaml
```

---

## Fleet Target Configuration (`fleet.yaml`)

Fleet uses `targetCustomizationMode: AllMatches` to evaluate both staging and production targets concurrently against the cluster labels:

```yaml
targetCustomizationMode: AllMatches
targetCustomizations:
  - name: staging
    clusterSelector:
      matchLabels:
        staging-env: "true"
    kustomize:
      dir: overlays/staging
    namespace: staging

  - name: prod
    clusterSelector:
      matchLabels:
        prod-env: "true"
    kustomize:
      dir: overlays/prod
    namespace: prod
```

---

## Cluster Prerequisites & Labeling

For Fleet to successfully target and deploy resources to your K3s cluster, ensure the cluster object in the `fleet-local` namespace is properly labeled with both environment flags:

```bash
kubectl label clusters.fleet.cattle.io local staging-env=true prod-env=true -n fleet-local --overwrite
```

Verify the labels have been successfully applied:
```bash
kubectl get clusters.fleet.cattle.io local -n fleet-local -o jsonpath='{.metadata.labels}'
```

---

## Verifying Deployments

Once Fleet syncs the repository, you can verify the status of the bundle deployments across your cluster:

```bash
kubectl get bundledeployments -A | grep hercey-k8s-deployment
```

Check that the pods and deployments are running successfully in both namespaces:

```bash
kubectl get deploy,pods -n staging
kubectl get deploy,pods -n prod
```