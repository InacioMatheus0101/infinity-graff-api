# INFINITY GRAFF API

> Marketplace de Serviços Criativos — Backend API  
> **Fase 1: Fundação Segura e Escalável**

---

## 1. Descrição do projeto e contexto

A **Infinity Graff API** é o backend da plataforma Infinity Graff, responsável por fornecer a base segura do marketplace de serviços criativos.

A **Fase 1** concentra a fundação técnica do sistema, preparando o projeto para evoluções posteriores. Nesta entrega, foram implementados:

- autenticação integrada ao **Supabase Auth**;
- validação de JWT no backend;
- perfil interno de usuário vinculado ao UUID do Supabase;
- controle de acesso por perfil;
- gestão administrativa de usuários;
- soft delete;
- auditoria de ações sensíveis;
- padronização de respostas e erros;
- migrations com Flyway;
- testes automatizados.

### Modelo de autenticação

A autenticação foi dividida em duas responsabilidades:

#### Supabase Auth

Responsável por:

- cadastro;
- login;
- sessão;
- refresh token;
- emissão do access token.

#### Backend Spring

Responsável por:

- validar o access token recebido no header `Authorization`;
- verificar emissor, audience e assinatura;
- extrair o `sub` do token;
- localizar o perfil interno correspondente na tabela `usuarios`;
- aplicar regras de autorização;
- bloquear usuários inativos ou removidos logicamente.

### Fluxo resumido

```text
Frontend autentica no Supabase Auth
        ↓
Supabase retorna access token
        ↓
Frontend chama a API com Bearer Token
        ↓
Backend valida o JWT
        ↓
Backend carrega o perfil interno
        ↓
Regras de autorização são aplicadas
```

---

## 2. Stack e versões

| Tecnologia | Versão / referência |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.14 |
| Spring Security | Gerenciado pelo Spring Boot |
| Spring Data JPA | Gerenciado pelo Spring Boot |
| Hibernate | Gerenciado pelo Spring Boot |
| PostgreSQL | Banco relacional via Supabase |
| Flyway | Gerenciado pelo Spring Boot |
| Maven | Maven Wrapper incluído no projeto |
| Lombok | Dependência do projeto |
| JUnit 5 | Infraestrutura de testes |
| Mockito | Infraestrutura de testes |
| Testcontainers | 1.20.6 |

> Dependências gerenciadas pelo POM do Spring Boot devem seguir o `pom.xml` vigente do projeto.

---

## 3. Pré-requisitos para rodar localmente

Antes de executar a aplicação, garanta que estão disponíveis:

- **Java 21** instalado;
- **Docker** instalado e em execução, necessário para os testes de integração com Testcontainers;

- acesso ao banco PostgreSQL/Supabase configurado;
- variáveis de ambiente definidas;
- terminal capaz de executar o Maven Wrapper do projeto.

### Verificações úteis

```bash
java -version
```

```bash
docker --version
```

---

## 4. Variáveis de ambiente

O projeto utiliza variáveis de ambiente para configuração.  
Segredos nunca devem ser versionados.

O arquivo `.env.example` pode ser usado como referência, mas o arquivo `.env` real não deve ser enviado ao repositório.

### Banco de dados

| Variável | Descrição | Obrigatória |
|---|---|---|
| `DATABASE_URL` | URL JDBC do PostgreSQL/Supabase | Sim |
| `DATABASE_USERNAME` | Usuário do banco | Sim |
| `DATABASE_PASSWORD` | Senha do banco | Sim |

### Pool de conexões

| Variável | Descrição | Valor padrão no `application.yml` |
|---|---|---|
| `DB_POOL_MAX_SIZE` | Tamanho máximo do pool | `10` |
| `DB_POOL_MIN_IDLE` | Quantidade mínima de conexões ociosas | `2` |
| `DB_POOL_CONNECTION_TIMEOUT` | Timeout para obtenção de conexão em milissegundos | `30000` |
| `DB_POOL_IDLE_TIMEOUT` | Tempo máximo de conexão ociosa em milissegundos | `600000` |
| `DB_POOL_MAX_LIFETIME` | Tempo máximo de vida de uma conexão em milissegundos | `1800000` |

### Supabase Auth

