# deploy/

Kubernetes manifests, an Argo CD application and a Grafana dashboard. Optional for the
demo: nothing else in the repository depends on this directory, and the walkthrough works
without a cluster. Bring it up when the audience runs the platform themselves and wants to
see the change land somewhere.

## Local cluster

    kind create cluster --name northgate

    # the image is built locally, so load it rather than pulling
    docker build -f ratings-service/Dockerfile -t northgate/ratings-service:1.4.2 .
    kind load docker-image northgate/ratings-service:1.4.2 --name northgate

    cp deploy/k8s/secret.example.yaml deploy/k8s/secret.yaml
    kubectl apply -f deploy/k8s/namespace.yaml
    kubectl apply -f deploy/k8s/
    kubectl -n northgate rollout status deploy/ratings-service

    kubectl -n northgate port-forward svc/ratings-service 8080:80
    curl localhost:8080/api/ratings/NG-1001

`secret.yaml` is gitignored. `secret.example.yaml` holds the same fabricated warehouse
token that is hardcoded in `WarehouseClient` and baked into the `Dockerfile` — the secret
object exists and the code ignores it, which is the state a lot of services are actually
in.

The deployment has no `securityContext` and the image runs as root. That is deliberate and
is part of what the Prisma export flags.

## Argo CD

    kubectl create namespace argocd
    kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
    kubectl -n argocd rollout status deploy/argocd-server

Set `repoURL` in `deploy/argocd/application.yaml` to your fork, then:

    kubectl apply -f deploy/argocd/application.yaml
    kubectl -n argocd port-forward svc/argocd-server 8081:443

Initial password:

    kubectl -n argocd get secret argocd-initial-admin-secret \
      -o jsonpath='{.data.password}' | base64 -d

Sync is automated with self-heal, so a merged change to `deploy/k8s/` shows up in the Argo
UI without another command. That is the only reason Argo is here: it closes the line from
merged pull request to running service.

## Grafana

`grafana/ratings-remediation-dashboard.json` imports through **Dashboards → New →
Import**. It expects a Prometheus data source and the following series, none of which this
repository emits — wire them from your own GitHub, Azure DevOps and Devin usage data, or
leave the panels empty and use the dashboard as the shape of the argument:

| Series | Meaning |
| ------ | ------- |
| `northgate_remediation_prs_merged_total` | Remediation pull requests merged |
| `northgate_scanner_findings_closed_total` | Scanner findings closed |
| `northgate_remediation_cost_acu_total` | ACUs spent in remediation sessions |
| `northgate_scanner_findings_open{group}` | Open findings by root cause group |
| `northgate_finding_to_merge_seconds_bucket` | Finding to merged fix, histogram |
| `northgate_remediation_prs_open{state}` | Open pull requests by review state |

The dashboard measures merged work, not opened pull requests, and it puts cost per closed
finding next to cost per merged pull request. Those two diverge sharply here: one base
image change closes 67 findings, so the per-finding number is dominated by whether the
grouping was done properly during triage. The last panel tracks the review queue, because
once fixes are cheap that is where the work backs up.
