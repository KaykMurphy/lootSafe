# LootSafe API

> Status: em desenvolvimento ativo.

API REST para intermediar transações digitais com pagamento via Pix, mantendo o valor retido até a liberação do produto, tratando disputas por mediação e recebendo notificações de webhook do Mercado Pago.

O projeto foi pensado para compra e venda de itens digitais, como contas de jogos, moedas virtuais e outros produtos digitais, mantendo credenciais sensíveis criptografadas no banco de dados.

## Funcionalidades

- Criação e gerenciamento de ofertas digitais.
- Cálculo automático da taxa da plataforma e do valor líquido do vendedor.
- Criptografia AES/GCM para login e senha do produto.
- Geração de pagamento Pix pelo Mercado Pago.
- Processamento assíncrono de webhooks do Mercado Pago.
- Envio de e-mail ao comprador após aprovação do pagamento.
- Fluxo de mediação com histórico de mensagens entre comprador, vendedor e moderador.
- Cancelamento automático de Pix pendentes expirados.
- Banco H2 em memória para desenvolvimento local.
- PostgreSQL via Docker Compose para ambiente conteinerizado.
- Sistema de Spring Profiles (dev, prod) com fail-fast.
- Spring Boot Actuator para health checks.
- Estrutura preparada para Flyway.

## Stack

- Java 17
- Spring Boot 4.0.6
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Mail
- Spring Scheduling / Async
- Spring Actuator
- Spring Profiles
- Maven Wrapper
- H2 Database
- PostgreSQL Driver
- Flyway
- MapStruct
- Lombok
- Mercado Pago Java SDK

## Estrutura do Projeto

```text
src/main/java/com/lootsafe
|-- config          # Segurança, criptografia, WebSocket e configuração assíncrona
|-- controller      # Endpoints REST
|-- dto             # Objetos de requisição e resposta da API
|-- enums           # Status, categorias e tipos do domínio
|-- exception       # Tratamento centralizado de erros
|-- mapper          # Conversões com MapStruct
|-- model           # Entidades JPA
|-- repository      # Repositórios e contrato de e-mail
|-- security        # API key, criptografia e validação de webhook
|-- service         # Regras de negócio
`-- swagger         # Configuração do SpringDoc OpenAPI
```

## Spring Profiles

O projeto usa três níveis de configuração via Spring Profiles:

| Profile | Arquivo | Função |
|---------|---------|--------|
| **Default** | `application.properties` | Regras fixas (porta, limites, taxa). Fail-fast: sem variáveis de ambiente = crash. |
| **Dev** | `application-dev.properties` | H2 em memória, logs DEBUG, chaves fixas. Roda sem configurar nada. |
| **Prod** | `application-prod.properties` | PostgreSQL, variáveis de ambiente obrigatórias, logs WARN, actuator. |

### Como ativar

```bash
# Dev (local):
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Prod (via Docker — já configurado no docker-compose.yml):
docker compose up -d
```

## Variáveis de Ambiente

A aplicação carrega variáveis do ambiente e de um arquivo `.env` na raiz do projeto.

Crie um arquivo `.env` local com:

```properties
# Banco PostgreSQL
DB_PASSWORD=sua-senha-forte

# JWT
JWT_SECRET=chave-gerada-com-openssl-rand-base64-32
JWT_EXPIRATION=60

# Criptografia (chaves Base64 AES-128)
LOOTSAFE_CRYPTO_KEY=chave-gerada-com-openssl-rand-base64-16
LOOTSAFE_CRYPTO_KEY_APP=chave-gerada-com-openssl-rand-base64-16

# API Key Admin
LOOTSAFE_ADMIN_API_KEY=chave-gerada-com-openssl-rand-base64-32

# Mercado Pago
LOOTSAFE_MP_TOKEN=APP_USR-seu-token-de-producao

# Webhook
SECRET_KEY=chave-gerada-com-openssl-rand-base64-32

# Email
EMAIL_PASSWORD=sua-senha-de-app-smtp

# Admin
ADMIN_PASSWORD=sua-senha-admin
admin_name=Admin LOOTSAFE
email_admin=admin@lootsafe.com

