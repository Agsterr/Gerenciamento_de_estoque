FROM postgres:15-alpine

RUN apk add --no-cache dcron bash gzip

COPY docker/backup/backup-cron.sh /usr/local/bin/backup-cron.sh
RUN chmod +x /usr/local/bin/backup-cron.sh

COPY docker/backup/crontab /etc/cron.d/db-backup
RUN chmod 0644 /etc/cron.d/db-backup && crontab /etc/cron.d/db-backup

CMD ["crond", "-f", "-l", "2"]
