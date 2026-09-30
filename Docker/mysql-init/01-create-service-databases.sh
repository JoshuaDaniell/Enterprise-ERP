#!/bin/bash
set -eu

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS erp_sales_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS erp_finance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON erp_sales_db.* TO '${MYSQL_USER}'@'%';
GRANT ALL PRIVILEGES ON erp_finance_db.* TO '${MYSQL_USER}'@'%';
FLUSH PRIVILEGES;
SQL
