#!/bin/sh
set -eu

: "${PORT:=80}"
: "${BACKEND_HOSTPORT:=backend:8080}"

# Restrict envsubst to deployment values so Nginx variables such as $host remain intact.
envsubst '${PORT} ${BACKEND_HOSTPORT}' \
  < /etc/nginx/templates/default.conf.template \
  > /etc/nginx/conf.d/default.conf

exec /docker-entrypoint.sh nginx -g 'daemon off;'
