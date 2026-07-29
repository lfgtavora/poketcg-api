# poketcg-api

API REST em Spring Boot que expõe dados do dataset [PokemonTCG/pokemon-tcg-data](https://github.com/PokemonTCG/pokemon-tcg-data). Os dados são sincronizados do GitHub para um banco H2 local e servidos em formato compatível com a API Pokémon TCG v2.

Sem autenticação.

## Quick start (Docker)

Requer [Docker](https://docs.docker.com/get-docker/) e Docker Compose.

```bash
docker compose up
```

API em `http://localhost:8080`. Na primeira subida o sync baixa o dataset do GitHub (pode levar alguns minutos). O banco H2 fica no volume `poketcg-data` e persiste entre restarts.

Acompanhe o progresso:

```bash
curl http://localhost:8080/internal/sync/status
curl http://localhost:8080/actuator/health
```

Sem Compose:

```bash
docker build -t poketcg-api .
docker run --rm -p 8080:8080 -v poketcg-data:/data poketcg-api
```

## Pré-requisitos (dev local)

- JDK 17
- Conexão com a internet (o primeiro sync baixa o dataset do GitHub)

## Como rodar (dev)

```bash
# desenvolvimento
./mvnw spring-boot:run

# build
./mvnw clean package

# produção (jar)
java -jar target/poketcg-api-0.0.1-SNAPSHOT.jar

# testes
./mvnw test
```

Servidor padrão: `http://localhost:8080`

Na primeira execução, o sync roda automaticamente e popula o banco em `./data/poketcg` (local) ou `/data/poketcg` (Docker). Um cron diário (04:00) re-sincroniza se houver mudanças no dataset.

## Configuração

Propriedades em `src/main/resources/application.properties`:

| Propriedade | Default | Descrição |
|---|---|---|
| `poketcg.sync.enabled` | `true` | Habilita sync no startup e via cron |
| `poketcg.sync.cron` | `0 0 4 * * *` | Cron do sync diário |
| `poketcg.dataset.owner` | `PokemonTCG` | Owner do repo no GitHub |
| `poketcg.dataset.repo` | `pokemon-tcg-data` | Nome do repo |
| `poketcg.dataset.branch` | `master` | Branch do dataset |
| `spring.datasource.url` | `jdbc:h2:file:./data/poketcg` | Banco H2 em arquivo (`/data/poketcg` no Docker) |

## Endpoints

### Públicos

| Método | Path | Descrição |
|---|---|---|
| `GET` | `/v2/cards` | Lista cards com filtros e paginação |
| `GET` | `/v2/cards/{id}` | Card por ID (ex: `base1-1`) |
| `GET` | `/v2/sets` | Lista sets com filtros e paginação |
| `GET` | `/v2/sets/{id}` | Set por ID (ex: `base1`) |
| `GET` | `/v2/search` | Autocomplete unificado de cards e sets por nome |

### Internos (sem auth)

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/internal/sync/run?force=false` | Dispara sync manual |
| `GET` | `/internal/sync/status` | Status do último sync |

### Actuator

| Método | Path | Descrição |
|---|---|---|
| `GET` | `/actuator/health` | Health check (inclui status do sync) |
| `GET` | `/actuator/info` | Info da aplicação |

## Formato das respostas

**Lista paginada** (`GET /v2/cards`, `GET /v2/sets`):

```json
{
  "data": [ /* objetos do dataset */ ],
  "page": 1,
  "pageSize": 250,
  "count": 1,
  "totalCount": 1
}
```

**Item único** (`GET /v2/cards/{id}`, `GET /v2/sets/{id}`):

```json
{
  "data": { /* objeto do dataset */ }
}
```

**Autocomplete** (`GET /v2/search`):

```json
{
  "data": [
    {
      "type": "card",
      "id": "base1-1",
      "name": "Alakazam",
      "set": { "id": "base1", "name": "Base" },
      "images": {
        "small": "https://images.pokemontcg.io/base1/1.png",
        "large": "https://images.pokemontcg.io/base1/1_hires.png"
      }
    },
    {
      "type": "set",
      "id": "base1",
      "name": "Base",
      "series": "Base",
      "images": {
        "symbol": "https://images.pokemontcg.io/base1/symbol.png",
        "logo": "https://images.pokemontcg.io/base1/logo.png"
      }
    }
  ]
}
```

| Param | Default | Descrição |
|---|---|---|
| `query` | — | Texto digitado (obrigatório, não vazio) |
| `limit` | `10` | Máximo de resultados (1–50) |
| `types` | `card,set` | Filtra entidades: `card`, `set`, ou ambos (`card,set`) |

Busca case-insensitive no nome com match literal (contains/prefix) e fuzzy (Levenshtein, typos como `picachu` → Pikachu). Não usa a sintaxe Lucene do `q`.

**Erro 400** (query inválida):

```json
{
  "error": {
    "code": 400,
    "message": "Unsupported card query field: foo"
  }
}
```

**404**: body vazio quando o ID não existe.

## Query params (listagem)

| Param | Default | Descrição |
|---|---|---|
| `q` | — | Filtro estilo Lucene (ver abaixo) |
| `page` | `1` | Página (mínimo 1) |
| `pageSize` | `250` | Itens por página (1–250) |
| `orderBy` | `id` asc | Campo de ordenação; prefixo `-` = DESC |
| `select` | — | Campos top-level a retornar, separados por vírgula (ex: `id,name,images,set`) |

## Sintaxe do `q`

- `campo:valor` — filtra por campo
- Sem campo explícito, assume `name` (ex: `Alakazam` = `name:Alakazam`)
- Operadores: `AND`, `OR`, `NOT`
- Parênteses: `(types:Fire OR types:Water) AND supertype:Pokemon`
- AND implícito: `name:Alakazam set.id:base1`
- Wildcards: `*` (ex: `name:Ala*`)
- Valores com espaço: `"Full Art"`

### Campos filtráveis — Cards

`id`, `name`, `set.id`, `supertype`, `types`, `subtypes`, `number`, `rarity`, `artist`, `hp`

### Campos filtráveis — Sets

`id`, `name`, `series`, `releaseDate`, `ptcgoCode`, `printedTotal`, `total` (últimos dois exigem inteiro)

### Campos de `orderBy` — Cards

`id`, `name`, `supertype`, `updatedAt`, `rarity`, `artist`, `hp`, `number`, `set.id`

### Campos de `orderBy` — Sets

`id`, `name`, `series`, `updatedAt`, `releaseDate`, `ptcgoCode`, `printedTotal`, `total`

## Exemplos

```bash
# Buscar cards por nome
curl "http://localhost:8080/v2/cards?q=name:Alakazam&page=1&pageSize=10"

# Cards de um set
curl "http://localhost:8080/v2/cards?q=set.id:base1&orderBy=number"

# Só campos necessários
curl "http://localhost:8080/v2/cards?q=set.id:base1&select=id,name,images,number,supertype,set&orderBy=number"

# Card por ID
curl "http://localhost:8080/v2/cards/base1-1"
curl "http://localhost:8080/v2/cards/base1-1?select=id,name,images"

# Sets de uma série, mais recentes primeiro
curl "http://localhost:8080/v2/sets?q=series:Base&orderBy=-releaseDate"

# Set por ID
curl "http://localhost:8080/v2/sets/base1"

# Autocomplete (typeahead)
curl "http://localhost:8080/v2/search?query=ala&limit=10"

# Autocomplete só cards / só sets
curl "http://localhost:8080/v2/search?query=base&types=card"
curl "http://localhost:8080/v2/search?query=base&types=set"

# Sync manual (força reimportação)
curl -X POST "http://localhost:8080/internal/sync/run?force=true"

# Status do sync
curl "http://localhost:8080/internal/sync/status"

# Health
curl "http://localhost:8080/actuator/health"
```

### Resposta do sync (`POST /internal/sync/run`)

```json
{
  "changed": true,
  "revision": "abc123...",
  "cardsProcessed": 15000,
  "setsProcessed": 150,
  "status": "UPDATED",
  "message": "..."
}
```

Status possíveis: `UPDATED`, `UNCHANGED`, `DISABLED`.

### Status do sync (`GET /internal/sync/status`)

```json
{
  "revision": "abc123...",
  "lastSyncAt": "2026-07-18T15:00:00Z",
  "status": "SUCCESS",
  "lastError": null
}
```

Status possíveis: `IDLE`, `RUNNING`, `SUCCESS`, `FAILED`.
