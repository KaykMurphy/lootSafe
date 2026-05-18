# LootSafe API

API REST para intermediar transações digitais com pagamento via Pix, retenção do valor até a liberação do produto, mediação em caso de disputa e notificações por webhook do Mercado Pago.

O projeto foi pensado para fluxos de compra e venda de itens digitais, como contas de jogos, moedas virtuais e outros produtos digitais, mantendo credenciais sensíveis criptografadas no banco de dados.

## Principais recursos

- Criação e gerenciamento de ofertas digitais.
- Cálculo automático de taxa da plataforma e valor líquido do vendedor.
- Criptografia AES/GCM para login e senha do produto.
- Geração de pagamento Pix via Mercado Pago.
- Processamento assíncrono de webhooks do Mercado Pago.
- Envio de e-mail ao comprador após aprovação do pagamento.
- Fluxo de mediação com histórico de mensagens entre comprador, vendedor e moderador.
- Cancelamento automático de Pix pendentes expirados.
- Banco H2 em memória para desenvolvimento local.
- Estrutura preparada para PostgreSQL e Flyway.

## Stack

- Java 17
- Spring Boot 4.0.6
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Mail
- Spring Scheduling / Async
- Maven Wrapper
- H2 Database
- PostgreSQL Driver
- Flyway
- MapStruct
- Lombok
- Mercado Pago Java SDK

## Estrutura do projeto

```text
src/main/java/com/lootsafe
├── config          # Configurações de segurança, criptografia e executor async
├── controller      # Endpoints REST
├── dto             # Objetos de entrada e saída da API
├── enums           # Status, categorias e tipos usados no domínio
├── exception       # Tratamento centralizado de erros
├── mapper          # Conversões MapStruct
├── model           # Entidades JPA
├── repository      # Repositórios e contrato de e-mail
├── security        # Filtro de API key, criptografia e validação de webhook
└── service         # Regras de negócio
```

## Requisitos

- JDK 17 instalado.
- Maven Wrapper incluído no projeto.
- Token do Mercado Pago para gerar/cancelar Pix e consultar pagamentos.
- Senha de aplicativo SMTP para envio de e-mails.

## Variáveis de ambiente

A aplicação carrega variáveis a partir do ambiente e também de um arquivo `.env` na raiz do projeto.

Crie um `.env` local com:

```properties
LOOTSAFE_CRYPTO_KEY=1234567890123456
LOOTSAFE_ADMIN_API_KEY=troque-esta-chave
LOOTSAFE_MP_TOKEN=TEST-seu-token-mercado-pago
SECRET_KEY=seu-segredo-de-webhook
EMAIL_PASSWORD=sua-senha-de-app-smtp
```

| Variável | Obrigatória | Descrição |
| --- | --- | --- |
| `LOOTSAFE_CRYPTO_KEY` | Sim | Chave AES usada para criptografar credenciais. Deve ter exatamente 16, 24 ou 32 bytes. |
| `LOOTSAFE_CRYPTO_KEY_APP` | Opcional | Alternativa com prioridade sobre `LOOTSAFE_CRYPTO_KEY`. |
| `LOOTSAFE_ADMIN_API_KEY` | Sim | Chave exigida no header `X-API-KEY` para rotas administrativas de mediação. |
| `LOOTSAFE_MP_TOKEN` | Sim | Access token do Mercado Pago. |
| `SECRET_KEY` | Sim | Segredo usado para validar assinatura dos webhooks do Mercado Pago. |
| `EMAIL_PASSWORD` | Sim | Senha SMTP usada pelo Spring Mail. |

> O arquivo `.env` já está no `.gitignore` e não deve ser versionado.

## Como executar localmente

```bash
./mvnw spring-boot:run
```

A API sobe por padrão em:

```text
http://localhost:8080
```

Console H2 local:

```text
http://localhost:8080/h2-console
```

Credenciais padrão do H2:

```text
JDBC URL: jdbc:h2:mem:lootsafedb
User: sa
Password: 1234
```

## Testes

```bash
./mvnw test
```

O teste atual valida a subida do contexto Spring com propriedades de teste para criptografia, API key, webhook e Mercado Pago.

## Fluxo principal

1. O vendedor cria uma oferta informando categoria, descrição, valor, credenciais do produto, e-mail e chave Pix.
2. A API calcula a taxa da plataforma e o valor líquido do vendedor.
3. O comprador solicita a geração do Pix para a oferta.
4. O Mercado Pago envia um webhook quando o pagamento é aprovado.
5. A API valida a assinatura do webhook, consulta o pagamento e marca a oferta como `PAYMENT_HELD`.
6. As credenciais são enviadas ao comprador por e-mail e a data limite de liberação é definida.
7. O pagamento pode ser liberado ao vendedor ou a oferta pode entrar em mediação.
8. Em mediação, mensagens podem ser trocadas e um moderador decide entre comprador ou vendedor.

## Status da transação

