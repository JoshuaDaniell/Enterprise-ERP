FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_INVENTORY_API_URL=/api/inventory
ARG VITE_SALES_API_URL=/api/sales
ARG VITE_FINANCE_API_URL=/api/finance
ENV VITE_INVENTORY_API_URL=$VITE_INVENTORY_API_URL
ENV VITE_SALES_API_URL=$VITE_SALES_API_URL
ENV VITE_FINANCE_API_URL=$VITE_FINANCE_API_URL
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
