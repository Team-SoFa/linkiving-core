# linkiving-core local bundle

## Included files

- `docker-compose.yml`
- `linkiving-core-local-image.tar.gz`

## Run

```bash
docker load -i linkiving-core-local-image.tar.gz
docker compose up -d
```

## Verify

```bash
curl -fsS http://localhost:8080/health-check
```

## Stop

```bash
docker compose down -v
```