| Status | Significado |
| --- | --- |
| `PENDING_PAYMENT` | Oferta criada aguardando pagamento. |
| `PAYMENT_HELD` | Pagamento aprovado e valor retido. |
| `IN_MEDIATION` | Oferta em disputa. |
| `SETTLED` | Pagamento liquidado ao vendedor. |
| `REFUNDED` | Pagamento reembolsado ao comprador. |
| `CANCELLED` | Oferta cancelada. |
| `COMPLETED` | Status reservado para conclusão de fluxo. |

## Endpoints

### Ofertas

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/offers` | Cria uma oferta. |
| `GET` | `/api/offers` | Lista ofertas com paginação. |
| `GET` | `/api/offers/{id}` | Busca uma oferta por ID. |
| `PUT` | `/api/offers/{id}` | Atualiza uma oferta. |
| `DELETE` | `/api/offers/{id}` | Remove uma oferta quando o status permite. |
| `POST` | `/api/offers/{id}/generate-pix` | Gera Pix para o comprador. |
| `POST` | `/api/offers/{id}/release-payment` | Tenta liberar o pagamento ao vendedor. |
| `POST` | `/api/offers/{id}/mediation` | Abre mediação para uma oferta com pagamento retido. |
| `POST` | `/api/offers/{id}/messages` | Envia mensagem na mediação. |
| `GET` | `/api/offers/{id}/messages` | Lista histórico de mensagens da mediação. |

### Mediação

As rotas abaixo exigem o header:

```text
X-API-KEY: <LOOTSAFE_ADMIN_API_KEY>
```

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/mediation/offers` | Lista ofertas em mediação. |
| `GET` | `/api/mediation/offers/all` | Lista todas as ofertas com paginação e ordenação. |
| `POST` | `/api/mediation/offers/{id}/resolve` | Resolve disputa com `BUYER_WINS` ou `SELLER_WINS`. |
| `DELETE` | `/api/mediation/offers/{id}/cancel` | Cancela manualmente uma oferta. |
| `GET` | `/api/mediation/offers/statistics/profit` | Retorna lucro acumulado da plataforma. |
| `POST` | `/api/mediation/offers/{id}/simulate-payment` | Simula pagamento aprovado para testes locais. |

### Webhooks

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/webhooks/mercadopago` | Recebe notificações do Mercado Pago. |

O webhook espera:

- Header `x-signature`
- Header `x-request-id`
- Query param `data.id`
- Corpo JSON com `type = "payment"`

## Exemplos de uso

### Criar oferta

```bash
curl -X POST http://localhost:8080/api/offers \
  -H "Content-Type: application/json" \
  -d '{
    "productCategory": "GAME_ACCOUNT",
    "description": "Conta nível 80 com itens raros",
    "grossAmount": 150.00,
    "trialPeriodHours": 24,
    "credentialLogin": "login-da-conta",
    "credentialPassword": "senha-da-conta",
    "sellerEmail": "seller@example.com",
    "pixKeyType": "EMAIL",
    "pixKey": "seller@example.com"
  }'
```

### Gerar Pix para comprador

```bash
curl -X POST "http://localhost:8080/api/offers/{offerId}/generate-pix?buyerEmail=buyer@example.com&buyerFirstName=Buyer&buyerLastName=Test"
```

### Simular pagamento aprovado em ambiente local

```bash
curl -X POST http://localhost:8080/api/mediation/offers/{offerId}/simulate-payment \
  -H "X-API-KEY: $LOOTSAFE_ADMIN_API_KEY"
```

### Abrir mediação

```bash
curl -X POST http://localhost:8080/api/offers/{offerId}/mediation
```

### Enviar mensagem na mediação

```bash
curl -X POST http://localhost:8080/api/offers/{offerId}/messages \
  -H "Content-Type: application/json" \
  -d '{
    "messageAuthor": "BUYER",
    "messageText": "O produto entregue não corresponde ao anúncio."
  }'
```

### Resolver disputa

```bash
curl -X POST "http://localhost:8080/api/mediation/offers/{offerId}/resolve?decision=BUYER_WINS" \
  -H "X-API-KEY: $LOOTSAFE_ADMIN_API_KEY"
```

## Banco de dados

O perfil atual usa H2 em memória com `ddl-auto=update`, indicado para desenvolvimento local.

Para produção, recomenda-se:

- Configurar PostgreSQL.
- Habilitar Flyway.
- Versionar migrations em `src/main/resources/db/migration`.
- Trocar `spring.jpa.hibernate.ddl-auto` para `validate` ou `none`.
- Remover credenciais e valores sensíveis do `application.yml`.

## Observações importantes antes de produção

- O endpoint `/api/mediation/offers/{id}/simulate-payment` está marcado no código como apenas para testes e deve ser removido antes de produção.
- O repasse automático Pix ao vendedor ainda não está implementado em `PaymentService.transferToSeller`.
- As rotas de mediação dependem de `X-API-KEY`, mas a política de segurança deve ser revisada antes de expor a API publicamente.
- O CORS está limitado a `localhost:5173` e `127.0.0.1:5173`.
- O e-mail remetente está configurado no `application.yml`; para produção, mova também o usuário SMTP para variável de ambiente.
- Avalie se as credenciais do produto devem aparecer nas respostas da API em todos os cenários.

## Licença

Licença ainda não definida.
