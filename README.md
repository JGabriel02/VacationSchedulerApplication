# Vacation Scheduler API

API REST para gerenciamento de férias de funcionários e gerentes.

O sistema permite o cadastro de gerentes, cadastro de funcionários vinculados a um gerente, autenticação com JWT, solicitação de férias e consulta dos períodos de férias da equipe.

A aplicação foi desenvolvida com Java, Spring Boot e PostgreSQL, e está preparada para execução local e deploy em produção utilizando Render e Neon.

---

## Sobre o projeto

O Vacation Scheduler foi criado para simplificar o gerenciamento de férias dentro de uma equipe.

Existem dois perfis de usuário:

* `EMPLOYEE`: funcionário;
* `MANAGER`: gerente.

O gerente recebe um código exclusivo ao criar sua conta. Esse código deve ser informado pelos funcionários durante o cadastro para que eles sejam vinculados à equipe correta.

### Funcionários podem

* criar uma conta usando o código de um gerente;
* realizar login;
* cadastrar períodos de férias;
* visualizar suas férias cadastradas.

### Gerentes podem

* criar uma conta;
* receber um código exclusivo;
* realizar login;
* visualizar as férias dos funcionários vinculados à sua equipe.

---

## Tecnologias utilizadas

* Java 21
* Spring Boot
* Spring Web
* Spring Data JPA
* Spring Security
* Jakarta Validation
* JWT
* PostgreSQL
* H2 para testes
* Maven
* Docker
* Render
* Neon PostgreSQL
* JUnit
* Mockito

---

## Arquitetura

O projeto segue uma organização em camadas:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Estrutura aproximada:

```text
src/
├── main/
│   ├── java/
│   │   └── com/JoaoGabriel/vacation_scheduler/
│   │       ├── auth/
│   │       ├── config/
│   │       ├── employee/
│   │       │   ├── controller/
│   │       │   ├── dto/
│   │       │   ├── entity/
│   │       │   ├── repository/
│   │       │   └── service/
│   │       ├── manager/
│   │       │   ├── controller/
│   │       │   ├── dto/
│   │       │   └── service/
│   │       ├── security/
│   │       ├── vacation/
│   │       │   ├── controller/
│   │       │   ├── dto/
│   │       │   ├── entity/
│   │       │   ├── repository/
│   │       │   └── service/
│   │       └── VacationSchedulerApplication.java
│   └── resources/
│       └── application.properties
└── test/
    ├── java/
    └── resources/
```

---

## Requisitos

Para executar localmente, você precisa de:

* Java 21
* Maven ou Maven Wrapper
* PostgreSQL
* Git

Docker é opcional para desenvolvimento local, mas o projeto possui `Dockerfile` para deploy.

---

## Configuração do Java

Confira a versão instalada:

```bash
java -version
```

O resultado deve indicar Java 21.

No Windows, caso existam várias versões instaladas:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.10"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

java -version
.\mvnw.cmd -v
```

Ajuste o caminho conforme a instalação disponível no computador.

---

## Clonando o projeto

```bash
git clone URL_DO_REPOSITORIO
cd vacation-scheduler
```

---

## Variáveis de ambiente

A aplicação utiliza variáveis de ambiente para proteger dados sensíveis.

Variáveis necessárias:

```env
SPRING_DATASOURCE_URL=
JWT_SECRET=
JWT_EXPIRATION=3600000
SPRING_JPA_HIBERNATE_DDL_AUTO=update
PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

`CORS_ALLOWED_ORIGINS` aceita várias origens separadas por vírgula. Em produção, inclua a URL do frontend publicado, por exemplo `http://localhost:5173,https://meu-frontend.com`.

Como a URL PostgreSQL pode conter usuário e senha, este projeto pode utilizar uma única variável:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST/BANCO?user=USUARIO&password=SENHA&sslmode=require
```

Não coloque credenciais reais no código ou no GitHub.

---

## application.properties

O arquivo `src/main/resources/application.properties` deve utilizar variáveis de ambiente:

```properties
spring.application.name=vacation-scheduler

spring.datasource.url=${SPRING_DATASOURCE_URL}

spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:update}
spring.jpa.open-in-view=false

jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION:3600000}

server.port=${PORT:8080}
```

Caso as credenciais do banco sejam separadas, utilize:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
```

---

## Gerando o JWT_SECRET

