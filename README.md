# Hercey Application - CI/CD Pipeline

Jenkins CI/CD for the **Hercey JBoss/WildFly application**. Every build produces a versioned
WAR, packages it into a container image **and** a Helm chart, pushes both to **Harbor**, and
deploys to Kubernetes with Helm. Harbor (OCI) is the **single source of truth** for both the
image and the chart.

---

## Architecture & Tech Stack

* **CI/CD:** Jenkins (Multibranch Pipeline)
* **Artifact registry / source of truth:** Harbor (OCI) - container images + Helm charts
* **Packaging / deployment:** Helm (`helm/hercey`)
* **Kubernetes platform:** K3s single-node cluster
* **Application:** JBoss Enterprise / WildFly container (`jboss-app`)

---

## Delivery Flow

1. Build the versioned WAR with Maven.
2. Build the WildFly container image.
3. Push the image to Harbor: `192.168.56.50/<project>/hercey:<image-tag>`.
4. Package the Helm chart and push it to Harbor OCI: `oci://192.168.56.50/<project>/charts/hercey:<version>` (a `charts` subpath keeps chart tags separate from image tags).
5. Deploy from Harbor with `helm upgrade --install` into the environment namespace.
6. Post-deploy security/health checks (Trivy audit, health test, OWASP ZAP DAST).

---

## Repository Structure

```text
.
├── Jenkinsfile                 # Multibranch pipeline definition
├── Dockerfile                  # WildFly image build
├── pom.xml                     # Maven WAR build
├── helm/
│   └── hercey/                 # Helm chart SOURCE (packaged + pushed to Harbor by CI)
│       ├── Chart.yaml
│       ├── values.yaml
│       └── templates/
│           ├── deployment.yaml
│           └── service.yaml
├── harbor-charts/              # Charts PULLED from Harbor OCI (packaged artifacts)
│   ├── INDEX.md                # Catalog: versions, digests, push dates
│   ├── hercey-<version>.tgz    # Exact OCI artifacts
│   └── extracted/<version>/    # Unpacked charts for review
├── archive/                    # Retired/earlier assets recovered from outside the repo
│   ├── k8s-fleet-config/       # Old Fleet + Kustomize GitOps manifests
│   └── jboss-container/        # Old standalone JBoss EAP image build
├── scripts/                    # Pipeline helper scripts (TruffleHog report)
├── templates/                  # Trivy HTML report template
├── Docs/                       # App/domain documentation
├── trufflehog-config.yaml      # Secret-scan config
├── .trivyignore                # Known/accepted findings
└── src/                        # Application source
```

---

## Environments

The branch selects the environment automatically (see the `Load environment config` stage):

| Branch    | Namespace | Harbor project  | Image tag          | NodePort |
|-----------|-----------|-----------------|--------------------|----------|
| `dev`     | `dev`     | `hercey-dev`    | `dev-1.0.<build>`  | 30081    |
| `staging` | `staging` | `hercey-staging`| `staging-1.0.<build>` | 30082 |
| `main`    | `prod`    | `hercey`        | `1.0.<build>`      | 30083    |

`staging` and `main` deploy automatically; `dev` builds and pushes artifacts only.

---

## Deploy & Rollback

Jenkins performs the deploy from Harbor, for example:

```bash
helm upgrade --install jboss-app \
  oci://192.168.56.50/<project>/charts/hercey \
  --version <version> \
  --namespace <env> --create-namespace \
  --set image.repository=192.168.56.50/<project>/hercey \
  --set image.tag=<image-tag> \
  --set service.type=NodePort --set service.nodePort=<nodePort> \
  --plain-http --wait --rollback-on-failure --take-ownership --force-conflicts
```

Roll back a release:

```bash
helm rollback jboss-app -n <env>
helm history jboss-app -n <env>
```

---

## Harbor chart archive (`harbor-charts/`)

Harbor is the source of truth for packaged charts, so every published chart is also
vendored into this repository for review:

```bash
helm pull oci://192.168.56.50/hercey/charts/hercey --version 1.0.4 --plain-http -d harbor-charts
helm template jboss-app harbor-charts/hercey-1.0.4.tgz
```

See [`harbor-charts/INDEX.md`](harbor-charts/INDEX.md) for the full catalog (versions,
digests, push dates). The unpacked chart is identical to the source in `helm/hercey`.

---

## Archive (`archive/`)

Retired/earlier assets recovered from outside this repository, kept for reference:

* **`archive/k8s-fleet-config/`** - the old Rancher Fleet + Kustomize GitOps manifests
  (per-env overlays with the same NodePorts as today).
* **`archive/jboss-container/`** - the earlier standalone JBoss EAP image build and a
  hand-written Kubernetes manifest.

See [`archive/README.md`](archive/README.md) for provenance.

---

## Retired

Rancher Fleet and the `K8S-Fleet-Deployment-Repo` GitOps repository are retired. Deployment is
now driven directly by Jenkins using Helm, pulling both the image and the chart from Harbor.
A copy of the retired GitOps manifests is preserved in [`archive/k8s-fleet-config/`](archive/k8s-fleet-config/).
