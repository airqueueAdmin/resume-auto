FROM node:24-alpine AS build

WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.29-alpine

COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.sh /usr/local/bin/render-nginx-config
RUN chmod +x /usr/local/bin/render-nginx-config
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 10000
ENTRYPOINT ["/usr/local/bin/render-nginx-config"]
