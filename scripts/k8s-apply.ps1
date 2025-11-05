param(
  [string]$Namespace = "campus-market"
)

Write-Host "Using namespace: $Namespace"
$null = kubectl get ns $Namespace 2>$null
if ($LASTEXITCODE -ne 0) {
  Write-Host "Namespace not found. Creating..."
  kubectl create ns $Namespace
  if ($LASTEXITCODE -ne 0) { Write-Error "Failed to create namespace"; exit 1 }
}

function Apply-OrExit {
  param([string]$Path)
  kubectl apply -f $Path
  if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to apply $Path"
    exit 1
  }
}

Write-Host "Applying ConfigMap & Secret"
Apply-OrExit ./deploy/k8s/base/backend-config.yaml
Apply-OrExit ./deploy/k8s/base/backend-secret.yaml

Write-Host "Applying MySQL (StatefulSet + Service)"
Apply-OrExit ./deploy/k8s/base/mysql.yaml

Write-Host "Applying Redis"
Apply-OrExit ./deploy/k8s/base/redis.yaml

Write-Host "Applying Kafka"
Apply-OrExit ./deploy/k8s/base/kafka.yaml

Write-Host "Applying MinIO"
Apply-OrExit ./deploy/k8s/base/minio.yaml

Write-Host "Applying Backend"
Apply-OrExit ./deploy/k8s/base/backend-deployment.yaml

Write-Host "Applying Frontend"
Apply-OrExit ./deploy/k8s/base/frontend-deployment.yaml

Write-Host "All manifests applied. Use 'kubectl get pods -n $Namespace' to check status."
