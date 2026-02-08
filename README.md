# Simple Config Service (No Spring Cloud Config)

Spring Boot config server that serves app+profile configuration from local filesystem.
- **One folder per application**
- App folder contains **either** YAML **or** `.properties` (not both)
- APIs return:
  - merged `.properties`
  - deep merged JSON
  - merged YAML
  - non-merged `sources` JSON

## Run

```bash
./gradlew bootRun
```

Default port: `8888`

Config repo root (default): `./config-repo`
Change via:

```bash
./gradlew bootRun --args='--config.root-dir=/opt/config-repo'
```

## Repo layout

Example (YAML mode):

```
config-repo/
  payment-service/
    application.yml
    application-dev.yml
```

Example (properties mode):

```
config-repo/
  order-service/
    application.properties
    application-uat.properties
```

## Endpoints

### Merged YAML (default)
```bash
curl -s 'http://localhost:8888/config/payment-service/dev'
```

### Merged JSON (deep)
```bash
curl -s 'http://localhost:8888/config/payment-service/dev?format=json'
```

### Merged properties
```bash
curl -s 'http://localhost:8888/config/payment-service/dev?format=properties'
```

### Non-merged sources JSON
```bash
curl -s 'http://localhost:8888/config/payment-service/dev?format=sources'
```

### Clear cache
```bash
curl -X POST 'http://localhost:8888/admin/reload'
```

## Placeholder resolution

By default `${ENV_VAR}` is resolved using the config-service environment.

Disable via:

```bash
curl -s 'http://localhost:8888/config/payment-service/dev?format=json&resolvePlaceholders=false'
```

## Run with Docker (recommended)

```bash
docker build -t simple-config-service .
docker run --rm -p 8888:8888 -v "$PWD/config-repo:/app/config-repo" simple-config-service
```