No PowerShell:

```powershell
$bytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Guarde o resultado em uma variável de ambiente chamada:

```env
JWT_SECRET=
```

Nunca publique essa chave.

---

## Executando com PostgreSQL local

Crie um banco:

```sql
CREATE DATABASE vacation_db;
```

Exemplo de configuração temporária no PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/vacation_db?user=postgres&password=SUA_SENHA"
$env:JWT_SECRET="SUA_CHAVE_BASE64"
$env:JWT_EXPIRATION="3600000"
$env:SPRING_JPA_HIBERNATE_DDL_AUTO="update"
$env:PORT="8080"
```

Depois execute:

```powershell
.\mvnw.cmd spring-boot:run
```

Ou:

```powershell
.\mvnw.cmd clean package
java -jar target\vacation-scheduler-0.0.1-SNAPSHOT.jar
```

No Linux ou macOS:

```bash
./mvnw spring-boot:run
```

---

## Executando os testes

```bash
./mvnw clean test
```

No Windows:

```powershell
.\mvnw.cmd clean test
```

Os testes utilizam H2 em memória.

Os testes de integração usam o perfil `test`, configurado em:

```text
src/test/resources/application-test.properties
```

```properties
spring.application.name=vacation-scheduler-test

spring.datasource.url=jdbc:h2:mem:vacation_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.open-in-view=false

jwt.secret=dGVzdF9qd3Rfc2VjcmV0X2tleV9mb3JfdmFjYXRpb25fc2NoZWR1bGVyXzIwMjY=
jwt.expiration=3600000

app.cors.allowed-origins=http://localhost:5173
```

Nenhum banco externo ou variável de ambiente é necessário para rodar os testes.

---

## Executando com Docker

O projeto possui um `Dockerfile`.

Exemplo:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY src src

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Para construir:

```bash
docker build -t vacation-scheduler-api .
```

Para executar:

```bash
docker run \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="SUA_URL_POSTGRESQL" \
  -e JWT_SECRET="SUA_CHAVE_JWT" \
  -e JWT_EXPIRATION="3600000" \
  vacation-scheduler-api
```

---

## Deploy em produção

A aplicação pode ser publicada utilizando:

* Render para a API;
* Neon para o PostgreSQL.

Arquitetura:

```text
Frontend
   ↓
API Spring Boot no Render
   ↓
PostgreSQL no Neon
```

---

## Configuração do Neon

Crie um projeto PostgreSQL no Neon.

A string fornecida pode ser parecida com:

```text
postgresql://USUARIO:SENHA@HOST/BANCO?sslmode=require
```

Para o Spring Boot, utilize:

```text
jdbc:postgresql://HOST/BANCO?user=USUARIO&password=SENHA&sslmode=require
```

Exemplo de variável:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST/neondb?user=USUARIO&password=SENHA&sslmode=require&channelBinding=require
```

A senha pode estar embutida na própria string.

Nunca publique essa variável no repositório.

---

## Configuração do Render

No Render:

1. Crie um novo Web Service.
2. Conecte o repositório GitHub.
3. Escolha a branch principal.
4. Selecione `Docker` como runtime.
5. Deixe o Root Directory vazio, caso o projeto esteja na raiz.
6. Use o caminho:

```text
./Dockerfile
```

Adicione as variáveis:

```env
SPRING_DATASOURCE_URL=SUA_URL_DO_NEON
SPRING_JPA_HIBERNATE_DDL_AUTO=update
JWT_SECRET=SUA_CHAVE_BASE64
JWT_EXPIRATION=3600000
```

O Render fornece automaticamente a variável:

```env
PORT
```

A aplicação utiliza:

```properties
server.port=${PORT:8080}
```

---

## URL pública

Após o deploy, a API ficará disponível em um endereço semelhante a:

```text
https://vacationschedulerapplication.onrender.com
```

A URL pode variar conforme o nome configurado no Render.

---

## Autenticação

A aplicação utiliza JWT.

Após o login, envie o token nas rotas protegidas:

```http
Authorization: Bearer SEU_TOKEN
```

Exemplo:

```http
GET /managers/vacations
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## Perfis

### EMPLOYEE

Funcionário vinculado a um gerente.

### MANAGER

Gerente responsável por funcionários.

Enum:

