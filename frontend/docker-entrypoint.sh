#!/bin/sh
set -eu

: "${PORT:=80}"
: "${BACKEND_BASE_URL:=http://${BACKEND_HOSTPORT:-backend:8080}}"
: "${GA_MEASUREMENT_ID:=}"

case "$GA_MEASUREMENT_ID" in
  '') ;;
  G-?*)
    case "$GA_MEASUREMENT_ID" in
      *[!A-Za-z0-9-]*)
        echo "Ignoring invalid GA_MEASUREMENT_ID; expected letters and numbers after G-." >&2
        GA_MEASUREMENT_ID=''
        ;;
    esac
    ;;
  *)
    echo "Ignoring invalid GA_MEASUREMENT_ID; expected a GA4 ID beginning with G-." >&2
    GA_MEASUREMENT_ID=''
    ;;
esac

printf 'window.__CAREER_NOTE_CONFIG__ = { gaMeasurementId: "%s" };\n' "$GA_MEASUREMENT_ID" \
  > /usr/share/nginx/html/runtime-config.js

# Restrict envsubst to deployment values so Nginx variables such as $host remain intact.
envsubst '${PORT} ${BACKEND_BASE_URL}' \
  < /etc/nginx/templates/default.conf.template \
  > /etc/nginx/conf.d/default.conf

exec /docker-entrypoint.sh nginx -g 'daemon off;'
