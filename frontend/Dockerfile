# syntax=docker/dockerfile:1.4

##
# Build stage: compile static assets with Vite
##
FROM node:18-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

##
# Runtime stage: serve with nginx
##
FROM nginx:1.25-alpine AS runtime
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
