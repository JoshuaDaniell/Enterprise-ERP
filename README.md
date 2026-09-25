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

From the repository root:

```bash
docker compose -f Docker/docker-compose.yml up --build
```

Open [http://localhost](http://localhost) after the containers become healthy.
Stop the stack with:

```bash
docker compose -f Docker/docker-compose.yml down
```

To remove persisted local database, cache, and event data as well:

```bash
docker compose -f Docker/docker-compose.yml down -v
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

For local development, run MySQL, Redis, and the event broker with Docker
Compose or provide equivalent services through the environment variables used
by each Spring Boot application.

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
