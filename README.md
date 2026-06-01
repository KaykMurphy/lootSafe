# LootSafe API

> Status: pausado temporariamente.
>
> Este projeto está funcional em ambiente local, mas o desenvolvimento foi pausado enquanto trabalho em outro projeto.

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
`-- service         # Regras de negócio
```

## Requisitos

- JDK 17 instalado.
- Maven Wrapper incluso no projeto.
- Token do Mercado Pago para gerar/cancelar Pix e consultar pagamentos.
- Senha de aplicativo SMTP para envio de e-mails.

## Variáveis de Ambiente

A aplicação carrega variáveis do ambiente e também de um arquivo `.env` na raiz do projeto.

Crie um arquivo `.env` local com:

```properties
LOOTSAFE_CRYPTO_KEY=1234567890123456
LOOTSAFE_ADMIN_API_KEY=troque-esta-chave
LOOTSAFE_MP_TOKEN=TEST-seu-token-do-mercado-pago
SECRET_KEY=seu-segredo-de-webhook
EMAIL_PASSWORD=sua-senha-smtp
```

| Variável | Obrigatória | Descrição |
| --- | --- | --- |
| `LOOTSAFE_CRYPTO_KEY` | Sim | Chave AES usada para criptografar credenciais. Deve ter exatamente 16, 24 ou 32 bytes. |
| `LOOTSAFE_CRYPTO_KEY_APP` | Opcional | Alternativa com prioridade sobre `LOOTSAFE_CRYPTO_KEY`. |
| `LOOTSAFE_ADMIN_API_KEY` | Sim | Chave exigida no header `X-API-KEY` para rotas administrativas de mediação. |
| `LOOTSAFE_MP_TOKEN` | Sim | Access token do Mercado Pago. |
| `SECRET_KEY` | Sim | Segredo usado para validar assinaturas dos webhooks do Mercado Pago. |
| `EMAIL_PASSWORD` | Sim | Senha SMTP usada pelo Spring Mail. |

> O arquivo `.env` já está listado no `.gitignore` e não deve ser commitado.

## Executando Localmente

```bash
./mvnw spring-boot:run
```

A API sobe por padrão em:

```text
http://localhost:8080
```

Console local do H2:

```text
http://localhost:8080/h2-console
```

Credenciais padrão do H2:

```text
JDBC URL: jdbc:h2:mem:lootsafedb
Usuário: sa
Senha: 1234
```

## Testes

```bash
./mvnw test
```

Os testes atuais validam o contexto Spring e cobrem regras de segurança, API key, respostas de oferta e propriedades de teste para webhook e Mercado Pago.

## Autenticação

A API usa dois mecanismos de autenticação:

- JWT com header `Authorization: Bearer <token>` para rotas autenticadas de usuário.
- API key com header `X-API-KEY: <LOOTSAFE_ADMIN_API_KEY>` para rotas administrativas em `/api/mediation/**`.