```java
public enum EmployeeRole {
    EMPLOYEE,
    MANAGER
}
```

---

## Endpoints

### Cadastro de gerente

```http
POST /managers
Content-Type: application/json
```

Body:

```json
{
  "nome": "Gestor Teste",
  "email": "gestor@teste.com",
  "password": "senha123",
  "admissionDate": "2024-01-10"
}
```

Resposta:

```json
{
  "id": 1,
  "nome": "Gestor Teste",
  "email": "gestor@teste.com",
  "role": "MANAGER",
  "managerCode": "SMAUZXV2"
}
```

O `managerCode` deve ser compartilhado com os funcionários da equipe.

---

### Cadastro de funcionário

```http
POST /employees
Content-Type: application/json
```

Body:

```json
{
  "nome": "Funcionário Teste",
  "email": "funcionario@teste.com",
  "password": "senha123",
  "admissionDate": "2024-02-01",
  "managerCode": "SMAUZXV2"
}
```

O funcionário será associado ao gerente correspondente ao código informado.

---

### Login

```http
POST /auth/login
Content-Type: application/json
```

Body:

```json
{
  "email": "funcionario@teste.com",
  "password": "senha123"
}
```

A resposta retorna um token JWT.

Exemplo:

```json
{
  "token": "SEU_TOKEN_JWT"
}
```

A estrutura exata pode variar conforme o DTO implementado.

---

### Cadastro de férias

```http
POST /vacations
Authorization: Bearer TOKEN
Content-Type: application/json
```

Body:

```json
{
  "startDate": "2026-07-01",
  "endDate": "2026-07-15"
}
```

Resposta esperada:

```json
{
  "id": 1,
  "startDate": "2026-07-01",
  "endDate": "2026-07-15",
  "totalDays": 15,
  "employeeId": 2,
  "employeeName": "Funcionário Teste"
}
```

---

### Listagem das férias do funcionário

```http
GET /vacations
Authorization: Bearer TOKEN
```

Retorna os períodos de férias do funcionário autenticado.

---

### Listagem das férias da equipe

```http
GET /managers/vacations
Authorization: Bearer TOKEN
```

Disponível apenas para usuários com perfil `MANAGER`.

Resposta:

```json
[
  {
    "id": 1,
    "startDate": "2026-07-01",
    "endDate": "2026-07-15",
    "totalDays": 15,
    "employeeId": 2,
    "employeeName": "Funcionário Teste"
  }
]
```

---

## Resumo das rotas

| Método | Rota                  | Acesso      | Descrição              |
| ------ | --------------------- | ----------- | ---------------------- |
| POST   | `/managers`           | Público     | Cadastra gerente       |
| POST   | `/employees`          | Público     | Cadastra funcionário   |
| POST   | `/auth/login`         | Público     | Realiza login          |
| POST   | `/vacations`          | Autenticado | Cadastra férias        |
| GET    | `/vacations`          | Funcionário | Lista férias próprias  |
| GET    | `/managers/vacations` | Gerente     | Lista férias da equipe |

---

## Códigos HTTP

A API utiliza códigos como:

| Código | Significado                               |
| ------ | ----------------------------------------- |
| 200    | Requisição realizada com sucesso          |
| 201    | Recurso criado com sucesso                |
| 400    | Dados inválidos                           |
| 401    | Autenticação necessária ou token inválido |
| 403    | Usuário sem permissão                     |
| 404    | Recurso não encontrado                    |
| 409    | Conflito de dados                         |
| 500    | Erro interno                              |

---

## Validações

Exemplo do cadastro de funcionário:

```java
public record EmployeeRequest(

        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String password,

        @NotNull(message = "A data de admissão é obrigatória")
        @PastOrPresent(message = "A data de admissão não pode estar no futuro")
        LocalDate admissionDate,

        @NotBlank(message = "O código do gestor é obrigatório")
        String managerCode

) {
}
```

Principais validações:

* nome obrigatório;
* e-mail obrigatório e válido;
* senha com no mínimo 6 caracteres;
* data de admissão obrigatória;
* data de admissão não pode estar no futuro;
* código do gerente obrigatório para funcionários;
* data final das férias não pode ser anterior à inicial.

---

## Segurança

A aplicação utiliza Spring Security e JWT.

Rotas públicas:

```text
POST /employees
POST /managers
POST /auth/login
```

Demais rotas exigem autenticação.

