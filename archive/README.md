# Archive of retired / earlier assets

Recovered from sibling folders that sat **outside** this repository and were never
committed. Kept for reference and portfolio history only — nothing here is used by the
current pipeline.

## `k8s-fleet-config/`

The retired **Rancher Fleet / Kustomize GitOps** deployment configuration.

* **Original repo:** `https://github.com/KithupaG/K8S-Fleet-Deployment-Repo.git`
  (its own git history is not included here; only the working tree).
* **Layout:**
  * `apps/jboss-app/base/` — Kustomize base (`deployment.yaml`, `service.yaml`, `kustomization.yaml`)
  * `apps/jboss-app/overlays/{dev,staging,prod}/` — per-environment image tags + NodePort patches
    (dev `30081`, staging `30082`, prod `30083`)
  * `apps/jboss-app/{dev,staging,prod}.yaml` — Fleet bundle namespace/dir mapping
* **Status:** retired. Replaced by the Helm-from-Harbor approach driven directly by
  the `Jenkinsfile` (see the "Retired" section of the main `README.md`).

## `jboss-container/`

The earlier standalone **JBoss EAP** container build, before the app image moved to the
repository-root `Dockerfile`.

* `Dockerfile` — multi-stage JBoss EAP 8 build.
  **Note:** it contains a hardcoded local admin credential (`Admin#12345`).
  It is legacy, never used by the current pipeline, and is archived verbatim.
* `jboss-deployment.yaml` — a hand-written Kubernetes Deployment/Service manifest
  (no chart; predates `helm/hercey`).
* The 52 MB `hercey.war` build artifact was **not** copied — rebuild it with `mvn package`.
