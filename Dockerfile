# Multi-stage build para otimizar o tamanho da imagem final
FROM maven:3.9.6-eclipse-temurin-11-alpine AS build

# Definir diretório de trabalho
WORKDIR /app

# Forçar ambiente UTF-8 para Maven/JVM para evitar problemas de encoding em Alpine
ENV LANG=C.UTF-8
ENV MAVEN_OPTS="-Dproject.build.sourceEncoding=UTF-8 -Dfile.encoding=UTF-8"
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

# Copiar arquivos de configuração do Maven primeiro (para cache de dependências)
COPY pom.xml .
COPY mvnw .
# mvnw.cmd é um wrapper para Windows — não é necessário no container Linux e pode causar erro se não existir
# COPY mvnw.cmd .
COPY .mvn .mvn

# Garantir que o wrapper do Maven seja executável (se usado)
RUN chmod +x mvnw || true

# Baixar dependências (será cached se o pom.xml não mudar)
RUN mvn dependency:go-offline -Dproject.build.sourceEncoding=UTF-8 -Dfile.encoding=UTF-8 -B

# Copiar código fonte
COPY src ./src

# Compilar aplicação (pulando testes para build mais rápido)
RUN mvn clean package -DskipTests -Dproject.build.sourceEncoding=UTF-8 -Dfile.encoding=UTF-8 -B

# Estágio final - runtime
FROM eclipse-temurin:11-jre-alpine

# Instalar dependências do sistema necessárias
RUN apk add --no-cache \
    fontconfig \
    ttf-dejavu \
    && rm -rf /var/cache/apk/*

# Criar usuário não-root para segurança
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -s /bin/sh -D appuser

# Definir diretório de trabalho
WORKDIR /app

# Copiar JAR da aplicação do estágio de build
COPY --from=build /app/target/demo.jar app.jar

# Alterar proprietário dos arquivos para o usuário não-root
RUN chown -R appuser:appgroup /app

# Mudar para usuário não-root
USER appuser

# Expor porta da aplicação
EXPOSE 8080

# Configurações da JVM para container
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Comando para executar a aplicação
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Health check para verificar se a aplicação está funcionando
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