Exemplo de configuração:

```java
.requestMatchers(HttpMethod.POST, "/employees").permitAll()
.requestMatchers(HttpMethod.POST, "/managers").permitAll()
.requestMatchers("/auth/**").permitAll()
.anyRequest().authenticated()
```

---

## Fluxo de autenticação

```text
Usuário envia e-mail e senha
        ↓
API valida as credenciais
        ↓
API gera JWT
        ↓
Frontend armazena o token
        ↓
Frontend envia Authorization: Bearer TOKEN
        ↓
Filtro JWT valida o token
        ↓
Usuário autenticado acessa a rota
```

---

## Integração com frontend

O frontend deve utilizar a URL pública da API:

```env
VITE_API_BASE_URL=https://vacationschedulerapplication.onrender.com
```

Exemplo com Axios:

```ts
import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 60000,
});
```

Interceptor:

```ts
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});
```

---

## CORS

Como o frontend será hospedado em outro domínio, será necessário liberar esse domínio no backend.

Exemplo:

```java
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://localhost:5173",
                                "https://SEU-FRONTEND.com"
                        )
                        .allowedMethods(
                                "GET",
                                "POST",
                                "PUT",
                                "PATCH",
                                "DELETE",
                                "OPTIONS"
                        )
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
```

Em produção, evite liberar todas as origens com `*`.

---

## Observação sobre o plano gratuito do Render

No plano gratuito, a aplicação pode entrar em suspensão após algum tempo sem requisições.

A primeira chamada após a suspensão pode levar vários segundos.

Isso é esperado.

O frontend deve:

* exibir loading;
* utilizar timeout maior;
* informar que a API pode estar inicializando;
* evitar mostrar erro imediatamente.

---

## Persistência

Os dados são persistidos no PostgreSQL do Neon.

O Render executa apenas a aplicação.

```text
Render
  └── API Spring Boot

Neon
  └── PostgreSQL
```

A aplicação pode reiniciar sem perder dados porque o banco está hospedado separadamente.

---

## .gitignore

Exemplo:

```gitignore
target/
.env
application-local.properties

.idea/
*.iml
.vscode/

*.log

.DS_Store
Thumbs.db
```

---

## .dockerignore

Exemplo:

```dockerignore
target
.git
.github
.idea
.vscode
*.iml
.env
application-local.properties
README.md
```

---

## Boas práticas de segurança

* nunca publique o `JWT_SECRET`;
* nunca publique a senha do Neon;
* nunca envie tokens em screenshots;
* nunca coloque credenciais no GitHub;
* rotacione credenciais expostas;
* utilize HTTPS em produção;
* mantenha dependências atualizadas;
* valide permissões no backend;
* não confie apenas no frontend para autorização;
* utilize senhas criptografadas com BCrypt.

---

## Melhorias futuras

* recuperação de senha;
* confirmação de e-mail;
* edição de perfil;
* cancelamento de férias;
* aprovação ou rejeição de solicitações;
* status de férias;
* histórico de alterações;
* notificações;
* documentação com Swagger/OpenAPI;
* paginação;
* filtros avançados;
* testes de integração;
* Flyway para migrations;
* refresh token;
* cookies HTTP-only;
* logs estruturados;
* monitoramento;
* painel administrativo;
* deploy do frontend;
* pipeline CI/CD com GitHub Actions.

---

## Fluxo principal

### Gerente

```text
Cadastro
→ recebe managerCode
→ login
→ consulta férias da equipe
```

### Funcionário

```text
Recebe managerCode
→ cadastro
→ login
→ solicita férias
→ visualiza períodos cadastrados
```

---

## Status do projeto

O backend está funcional e publicado.

Funcionalidades implementadas:

* cadastro de gerente;
* geração de código exclusivo;
* cadastro de funcionário vinculado;
* autenticação JWT;
* cadastro de férias;
* listagem das férias do funcionário;
* listagem das férias da equipe;
* banco PostgreSQL em produção;
* deploy no Render;
* integração com Neon.

---

## Autor

Desenvolvido por João Gabriel.

---

## Licença

Este projeto pode ser utilizado para fins de estudo, portfólio e desenvolvimento pessoal.

Caso deseje disponibilizá-lo como software aberto, adicione um arquivo `LICENSE` ao repositório.

Uma opção comum é a licença MIT.