Rotas públicas:

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/offers`
- `GET /api/offers`
- `/api/chat/**`
- `/chat-test.html`

As demais rotas exigem JWT ou API key administrativa, dependendo do grupo do endpoint.

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
| `POST` | `/api/offers` | Pública | Cria uma oferta. |
| `GET` | `/api/offers` | Pública | Lista ofertas com paginação. |
| `GET` | `/api/offers/{id}` | JWT | Busca uma oferta por ID. |
| `PUT` | `/api/offers/{id}` | JWT | Atualiza uma oferta. Permitido ao vendedor dono da oferta ou moderador. |
| `DELETE` | `/api/offers/{id}` | JWT | Remove uma oferta quando o status permite. Permitido ao vendedor dono da oferta ou moderador. |
| `POST` | `/api/offers/{id}/generate-pix` | JWT | Gera Pix para o comprador. |
| `POST` | `/api/offers/{id}/release-payment` | JWT | Tenta liberar o pagamento ao vendedor. |
| `POST` | `/api/offers/{id}/mediation` | JWT | Abre mediação para uma oferta com pagamento retido. |
| `POST` | `/api/offers/{id}/mediation/drop` | JWT | Permite ao comprador desistir da mediação e liberar o repasse ao vendedor. |
| `POST` | `/api/offers/{id}/messages` | JWT | Envia uma mensagem de mediação. |
| `GET` | `/api/offers/{id}/messages` | JWT | Lista o histórico de mensagens. Permitido ao comprador, vendedor ou moderador da oferta. |

### Chat Público

| Método | Rota | Autenticação | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/chat/history` | Pública | Lista as mensagens mais recentes do chat público. |
| `STOMP` | `/app/chat.send` | Pública | Envia uma mensagem no chat WebSocket público. |
| `STOMP` | `/topic/public` | Pública | Tópico público do WebSocket. |

### Mediação Administrativa

As rotas abaixo exigem:

```text
X-API-KEY: <LOOTSAFE_ADMIN_API_KEY>
```

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/mediation/offers` | Lista ofertas em mediação. |
| `GET` | `/api/mediation/offers/all` | Lista todas as ofertas com paginação e ordenação. |
| `POST` | `/api/mediation/offers/{id}/resolve` | Resolve uma disputa com `BUYER_WINS` ou `SELLER_WINS`. |
| `DELETE` | `/api/mediation/offers/{id}/cancel` | Cancela manualmente uma oferta. |
| `POST` | `/api/mediation/offers/{id}/drop` | Encerra a mediação e libera o repasse ao vendedor. |
| `GET` | `/api/mediation/offers/statistics/profit` | Retorna o lucro acumulado da plataforma. |
| `POST` | `/api/mediation/offers/{id}/simulate-payment` | Simula um pagamento aprovado para testes locais. |

### Webhooks

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/webhooks/mercadopago` | Recebe notificações do Mercado Pago. |

O webhook espera:

- Header `x-signature`
- Header `x-request-id`
- Query param `data.id`
- JSON body com `type = "payment"`

## Exemplos de Uso

### Criar Oferta

```bash
curl -X POST http://localhost:8080/api/offers \
  -H "Content-Type: application/json" \
  -d '{
    "productCategory": "GAME_ACCOUNT",
    "description": "Conta level 80 com itens raros",
    "grossAmount": 150.00,
    "trialPeriodHours": 24,
    "credentialLogin": "login-da-conta",
    "credentialPassword": "senha-da-conta",
    "sellerEmail": "vendedor@example.com",
    "pixKeyType": "EMAIL",
    "pixKey": "vendedor@example.com"
  }'
```

### Gerar Pix Para o Comprador

```bash
curl -X POST "http://localhost:8080/api/offers/{offerId}/generate-pix?buyerEmail=comprador@example.com&buyerFirstName=Comprador&buyerLastName=Teste" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### Simular Pagamento Aprovado Localmente

```bash
curl -X POST http://localhost:8080/api/mediation/offers/{offerId}/simulate-payment \
  -H "X-API-KEY: $LOOTSAFE_ADMIN_API_KEY"
```

### Abrir Mediação

```bash
curl -X POST http://localhost:8080/api/offers/{offerId}/mediation \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### Enviar Mensagem de Mediação

```bash
curl -X POST http://localhost:8080/api/offers/{offerId}/messages \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "author": "BUYER",
    "messageText": "O produto entregue não corresponde ao anúncio.",
    "messageType": "CHAT"
  }'
```

### Desistir da Mediação Como Comprador

```bash
curl -X POST http://localhost:8080/api/offers/{offerId}/mediation/drop \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### Resolver Disputa Como Administrador

```bash
curl -X POST "http://localhost:8080/api/mediation/offers/{offerId}/resolve?decision=BUYER_WINS" \
  -H "X-API-KEY: $LOOTSAFE_ADMIN_API_KEY"
```

## Banco de Dados

O perfil atual usa H2 em memória com `ddl-auto=update`, pensado para desenvolvimento local.

Para produção, configure:

- PostgreSQL.
- Flyway.
- Migrations versionadas em `src/main/resources/db/migration`.
- `spring.jpa.hibernate.ddl-auto` como `validate` ou `none`.
- Remoção de credenciais e valores sensíveis do `application.yml`.

## Observações Antes de Produção

- O endpoint `/api/mediation/offers/{id}/simulate-payment` está marcado no código como apenas para testes e deve ser removido antes de produção.
- A transferência automática de Pix para o vendedor ainda não está plenamente implementada em `PaymentService.transferToSeller`.
- As rotas administrativas de mediação dependem de `X-API-KEY`; revise a política de segurança antes de expor a API publicamente.
- O CORS está limitado a `localhost:5173` e `127.0.0.1:5173`.
- O e-mail remetente está configurado em `application.yml`; mova o usuário SMTP para variável de ambiente antes de produção.
- As respostas públicas de oferta não expõem credenciais, dados Pix, e-mail do vendedor nem ID de pagamento do Mercado Pago.

## Licença

Licença ainda não definida.