| Variável | Descrição | Obrigatória |
|---|---|---|
| `SUPABASE_PROJECT_URL` | URL base do projeto Supabase | Sim |
| `SUPABASE_ISSUER` | Issuer esperado do token | Sim |
| `SUPABASE_AUDIENCE` | Audience esperada do token | Sim |
| `SUPABASE_JWKS_URL` | URL JWKS usada na validação da assinatura do JWT | Sim |

### CORS

| Variável | Descrição | Obrigatória |
|---|---|---|
| `CORS_ALLOWED_ORIGINS` | Lista de origens permitidas separadas por vírgula | Sim |

Exemplo de valor local:

```env
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,http://localhost:8080
```

### Ambiente da aplicação

| Variável | Descrição | Obrigatória |
|---|---|---|
| `APP_VERSION` | Versão lógica exibida/configurada pela aplicação | Não |
| `APP_ENV` | Ambiente lógico: `development`, `staging` ou `production` | Sim |
| `SPRING_PROFILES_ACTIVE` | Profile Spring ativo | Sim |
| `TZ` | Timezone do processo | Recomendado |

### AdminSeeder em desenvolvimento

Essas variáveis são usadas apenas em ambiente `development`, pelo `AdminSeeder`.

| Variável | Descrição | Obrigatória em `development` |
|---|---|---|
| `ADMIN_INITIAL_USER_ID` | UUID de um usuário que já existe no Supabase Auth | Sim |
| `ADMIN_INITIAL_EMAIL` | E-mail do usuário administrador | Sim |
| `ADMIN_INITIAL_NAME` | Nome do usuário administrador | Sim |

Exemplo:

```env
ADMIN_INITIAL_USER_ID=uuid-do-usuario-existente-no-supabase
ADMIN_INITIAL_EMAIL=admin@infinitygraff.com
ADMIN_INITIAL_NAME=Administrador Infinity Graff
```

---

## 5. Como subir localmente

### 5.1. Configurar o ambiente

Configure as variáveis de ambiente necessárias no sistema operacional, na IDE ou em um arquivo `.env` carregado pelo ambiente de execução.

### 5.2. Executar a aplicação

#### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

#### Linux/macOS

```bash
./mvnw spring-boot:run
```

### 5.3. Endereço local

Por padrão, a API sobe em:

```text
http://localhost:8080
```

---

## 6. Como rodar as migrations Flyway

As migrations ficam no diretório:

```text
src/main/resources/db/migration
```

No estado atual do projeto, o Flyway está configurado para executar automaticamente as migrations durante a inicialização da aplicação.

### Rodar migrations no fluxo normal do projeto

#### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

#### Linux/macOS

```bash
./mvnw spring-boot:run
```

Ao subir a API:

1. a aplicação conecta no banco configurado;
2. o Flyway valida o histórico de migrations;
3. migrations pendentes são executadas;
4. o Hibernate valida o schema com `ddl-auto: validate`.

### Observação

O schema do banco **não é criado pelo Hibernate**.  
A fonte oficial de evolução do banco é o **Flyway**.

---

## 7. Como rodar os testes unitários e de integração

No estado atual do projeto, a suíte padrão executa os testes automatizados do projeto em conjunto.

### Rodar todos os testes

#### Windows

```powershell
.\mvnw.cmd test
```

#### Linux/macOS

```bash
./mvnw test
```

### Rodar build completo com testes

#### Windows

```powershell
.\mvnw.cmd clean package
```

#### Linux/macOS

```bash
./mvnw clean package
```

### Sobre os testes de integração

Os testes de integração utilizam **Testcontainers** com PostgreSQL.  
Por isso:

- o Docker precisa estar ativo;
- a execução pode levar mais tempo que os testes puramente unitários;
- falhas de inicialização do Docker impedem os testes de integração de subir o banco temporário.

---

## 8. Exemplos de chamadas dos endpoints principais

Todos os endpoints protegidos exigem:

```http
Authorization: Bearer SEU_ACCESS_TOKEN_SUPABASE
```

---

### 8.1. Completar perfil interno

```bash
curl -X POST http://localhost:8080/api/v1/autenticacao/completar-perfil \
  -H "Authorization: Bearer SEU_ACCESS_TOKEN_SUPABASE" \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Cliente Teste",
    "role": "CLIENTE",
    "aceitouTermos": true
  }'
```

---

### 8.2. Consultar meu perfil

```bash
curl http://localhost:8080/api/v1/autenticacao/meu-perfil \
  -H "Authorization: Bearer SEU_ACCESS_TOKEN_SUPABASE"
```