# CORS
CORS_ORIGIN=http://localhost:5173,https://lootsafe.com.br
```

| Variável | Obrigatória | Descrição |
| --- | --- | --- |
| `DB_PASSWORD` | Prod | Senha do PostgreSQL. |
| `JWT_SECRET` | Sim | Chave para assinar tokens JWT. Gere com `openssl rand -base64 32`. |
| `JWT_EXPIRATION` | Não | Expiração do JWT em minutos (padrão: 60). |
| `LOOTSAFE_CRYPTO_KEY` | Sim | Chave AES para criptografar credenciais. Base64 de 16 bytes. |
| `LOOTSAFE_ADMIN_API_KEY` | Sim | Chave para rotas admin em `/api/mediation/**`. |
| `LOOTSAFE_MP_TOKEN` | Sim | Access token do Mercado Pago. |
| `SECRET_KEY` | Sim | Segredo para validar webhooks do Mercado Pago. |
| `EMAIL_PASSWORD` | Sim | Senha SMTP para envio de e-mails. |
| `admin_name` | Não | Nome do admin criado automaticamente (padrão: Admin LOOTSAFE). |
| `email_admin` | Não | Email do admin criado automaticamente. |
| `ADMIN_PASSWORD` | Não | Senha do admin criado automaticamente. |
| `CORS_ORIGIN` | Não | Origens permitidas para CORS (separadas por vírgula). |

> O arquivo `.env` está no `.gitignore` e não deve ser commitado.

## Requisitos

- JDK 17+ instalado.
- Maven Wrapper incluso no projeto.
- Docker e Docker Compose (para ambiente conteinerizado).
- Token do Mercado Pago para gerar/cancelar Pix e consultar pagamentos.
- Senha de aplicativo SMTP para envio de e-mails.

## Executando Localmente

### Com Docker (banco PostgreSQL)

```bash
# Subir apenas o banco
docker compose -f docker-compose.dev.yml up -d

# Rodar a API com profile dev
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Sem Docker (banco H2 em memória)

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

A API sobe em `http://localhost:8080`.

## Executando com Docker (Produção)

```bash
docker compose up --build -d
```

A API sobe em `http://localhost:8080` e o PostgreSQL em `localhost:5432`.

Para derrubar:

```bash
docker compose down
```

> O Docker Compose usa `SPRING_PROFILES_ACTIVE=prod` e conecta ao PostgreSQL do container via variáveis de ambiente.

## Testes

```bash
./mvnw test
```

## Autenticação

A API usa dois mecanismos de autenticação:

- JWT com header `Authorization: Bearer <token>` para rotas autenticadas de usuário.
- API key com header `X-API-KEY: <LOOTSAFE_ADMIN_API_KEY>` para rotas administrativas em `/api/mediation/**`.

Rotas públicas:

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/offers`
- `GET /api/offers/{id}`
- `GET /api/chat/history`
- `POST /webhooks/mercadopago`
- `GET /actuator/health`

As demais rotas exigem JWT ou API key administrativa.

### Cadastro e Login

Novos usuários cadastrados pela API recebem o papel `BUYER` por padrão.

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "comprador",
    "email": "comprador@example.com",
    "password": "123456",
    "confirmPassword": "123456"
  }'
```

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "name": "comprador",
    "password": "123456"
  }'
```

A resposta do login contém o token JWT:

```json
{
  "userId": "comprador",
  "token": "jwt-gerado"
}
```

## Fluxo Principal

1. O vendedor cria uma oferta com categoria, descrição, valor, credenciais do produto, e-mail e chave Pix.
2. A API calcula a taxa da plataforma e o valor líquido do vendedor.
3. O comprador solicita a geração do Pix para a oferta.
4. O Mercado Pago envia um webhook quando o pagamento é aprovado.
5. A API valida a assinatura do webhook, consulta o pagamento e marca a oferta como `PAYMENT_HELD`.
6. As credenciais são enviadas por e-mail ao comprador e o prazo de liberação é definido.
7. O pagamento pode ser liberado ao vendedor ou a oferta pode entrar em mediação.
8. Durante a mediação, comprador, vendedor e moderador podem consultar o histórico de mensagens conforme suas permissões.
9. O moderador resolve a disputa a favor do comprador ou do vendedor.

## Status da Transação

| Status | Significado |
| --- | --- |
| `PENDING_PAYMENT` | Oferta criada e aguardando pagamento. |
| `PAYMENT_HELD` | Pagamento aprovado e valor retido. |
| `IN_MEDIATION` | Oferta em disputa. |
| `SETTLED` | Pagamento repassado ao vendedor. |
| `REFUNDED` | Pagamento reembolsado ao comprador. |
| `CANCELLED` | Oferta cancelada. |
| `COMPLETED` | Status reservado para conclusão do fluxo. |

## Endpoints

### Autenticação

| Método | Rota | Autenticação | Descrição |
| --- | --- | --- | --- |
| `POST` | `/api/auth/signup` | Pública | Cadastra um usuário. |
| `POST` | `/api/auth/login` | Pública | Autentica um usuário e retorna JWT. |
| `GET` | `/api/auth/me` | JWT | Retorna nome e papéis do usuário autenticado. |

### Ofertas

| Método | Rota | Autenticação | Descrição |
| --- | --- | --- | --- |
| `POST` | `/api/offers` | JWT | Cria uma oferta. |
| `GET` | `/api/offers` | Pública | Lista ofertas com paginação. |
| `GET` | `/api/offers/{id}` | Pública | Busca uma oferta por ID. |
| `PUT` | `/api/offers/{id}` | JWT | Atualiza uma oferta (vendedor dono ou moderador). |
| `DELETE` | `/api/offers/{id}` | JWT | Remove uma oferta (vendedor dono ou moderador). |
| `POST` | `/api/offers/{id}/generate-pix` | JWT | Gera Pix para o comprador. |
| `POST` | `/api/offers/{id}/release-payment` | JWT | Libera o pagamento ao vendedor (apenas comprador). |
| `POST` | `/api/offers/{id}/mediation` | JWT | Abre mediação (comprador ou vendedor). |
| `POST` | `/api/offers/{id}/mediation/drop` | JWT | Comprador desiste da mediação. |
| `POST` | `/api/offers/{id}/messages` | JWT | Envia mensagem de mediação. |
| `GET` | `/api/offers/{id}/messages` | JWT | Lista histórico de mensagens. |
| `GET` | `/api/offers/my-sales` | JWT | Lista vendas do usuário. |
| `GET` | `/api/offers/my-purchases` | JWT | Lista compras do usuário. |

### Chat Público

| Método | Rota | Autenticação | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/chat/history` | Pública | Lista mensagens mais recentes. |
| `STOMP` | `/app/chat.send` | Pública | Envia mensagem no chat WebSocket. |
| `STOMP` | `/topic/public` | Pública | Tópico público do WebSocket. |

### Mediação Administrativa

Exige header `X-API-KEY: <LOOTSAFE_ADMIN_API_KEY>`:

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/mediation/offers` | Lista ofertas em mediação. |
| `GET` | `/api/mediation/offers/all` | Lista todas as ofertas (paginado). |
| `POST` | `/api/mediation/offers/{id}/resolve` | Resolve disputa (`BUYER_WINS` ou `SELLER_WINS`). |
| `DELETE` | `/api/mediation/offers/{id}/cancel` | Cancela oferta manualmente. |
| `POST` | `/api/mediation/offers/{id}/drop` | Encerra mediação e libera repasse. |
| `GET` | `/api/mediation/offers/statistics/profit` | Lucro acumulado da plataforma. |
| `POST` | `/api/mediation/offers/{id}/simulate-payment` | Simula pagamento (apenas testes). |

### Webhooks

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/webhooks/mercadopago` | Recebe notificações do Mercado Pago. |

Espera headers `x-signature`, `x-request-id`, query param `data.id` e body JSON com `type = "payment"`.

### Health Check

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/actuator/health` | Status da aplicação (público). |

## Exemplos de Uso

### Criar Oferta

```bash
curl -X POST http://localhost:8080/api/offers \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productCategory": "GAME_ACCOUNT",
    "description": "Conta level 80 com itens raros",
    "grossAmount": 150.00,
    "trialPeriodHours": 24,
    "credentialLogin": "login-da-conta",
    "credentialPassword": "senha-da-conta",
    "pixKeyType": "EMAIL",
    "pixKey": "vendedor@example.com"
  }'
```

### Simular Pagamento Aprovado

```bash
curl -X POST http://localhost:8080/api/mediation/offers/{offerId}/simulate-payment \
  -H "X-API-KEY: $LOOTSAFE_ADMIN_API_KEY"
```

### Resolver Disputa

```bash
curl -X POST "http://localhost:8080/api/mediation/offers/{offerId}/resolve?decision=BUYER_WINS" \
  -H "X-API-KEY: $LOOTSAFE_ADMIN_API_KEY"
```

## Banco de Dados

### Dev (H2)

Profile `dev`: banco H2 em memória com `ddl-auto=update`. Reset a cada reinício.

### Dev com Docker (PostgreSQL)

```bash
docker compose -f docker-compose.dev.yml up -d
```

Banco PostgreSQL em `localhost:5432` com:
- Database: `lootsafedb_dev`
- User: `devuser`
- Password: `devpassword`

### Prod (PostgreSQL)

`docker compose up -d` sobe PostgreSQL com:
- Database: `lootsafedb`
- User: `postgres`
- Password: via `${DB_PASSWORD}`

Flyway habilitado, `ddl-auto=validate`.

## Observações

- O endpoint `/api/mediation/offers/{id}/simulate-payment` deve ser removido antes de produção.
- A transferência automática de Pix para o vendedor ainda não está plenamente implementada.
- O CORS é configurado dinamicamente via `CORS_ORIGIN`.
- O admin é criado automaticamente no startup via `AdminInitializer`.
- As respostas públicas de oferta não expõem credenciais, dados Pix ou e-mail do vendedor.

## Licença

Licença ainda não definida.
