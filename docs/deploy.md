# Deploying the backend (Cloud Run + Neon)

The API runs on Google Cloud Run (region `asia-southeast1`, scales to zero) and stores data in a Neon PostgreSQL
database. `.github/workflows/deploy-backend.yml` deploys it on every push to `main` that touches `backend/**`, `core/**`,
`content/**` or the Gradle build files, and on demand from the Actions tab (**Deploy backend → Run workflow**).
CI builds the same image on every pull request that touches its inputs, so a change that breaks it fails before it reaches `main`.

What a deploy does:

1. Builds `backend/Dockerfile.multistage`, which compiles `:backend` inside Docker with `-PbackendOnly` (no Android SDK).
2. Pushes the image to Artifact Registry as `<region>-docker.pkg.dev/<project>/<repository>/api:<commit sha>`.
3. Deploys a new Cloud Run revision. On start the server runs the Flyway migrations against Neon before it opens its
   port, so a failing migration stops the revision from becoming ready and traffic stays on the previous one.
4. Calls `/health` on the service URL and fails the run unless it answers `200` (database reachable).

GitHub authenticates to Google Cloud with Workload Identity Federation: no service-account keys exist anywhere.

The steps below are done once by the project owner. They use the `gcloud` CLI; replace the values in `<...>`.

## 1. Google Cloud project

```bash
export PROJECT_ID=<project-id>            # e.g. language-platform-prod
export REGION=asia-southeast1
export SERVICE=language-platform-api      # must match the CLOUD_RUN_SERVICE variable if you set one
export GITHUB_REPO=<owner>/<repo>         # e.g. tuantm/language-platform

gcloud projects create "$PROJECT_ID"
gcloud billing projects link "$PROJECT_ID" --billing-account <billing-account-id>
gcloud config set project "$PROJECT_ID"
gcloud services enable run.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com \
  iamcredentials.googleapis.com sts.googleapis.com
export PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format 'value(projectNumber)')"
```

## 2. Artifact Registry

```bash
gcloud artifacts repositories create backend --repository-format docker --location "$REGION" \
  --description "Backend images"
```

Optional, to keep storage within the free tier: in the console, **Artifact Registry → backend → Cleanup policies**, keep
the 10 most recent versions and delete the rest.

## 3. Neon database

1. Sign up at <https://neon.tech> and create a project in region **AWS Asia Pacific (Singapore)**, closest to
   `asia-southeast1`, with PostgreSQL 17.
2. Create the database `language_platform` and a role `app` (**Branches → main → Roles / Databases**).
3. Open **Connection details**, select the database and role, and turn **Connection pooling off**: Flyway needs a direct
   connection (the pooled endpoint runs PgBouncer in transaction mode). Note the host, e.g.
   `ep-cool-name-123456.ap-southeast-1.aws.neon.tech`.
4. The JDBC URL is `jdbc:postgresql://<host>/language_platform?sslmode=require`.

The free plan suspends the compute after 5 minutes idle; the first request after that takes a second or two longer.

## 4. Secrets for the service

The database credentials live in Secret Manager and are mounted as environment variables by Cloud Run, so they never
pass through GitHub.

```bash
printf '%s' 'jdbc:postgresql://<host>/language_platform?sslmode=require' | \
  gcloud secrets create database-url --data-file=-
printf '%s' 'app' | gcloud secrets create database-user --data-file=-
printf '%s' '<neon-password>' | gcloud secrets create database-password --data-file=-
```

To rotate the password later: `printf '%s' '<new>' | gcloud secrets versions add database-password --data-file=-`, then
re-run the workflow (each revision reads `latest` when it starts).

## 5. Service accounts

One account runs the service, another deploys it from GitHub.

```bash
gcloud iam service-accounts create api-runtime --display-name "Cloud Run API runtime"
gcloud iam service-accounts create github-deployer --display-name "GitHub Actions deployer"
export RUNTIME_SA="api-runtime@$PROJECT_ID.iam.gserviceaccount.com"
export DEPLOY_SA="github-deployer@$PROJECT_ID.iam.gserviceaccount.com"

for secret in database-url database-user database-password; do
  gcloud secrets add-iam-policy-binding "$secret" \
    --member "serviceAccount:$RUNTIME_SA" --role roles/secretmanager.secretAccessor
done

gcloud projects add-iam-policy-binding "$PROJECT_ID" --member "serviceAccount:$DEPLOY_SA" --role roles/run.admin
gcloud artifacts repositories add-iam-policy-binding backend --location "$REGION" \
  --member "serviceAccount:$DEPLOY_SA" --role roles/artifactregistry.writer
gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_SA" \
  --member "serviceAccount:$DEPLOY_SA" --role roles/iam.serviceAccountUser
```

## 6. Workload Identity Federation

Lets only jobs of this repository that run on `main` in its `production` environment act as `github-deployer`. The
owner id check stops a new repository that reuses the same name after a rename or deletion.