---

### 8.3. Listar usuários

```bash
curl "http://localhost:8080/api/v1/usuarios?page=0&size=20&sort=criadoEm,desc" \
  -H "Authorization: Bearer SEU_ACCESS_TOKEN_SUPABASE"
```

Exemplo com filtros:

```bash
curl "http://localhost:8080/api/v1/usuarios?role=CLIENTE&ativo=true&nome=Ana&page=0&size=20" \
  -H "Authorization: Bearer SEU_ACCESS_TOKEN_SUPABASE"
```

---

### 8.4. Buscar usuário por ID

```bash
curl http://localhost:8080/api/v1/usuarios/UUID_DO_USUARIO \
  -H "Authorization: Bearer SEU_ACCESS_TOKEN_SUPABASE"
```

---

### 8.5. Alterar status de usuário

```bash
curl -X PATCH http://localhost:8080/api/v1/usuarios/UUID_DO_USUARIO/status \
  -H "Authorization: Bearer SEU_ACCESS_TOKEN_SUPABASE" \
  -H "Content-Type: application/json" \
  -d '{
    "ativo": false
  }'
```

---

### 8.6. Aplicar soft delete em usuário

```bash
curl -X DELETE http://localhost:8080/api/v1/usuarios/UUID_DO_USUARIO \
  -H "Authorization: Bearer SEU_ACCESS_TOKEN_SUPABASE"
```

---

### 8.7. Consultar logs de auditoria

```bash
curl "http://localhost:8080/api/v1/auditoria/logs?page=0&size=50" \
  -H "Authorization: Bearer SEU_ACCESS_TOKEN_SUPABASE"
```

Exemplo com filtros:

```bash
curl "http://localhost:8080/api/v1/auditoria/logs?acao=USUARIO_STATUS_ATUALIZADO&page=0&size=50" \
  -H "Authorization: Bearer SEU_ACCESS_TOKEN_SUPABASE"
```

---

## 9. Criação do admin em produção

O `AdminSeeder` existe apenas para o profile `development`.  
Ele **não deve** ser usado como mecanismo de criação de administrador em produção.

### Procedimento recomendado para produção

1. Criar previamente o usuário administrador no **Supabase Auth**.
2. Obter o UUID desse usuário.
3. Criar manualmente o perfil interno correspondente na tabela `usuarios`, usando:
   - o mesmo UUID do Supabase;
   - o e-mail correto;
   - o nome desejado;
   - `role = ADMIN`;
   - `ativo = true`.
4. Validar o acesso administrativo com token emitido pelo Supabase Auth.

### Diretriz de segurança

A criação do primeiro administrador em produção deve ser tratada como operação controlada de infraestrutura ou implantação, nunca como fluxo público da aplicação.

---

## 10. Limitações conhecidas desta fase

A Fase 1 entrega a fundação da API, mas ainda não cobre:

- upload e tratamento de imagens/arquivos;
- chat entre cliente e prestador;
- painel de solicitação de artes;
- pagamentos e integrações financeiras;
- notificações assíncronas;
- WebSocket ou comunicação em tempo real;
- rate limiting;
- fluxos completos de negócio do marketplace;
- frontend.

---

## 11. Arquitetura de pacotes

```text
com.infinitygraff.api
├── auth/        → Fluxo de autenticação interna e conclusão de perfil
├── auditoria/   → Registro e consulta de logs de auditoria
├── common/      → Respostas, exceções e estruturas compartilhadas
├── config/      → Configurações da aplicação
├── infra/       → Infraestrutura auxiliar, como AdminSeeder
├── security/    → Validação JWT, contexto de segurança e handlers
└── usuario/     → Domínio de usuários, roles, serviços e repositórios
```

### Organização por responsabilidade

- `controller`: camada HTTP;
- `service`: regras de negócio;
- `repository`: acesso a dados;
- `dto`: contratos de entrada e saída;
- `model`: entidades de domínio;
- `enums`: enumerações do domínio;
- `exception`: exceções específicas quando aplicável.

### Princípios aplicados

- controllers finos;
- regras de negócio concentradas nos services;
- persistência isolada nos repositories;
- DTOs separados das entidades;
- respostas padronizadas;
- autorização validada no backend;
- auditoria de ações sensíveis.

---

## Status da Fase 1

A Fase 1 está pronta

