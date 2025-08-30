@echo off
echo ====================================
echo    GERENCIAMENTO DE ESTOQUE - DOCKER
echo ====================================
echo.

if "%1"=="build" goto build
if "%1"=="run" goto run
if "%1"=="stop" goto stop
if "%1"=="logs" goto logs
if "%1"=="clean" goto clean
if "%1"=="restart" goto restart

:help
echo Uso: docker-scripts.bat [comando]
echo.
echo Comandos disponíveis:
echo   build    - Constrói a imagem Docker
echo   run      - Inicia a aplicação (build + up)
echo   stop     - Para todos os containers
echo   logs     - Mostra logs da aplicação
echo   clean    - Remove containers e imagens
echo   restart  - Reinicia a aplicação
echo.
goto end

:build
echo Construindo imagem Docker...
docker-compose build --no-cache
goto end

:run
echo Iniciando aplicação...
docker-compose up -d --build
echo.
echo Aplicação iniciada! Acesse:
echo - API: http://localhost:8080
echo - Swagger: http://localhost:8080/swagger-ui.html
echo - Health Check: http://localhost:8080/actuator/health
echo.
echo Use 'docker-scripts.bat logs' para ver os logs
goto end

:stop
echo Parando containers...
docker-compose down
goto end

:logs
echo Mostrando logs da aplicação...
docker-compose logs -f app
goto end

:clean
echo Removendo containers e imagens...
docker-compose down -v --rmi all
docker system prune -f
goto end

:restart
echo Reiniciando aplicação...
docker-compose restart app
goto end

:end
