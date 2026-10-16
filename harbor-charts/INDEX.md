# Harbor Chart Archive

Snapshot of the Helm charts published to **Harbor** (OCI) so the packaged releases
are preserved in the repository alongside the chart source in [`../helm/hercey`](../helm/hercey).

* **Registry:** `192.168.56.50` (Harbor, plain HTTP)
* **OCI path:** `oci://192.168.56.50/hercey/charts/hercey`
* **Chart source of truth:** `helm/hercey` (packaged by the Jenkins pipeline)
* **Fetched on:** 2026-10-16

Each chart is stored twice:

* `hercey-<version>.tgz` - the exact OCI artifact pulled from Harbor.
* `extracted/<version>/hercey/` - the unpacked chart (`Chart.yaml`, `values.yaml`, `templates/`) for easy review.

## Catalog

| Version | App version | Pushed (UTC)          | Digest                                                             |
|---------|-------------|-----------------------|-------------------------------------------------------------------|
| 1.0.4   | 1.0.4       | 2026-10-15T03:56:58Z  | `sha256:6df8262ce23589a10253bb51b8724b190d339daa0405c5e014442c0f58d8cb27` |
| 1.0.2   | 1.0.2       | 2026-10-13T09:21:33Z  | `sha256:961c3bddf291bf44e9516d48ffd4d61ee73827efd0943509b095fee013b2ec94` |

> Both charts are content-identical to `helm/hercey` (only the packaged `version` /
> `appVersion` differ, set by the pipeline at release time).

## Pulling / verifying

```bash
# Pull a specific version straight from Harbor
helm pull oci://192.168.56.50/hercey/charts/hercey --version 1.0.4 --plain-http -d harbor-charts

# Inspect the archived copy
helm show chart   harbor-charts/hercey-1.0.4.tgz
helm show values  harbor-charts/hercey-1.0.4.tgz
helm template jboss-app harbor-charts/hercey-1.0.4.tgz
```

## Related Harbor repositories

| Repository              | Contents                                  |
|-------------------------|-------------------------------------------|
| `hercey/charts/hercey`  | Helm charts (this folder)                 |
| `hercey/hercey`         | Production application images (`1.0.x`)   |
| `hercey/jboss-app`      | Application images                        |

> The `hercey-dev` and `hercey-staging` Harbor projects referenced by the pipeline no
> longer exist on the registry; only the production `hercey` project remains.
