# tech-challenge-billing-service

Microsserviço de Orçamentos e Pagamentos do **Tech Challenge — Fase 4**.

Responsável pelo cálculo de orçamentos a partir do diagnóstico de veículos, cobrança e integração com provedor de pagamentos (**Mercado Pago** via Pix / QR Code), estornos e compensações financeiras da saga distribuída. Desenvolvido em **Java 21 com Spring Boot 4**, utiliza banco não relacional **Amazon DynamoDB (NoSQL)**, mensageria assíncrona orientada a eventos com **Amazon SQS** (entrada) e **Amazon SNS** (saída), executado em contêineres gerenciados no **Amazon EKS**.

---

## Propósito

- **Cálculo de Orçamentos:** processa os itens do diagnóstico comunicados pelo `execution-service`, aplicando os valores do catálogo de peças e serviços da oficina para consolidar o valor total.
- **Gestão de Pagamentos:** gera cobranças com QR Code dinâmico do Mercado Pago (Pix) e processa notificações via Webhook para confirmação de pagamento.
- **Gestão Transacional e Compensação (Saga Pattern):** atua de forma coreografada na saga da ordem de serviço. Em caso de falha após o pagamento (ex: indisponibilidade de peças no estoque comunicada pelo `execution-service`), aciona automaticamente o estorno financeiro e encerra o fluxo com consistência eventual.
- **Catálogo de Serviços e Peças:** provê endpoints REST para consulta e manutenção da tabela de preços de serviços e tipos de peças.

---

## Tecnologias

- **Linguagem & Runtime:** Java 21 (Amazon Corretto)
- **Framework Principal:** Spring Boot 4.0.5 (Spring MVC, Spring Actuator, Spring Validation)
- **Banco de Dados Não Relacional (NoSQL):** Amazon DynamoDB (AWS SDK v2 com Enhanced Client)
- **Mensageria Assíncrona:** Amazon SQS (filas de entrada) e Amazon SNS (tópico de saída com padrão Fan-Out) via Spring Cloud AWS 4.0.2
- **Provedor de Pagamentos:** Mercado Pago REST API (com fallback/mock configurável para testes)
- **Testes & Qualidade:** JUnit 5, Mockito, AssertJ, Awaitility, Testcontainers (LocalStack), JaCoCo e SonarQube
- **BDD (Behavior-Driven Development):** Cucumber 7.x com cenários em português (Gherkin)
- **Documentação de API:** SpringDoc OpenAPI 3 (Swagger UI)
- **Containerização & Orquestração:** Docker multi-stage build e Kubernetes (Amazon EKS)
- **CI/CD:** GitHub Actions

---

## Estrutura do Repositório

```
.github/workflows/
└── deploy.yml          CI/CD: compilação, testes, cobertura JaCoCo, push ECR e deploy no EKS
app/
├── src/main/java/com/fiap/techchallenge/
│   ├── orcamento/      cálculo, geração e aprovação de orçamentos (Clean Architecture)
│   ├── pagamento/      cobrança Pix/QR Code, integração Mercado Pago e webhooks
│   ├── peca/           catálogo de tipos de peças e preços
│   ├── servico/        catálogo de tipos de serviços e preços
│   ├── mensageria/     listeners SQS, outbox pattern, publisher SNS/SQS e idempotência
│   ├── sequence/       gerador de sequências atômicas no DynamoDB
│   ├── config/         configurações de DynamoDB, Jackson, Swagger e Time
│   ├── exception/      tratamento global de exceções
│   └── BillingServiceApplication.java
├── src/main/resources/
│   ├── application.properties    configurações e mapeamento de variáveis de ambiente
│   └── logback-spring.xml        logs estruturados em formato JSON (Logstash)
├── src/test/
│   ├── java/.../bdd/             steps e suíte de execução do Cucumber (BDD)
│   ├── resources/features/       cenários Gherkin (faturamento_saga.feature)
│   └── ...                       testes unitários e de integração com LocalStack
├── build.gradle                  dependências, tarefas de teste e verificação JaCoCo (mínimo 80%)
└── Dockerfile                    multi-stage build para plataforma linux/amd64
k8s/
├── configmap.yaml                variáveis de configuração não sensíveis
├── deployment.yaml               Deployment (réplicas, probes de liveness/readiness, non-root)
├── service.yaml                  Service do tipo LoadBalancer (AWS Network Load Balancer)
├── hpa.yaml                      Horizontal Pod Autoscaler (2 a 10 réplicas)
└── pod-disruption-budget.yaml    PDB garantindo alta disponibilidade
```

---

## Arquitetura deste Repositório

```
                                  ┌───────────────────────────┐
                                  │   Amazon API Gateway      │
                                  └─────────────┬─────────────┘
                                                │ /billing/{proxy+}
                                                ▼
                                  ┌───────────────────────────┐
                                  │   NLB (Load Balancer)     │
                                  └─────────────┬─────────────┘
                                                │
                                                ▼
┌───────────────────────┐         ┌───────────────────────────┐         ┌────────────────────────┐
│     Amazon SQS        │         │   Pods billing-service    │         │      Amazon SNS        │
│  (Filas de Entrada)   ├────────►│        (Amazon EKS)       ├────────►│   (Tópico de Saída)    │
│                       │         │                           │         │  Eventos de Faturamento│
│ - Diagnóstico         │         │ - Clean Architecture      │         └───────────┬────────────┘
│ - Aprovação           │         │ - Idempotência por eventId│                     │ (Fan-Out)
│ - Falha de Execução   │         │ - Transacional com Outbox │         ┌───────────┴────────────┐
│ - Cancelamento de OS  │         └──────┬─────────────┬──────┘         ▼                        ▼
└───────────────────────┘                │             │         ┌──────────────┐         ┌──────────────┐
                                         │             │         │  os-service  │         │  execution   │
                                         ▼             ▼         │ (Atualiza OS)│         │(Reserva Peça)│
                                  ┌─────────────┐ ┌─────────────┐└──────────────┘         └──────────────┘
                                  │  DynamoDB   │ │ Mercado Pago│
                                  │  (NoSQL)    │ │ (Pix/Webhook│
                                  └─────────────┘ └─────────────┘
```

