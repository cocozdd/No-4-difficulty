param(
  [string]$BackendTag = "campus-market-backend:dev",
  [string]$FrontendTag = "campus-market-frontend:dev"
)

Write-Host "[1/3] Building backend image: $BackendTag"
docker build -t $BackendTag ./backend
if ($LASTEXITCODE -ne 0) { Write-Error "Backend image build failed"; exit 1 }

Write-Host "[2/3] Building frontend image: $FrontendTag"
docker build -t $FrontendTag ./frontend
if ($LASTEXITCODE -ne 0) { Write-Error "Frontend image build failed"; exit 1 }

Write-Host "[3/3] Loading images into minikube"
minikube image load $BackendTag
if ($LASTEXITCODE -ne 0) { Write-Error "Failed to load backend image into minikube"; exit 1 }

minikube image load $FrontendTag
if ($LASTEXITCODE -ne 0) { Write-Error "Failed to load frontend image into minikube"; exit 1 }

Write-Host "Done. Images are loaded into minikube."
