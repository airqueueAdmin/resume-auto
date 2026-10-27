#!/bin/sh
set -eu

: "${PORT:=80}"
: "${BACKEND_BASE_URL:=http://${BACKEND_HOSTPORT:-backend:8080}}"

# Restrict envsubst to deployment values so Nginx variables such as $host remain intact.
envsubst '${PORT} ${BACKEND_BASE_URL}' \
  < /etc/nginx/templates/default.conf.template \
  > /etc/nginx/conf.d/default.conf

exec /docker-entrypoint.sh nginx -g 'daemon off;'