```bash
export GITHUB_OWNER_ID="$(gh api "repos/$GITHUB_REPO" --jq .owner.id)"

gcloud iam workload-identity-pools create github --location global --display-name "GitHub Actions"
gcloud iam workload-identity-pools providers create-oidc github-repo \
  --location global --workload-identity-pool github \
  --issuer-uri "https://token.actions.githubusercontent.com" \
  --attribute-mapping "google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition "assertion.repository == '$GITHUB_REPO' && assertion.repository_owner_id == '$GITHUB_OWNER_ID' && assertion.environment == 'production' && assertion.ref == 'refs/heads/main'"

gcloud iam service-accounts add-iam-policy-binding "$DEPLOY_SA" \
  --role roles/iam.workloadIdentityUser \
  --member "principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/attribute.repository/$GITHUB_REPO"

echo "projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/providers/github-repo"
```

The last line prints the provider name for the `GCP_WORKLOAD_IDENTITY_PROVIDER` secret.

## 7. GitHub secrets and variables

In the repository, **Settings → Environments → New environment** `production` (the deploy job runs in it; add yourself
as a required reviewer if deploys should wait for approval). Set its **Deployment branches and tags** to
**Selected branches and tags** with the rule `main`, so a workflow edited on another branch cannot deploy to production.
Add these to the environment, or to **Settings → Secrets and variables → Actions** for the whole repository:

| Kind | Name | Value |
| --- | --- | --- |
| Secret | `GCP_WORKLOAD_IDENTITY_PROVIDER` | `projects/<number>/locations/global/workloadIdentityPools/github/providers/github-repo` |
| Secret | `GCP_DEPLOY_SERVICE_ACCOUNT` | `github-deployer@<project-id>.iam.gserviceaccount.com` |
| Secret | `GCP_RUNTIME_SERVICE_ACCOUNT` | `api-runtime@<project-id>.iam.gserviceaccount.com` |
| Variable | `GCP_PROJECT_ID` | `<project-id>` (required) |
| Variable | `GCP_REGION` | Optional, default `asia-southeast1` |
| Variable | `GAR_REPOSITORY` | Optional, default `backend` |
| Variable | `CLOUD_RUN_SERVICE` | Optional, default `language-platform-api` |
| Variable | `CORS_ALLOWED_ORIGINS` | Optional, comma-separated web origins (`https://app.example.com`); the mobile and desktop apps do not need CORS |
| Variable | `AUDIO_BASE_URL` | `https://<r2-public-domain>/audio` (required): public base URL of the Cloudflare R2 bucket or CDN serving listening audio |

The ids are not confidential, but keeping them as secrets keeps them out of the logs.

## 8. First deploy

Run **Actions → Deploy backend → Run workflow** on `main`. The first deploy creates the Cloud Run service with:

- min instances `0` (scales to zero, no idle cost), max instances `2`, 1 vCPU, 512 MiB;
- public access (`--allow-unauthenticated`), `APP_ENV=prod`, database settings from Secret Manager.

Then load the reading and listening tests into Neon from your machine (idempotent, safe to repeat after content
changes):

```bash
APP_ENV=prod DATABASE_URL='jdbc:postgresql://<host>/language_platform?sslmode=require' \
  DATABASE_USER=app DATABASE_PASSWORD='<neon-password>' \
  ./gradlew -PbackendOnly :backend:seedContent
```

Upload each listening section's audio to the bucket at the path its `audioUrl` names in `content/listening/*.json`
(for example `listening/listening-sample-01/section-1.mp3`).

Check it: `curl "$(gcloud run services describe "$SERVICE" --region "$REGION" --format 'value(status.url)')/api/v1/reading/tests"`.

## 9. Budget alert

So a mistake cannot quietly cost money:

```bash
gcloud services enable billingbudgets.googleapis.com
gcloud billing budgets create --billing-account <billing-account-id> \
  --display-name "language-platform" \
  --filter-projects "projects/$PROJECT_ID" \
  --budget-amount 10USD \
  --threshold-rule percent=0.5 --threshold-rule percent=0.9 --threshold-rule percent=1.0
```

Alerts go by email to the billing account admins. A budget only notifies; it does not stop spending. Neon's free plan
has no billing, and its usage page shows how close the project is to the free limits.

## Building the image locally

```bash
docker build -f backend/Dockerfile.multistage -t language-platform-backend .
docker compose up -d db
docker run --rm -p 8080:8080 --add-host=host.docker.internal:host-gateway \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/language_platform language-platform-backend
```

On Apple silicon add `--platform linux/amd64` to `docker build` to produce the same architecture as Cloud Run.

The jar-based `backend/Dockerfile` used by `docker compose` keeps working: build the jar first with
`./gradlew -PbackendOnly :backend:shadowJar`.

## Rolling back

```bash
gcloud run revisions list --service "$SERVICE" --region "$REGION"
gcloud run services update-traffic "$SERVICE" --region "$REGION" --to-revisions <revision>=100
```

Flyway migrations are not rolled back: write a new migration that undoes the change instead.
