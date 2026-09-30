# Star Enterprises ERP

Star Enterprises is a modular business dashboard for inventory, sales, customer
orders, invoicing, and payment tracking. The React dashboard presents a single
lightweight workspace while Spring Boot applications handle each business area.

## Architecture

```text
                         +----------------------+
                         |  React Dashboard     |
                         |  Vite + Tailwind CSS |
                         +----------+-----------+
                                    |
               +--------------------+--------------------+
               |                    |                    |
       +-------v-------+    +-------v-------+    +-------v-------+
       | Inventory API |    |   Sales API   |    |  Finance API  |
       |    :18082     |    |    :18081     |    |    :18083     |
       +-------+-------+    +-------+-------+    +-------+-------+
               |                    |                    |
               +--------------------+--------------------+
                                    |
                         +----------v-----------+
                         | MySQL + Redis +      |
                         | event infrastructure|
                         +----------------------+
```

## Technology stack

- **Frontend:** React, Vite, Tailwind CSS, Axios
- **Backend:** Java, Spring Boot, Spring Data JPA, Spring Security
- **Data:** MySQL 8 and Redis
- **Operations:** Docker Compose, Nginx, Maven

## Prerequisites

- Docker Desktop with Docker Compose
- Node.js 20+ and npm (for local frontend development)
- Java 17+ and Maven (for local backend development)

## Quick start with Docker Compose

Copy `Docker/.env.example` to `Docker/.env`, replace every placeholder with a
strong local secret, then run from the repository root:

```bash
docker compose --env-file Docker/.env -f Docker/docker-compose.yml up --build
```

Open [http://localhost](http://localhost) after the containers become healthy.
Stop the stack with:

```bash
docker compose --env-file Docker/.env -f Docker/docker-compose.yml down
```

To remove persisted local database, cache, and event data as well:

```bash
docker compose --env-file Docker/.env -f Docker/docker-compose.yml down -v
```

## Local development

### Dashboard

```bash
cd Frontend/frontend-dashboard
npm install
copy .env.example .env
npm run dev
```

The Vite development server runs at `http://localhost:5173` and proxies API
requests to the local backend ports.

### Backend applications

Each service can be started from its project directory:

```bash
cd Microservices/inventory-service/inventory-service
mvn spring-boot:run

cd Microservices/sales-service
mvn spring-boot:run

cd Microservices/finance-service
mvn spring-boot:run
```

All three applications require `APPLICATION_SECURITY_JWT_SECRET_KEY` at
startup. Set it in the environment of each IDE run configuration (IntelliJ:
**Run → Edit Configurations → Environment variables**) or in the shell before
starting Maven. Use the same randomly generated value for Inventory, Sales,
and Finance; use at least 32 characters and keep it private. For PowerShell,
generate a value once and retain it securely:

```powershell
$env:APPLICATION_SECURITY_JWT_SECRET_KEY = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Then start each service from that PowerShell session, or copy the exact value
into each IDE run configuration. Do not add a development fallback secret to
the application files. Inventory also needs `USER_PROVISIONING_KEY` set when
using its controlled user-provisioning endpoint.

For local development, run MySQL, Redis, and the event broker with Docker
Compose or provide equivalent services through the environment variables used
by each Spring Boot application.

### Authentication and user provisioning

Login accepts only `username` and `password`. Roles are stored with the user in
the Inventory service database and are copied into the signed JWT. No demo
accounts or public self-registration are created. Set the same `JWT_SECRET_KEY`
for all three services; it must contain at least 32 bytes. The Docker Compose
configuration also requires a separate `USER_PROVISIONING_KEY`. Provision
each account through the controlled Inventory endpoint:

```bash
curl -X POST http://localhost:18082/api/auth/provision \
  -H "Content-Type: application/json" \
  -H "X-Provisioning-Key: $USER_PROVISIONING_KEY" \
  -d '{"username":"inventory","password":"<choose-a-password>","role":"ROLE_INVENTORY_USER"}'
```

Use `ROLE_SALES_USER` and `ROLE_FINANCE_USER` for the other accounts. The
former development seed used `inventory/inventory123`, `sales/sales123`, and
`finance/finance123`; those credentials are no longer created by the source
code and should only work if those records already exist in your database.
Existing legacy roles are migrated on Inventory startup: `ROLE_ADMIN` and
`ROLE_WAREHOUSE_MANAGER` become `ROLE_INVENTORY_USER`; `ROLE_VIEWER` is
disabled and mapped to the inventory enum value so the database contains only
the three supported roles.

### Event flow

Sales writes `ORDER_PLACED` and `ORDER_STATUS_CHANGED` to its transactional
outbox. Inventory consumes placed orders, reserves stock within one database
transaction using optimistic locking, and records `STOCK_RESERVED` or
`STOCK_REJECTED` in its outbox. Sales maps those results to `CONFIRMED` or
`REJECTED`; Finance consumes the resulting events idempotently. The order
workflow remains Kafka-based. Consumers use manual offset commits, bounded
retries, and per-topic dead-letter topics (`<topic>.DLT`). Core topics use three
partitions, replication factor one for the local single-broker Compose setup,
and seven-day retention; dead-letter topics retain records for fourteen days.

The MySQL initialization script creates separate Sales and Finance databases
and grants the configured application user access to them on a new volume. For
an existing `mysql_data` volume, create those databases and grants manually or
use a fresh development volume before starting the services.

## Port map

| Component | Local port | Purpose |
| --- | ---: | --- |
| Dashboard container | 80 | Production web UI |
| Vite development server | 5173 | Local frontend development |
| Inventory API | 18082 | Inventory and stock management |
| Sales API | 18081 | Customers and orders |
| Finance API | 18083 | Invoices and payments |
| MySQL | 3307 | Relational data |
| Redis | 6379 | Cache |
| Event broker | 9092 | Application events |

## Project structure

```text
.
├── Docker/
│   └── docker-compose.yml
├── Frontend/
│   └── frontend-dashboard/
│       ├── src/
│       │   ├── modules/auth/
│       │   ├── modules/inventory/
│       │   ├── modules/sales/
│       │   └── modules/finance/
│       └── package.json
└── Microservices/
    ├── inventory-service/
    ├── sales-service/
    └── finance-service/
```

## Environment variables

The Compose file provides development defaults. Set these in a root `.env`
file when overriding them:

| Variable | Description |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL root password |
| `MYSQL_USER` | Application database user |
| `MYSQL_PASSWORD` | Application database password |
| `JWT_SECRET_KEY` | Shared signing secret for authenticated requests |
| `VITE_INVENTORY_API_URL` | Inventory API base URL for the frontend |
| `VITE_SALES_API_URL` | Sales API base URL for the frontend |
| `VITE_FINANCE_API_URL` | Finance API base URL for the frontend |

Never commit real credentials. Use the provided `.env.example` files as
templates for local configuration.

## Screenshots

Screenshots can be added here as the dashboard evolves:

- Dashboard overview
- Inventory workspace
- Sales and orders workspace
- Finance and billing workspace

## License

This project is currently provided for internal use. Add the organization’s
approved license text before distributing it publicly.
