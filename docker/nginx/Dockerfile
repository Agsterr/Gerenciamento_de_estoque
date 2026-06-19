# Build do frontend Angular
FROM node:20-alpine AS front-build
WORKDIR /front
COPY Gerenciamento_de_estoque_front/package.json Gerenciamento_de_estoque_front/package-lock.json* ./
RUN npm ci --legacy-peer-deps 2>/dev/null || npm install
COPY Gerenciamento_de_estoque_front/ ./
RUN npm run build -- --configuration=docker

# Nginx: estáticos + proxy reverso para o backend
FROM nginx:1.27-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY docker/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf
COPY --from=front-build /front/dist/gerenciamento-de-estoque-front/browser /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
