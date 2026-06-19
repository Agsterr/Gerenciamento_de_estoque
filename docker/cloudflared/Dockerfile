FROM alpine:3.20

COPY --from=cloudflare/cloudflared:2026.5.2 /usr/local/bin/cloudflared /usr/local/bin/cloudflared
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh /usr/local/bin/cloudflared

ENTRYPOINT ["/entrypoint.sh"]