### Justificativa da Estratégia de Saga (Coreografada)

Conforme os requisitos da Fase 4, a coordenação dos processos distribuídos adota o padrão **Saga Coreografada** (orientada a eventos):

- **Desacoplamento e Autonomia:** Não há um orquestrador centralizado que precise conhecer as regras de todos os serviços. Cada microsserviço reage a eventos de seu interesse e publica os resultados de seu próprio domínio.
- **Resiliência e Tolerância a Falhas:** A ausência de um ponto único de falha (*Single Point of Failure*) garante que a indisponibilidade temporária de um serviço não derrube os demais.
- **Compensação Financeira:** Caso ocorra uma falha nas etapas posteriores do fluxo (como indisponibilidade de peças comunicada pelo serviço de execução), o `billing-service` é notificado de forma assíncrona para acionar a transação compensatória (estorno do valor pago e cancelamento do orçamento), mantendo a consistência eventual do sistema.

---

## Pré-requisitos

- Java JDK 21 (Amazon Corretto ou Eclipse Temurin)
- Docker e Docker Compose (para testes locais e build da imagem)
- Gradle 8.x (ou uso do wrapper `./gradlew` incluso no projeto)
- AWS CLI v2 configurado (para operações na nuvem)

---

## Execução e Testes Locais

### 1. Testes Unitários, de Integração e BDD (Cucumber)

O projeto possui suíte completa de testes executada contra instâncias de contêineres gerenciadas pelo Testcontainers (LocalStack DynamoDB e SQS):

```bash
cd app
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

Os cenários de BDD com Cucumber estão localizados em `app/src/test/resources/features/faturamento_saga.feature` e são executados automaticamente durante o comando acima.

### 2. Execução Local da Aplicação

Para rodar a aplicação localmente conectando a um LocalStack local:

```bash
# Iniciar LocalStack para DynamoDB e SQS
docker run -d --name localstack -p 4566:4566 -e SERVICES=sqs,dynamodb localstack/localstack:3

# Iniciar o microsserviço
cd app
./gradlew bootRun
```

A documentação Swagger interativa estará disponível em:
`http://localhost:8080/swagger-ui.html`

---

## CI/CD (GitHub Actions)

O pipeline em [.github/workflows/deploy.yml](.github/workflows/deploy.yml) é disparado a cada push na branch `main` ou via execução manual (`workflow_dispatch`):

1. **Testes & Cobertura:** Executa testes unitários, testes de integração, testes BDD (Cucumber) e valida a barreira de 80% de cobertura no JaCoCo.
2. **Construção da Imagem:** Cria a imagem Docker multi-stage otimizada para `linux/amd64` e envia para o **Amazon ECR**.
3. **Provisionamento Idempotente de Mensageria:** Garante a existência das filas SQS (com respectivas Dead Letter Queues - DLQ) e do tópico SNS `billing-events`.
4. **Deploy no Amazon EKS:** Conecta ao cluster Kubernetes, aplica ConfigMaps, Secrets e realiza o rollout progressivo com verificação de estabilidade (`kubectl rollout status`).

### Secrets do Repositório

| Secret | Descrição |
|---|---|
| `AWS_ACCESS_KEY_ID` | Chave de acesso AWS para deploy |
| `AWS_SECRET_ACCESS_KEY` | Chave secreta AWS |
| `AWS_SESSION_TOKEN` | Token de sessão da AWS (obrigatório para AWS Academy) |
| `MERCADO_PAGO_ACCESS_TOKEN` | Token de autenticação da API do Mercado Pago |

---

## Evidências de Testes e Cobertura

O projeto adota verificação estrita de cobertura com JaCoCo integrada ao ciclo de vida do build (`check` / `jacocoTestCoverageVerification`), garantindo no mínimo 80% de cobertura em instruções e ramificações.

### Resumo de Cobertura JaCoCo

| Métrica | Cobertura Obtida | Mínimo Exigido | Status |
|---|---|---|---|
| **Cobertura Geral de Instruções** | **96%** | **80%** | **Aprovado** |
| **Linhas de Código Cobertas** | **1.257 de 1.299 linhas** | - | **Aprovado** |
| **Testes Unitários / Integração / BDD** | **100% de sucesso (0 falhas)** | 100% | **Aprovado** |

Relatório completo gerado em: `app/build/reports/jacoco/test/html/index.html`.

---

## Endpoints da API (Swagger / OpenAPI)

O microsserviço expõe endpoints REST documentados via OpenAPI 3. Quando integrado via API Gateway, as rotas recebem o prefixo `/billing/`:

- `GET /api/orcamentos/os/{osId}`: Consulta o orçamento detalhado de uma OS.
- `GET /api/pagamentos/os/{osId}`: Consulta o status do pagamento e dados do QR Code.
- `POST /api/pagamentos/os/{osId}/iniciar`: Dispara manualmente o início da cobrança Pix.
- `POST /api/pagamentos/webhook`: Endpoint público para recepção de notificações de pagamento do Mercado Pago.
- `GET /api/pecas` e `POST /api/pecas`: Consulta e cadastro de tipos de peças do catálogo.
- `GET /api/servicos` e `POST /api/servicos`: Consulta e cadastro de tipos de serviços do catálogo.
- `GET /actuator/health`: Health check, liveness e readiness probes do Kubernetes.

