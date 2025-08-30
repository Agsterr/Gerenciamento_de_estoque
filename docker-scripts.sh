#!/bin/bash

echo "===================================="
echo "   GERENCIAMENTO DE ESTOQUE - DOCKER"
echo "===================================="
echo

case "$1" in
  build)
    echo "Construindo imagem Docker..."
    docker-compose build --no-cache
    ;;
  run)
    echo "Iniciando aplicação..."
    docker-compose up -d --build
    echo
    echo "Aplicação iniciada! Acesse:"
    echo "- API: http://localhost:8080"
    echo "- Swagger: http://localhost:8080/swagger-ui.html"
    echo "- Health Check: http://localhost:8080/actuator/health"
    echo
    echo "Use './docker-scripts.sh logs' para ver os logs"
    ;;
  stop)
    echo "Parando containers..."
    docker-compose down
    ;;
  logs)
    echo "Mostrando logs da aplicação..."
    docker-compose logs -f app
    ;;
  clean)
    echo "Removendo containers e imagens..."
    docker-compose down -v --rmi all
    docker system prune -f
    ;;
  restart)
    echo "Reiniciando aplicação..."
    docker-compose restart app
    ;;
  *)
    echo "Uso: ./docker-scripts.sh [comando]"
    echo
    echo "Comandos disponíveis:"
    echo "  build    - Constrói a imagem Docker"
    echo "  run      - Inicia a aplicação (build + up)"
    echo "  stop     - Para todos os containers"
    echo "  logs     - Mostra logs da aplicação"
    echo "  clean    - Remove containers e imagens"
    echo "  restart  - Reinicia a aplicação"
    echo
    ;;
esac
