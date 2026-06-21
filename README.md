# PES (Project Execution System)

[![CI](https://github.com/X0005274/pes-boot/actions/workflows/ci.yml/badge.svg)](https://github.com/X0005274/pes-boot/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/X0005274/pes-boot?sort=semver)](https://github.com/X0005274/pes-boot/releases/latest)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen)

반도체 제조 MES 영역에서 **LOT 중심 이벤트**를 관리하는 시스템입니다.
TIBCO Rendezvous 기반 메시징 위에 Spring Boot Biz 레이어를 결합해, **LOT / WF / DURABLE**
도메인의 이벤트를 공통 **Workflow 메시지 스키마**로 통합 처리합니다.

> 상세 도메인 계약·메시징 규약은 [`CLAUDE.md`](./CLAUDE.md) 참조.

## 관련 저장소

이 저장소는 **Biz 레이어(서버)** 입니다. UI 클라이언트는 별도 저장소에 있습니다.

| 저장소 | 레이어 | 설명 |
|---|---|---|
| **pes-boot** (이 저장소) | PES.BIZ (Java/Spring Boot) | 워크플로우 처리·조회·HubDB 적재·PES.UI 포워더 |
| [pes-ui](https://github.com/X0005274/pes-ui) | C# UI (.NET Framework 4.8) | TIBCO RV 로 LOT/WF/DURABLE 요청 송신 |

```
[pes-ui (C# UI)] --PES.UI.*.REQUEST--> [PES.UI 포워더] --PES.BIZ.*.EVENT--> [pes-boot (Java Biz)]
                 <----------------- RV INBOX Reply (PesProcessResult) -----------------
```
- 양쪽은 동일한 Workflow JSON 메시지 계약(camelCase) + RV `json` 필드 규약을 공유합니다.
- PES.UI 포워더는 이 저장소에 포함(`pes.forwarder.enabled=true` 로 활성화).

---

## 기술 스택

| 영역 | 사용 |
|---|---|
| Language / Runtime | Java 17 LTS |
| Framework | Spring Boot 4.1.x (Web / Data JPA / Validation / Actuator) |
| DB | Oracle AI Database 26ai (운영) · H2 (테스트) |
| Messaging | TIBCO Rendezvous 8.4.x (profile) |
| Migration | Flyway |
| API Docs | springdoc-openapi (Swagger UI) |
| Boilerplate | Lombok |
| Build | Maven (멀티모듈) |

---

## 모듈 구조

```
pes-boot (parent)
├── pes-common          공통 VO/엔티티 임베디드/라우터/결과/관측/멱등 SPI/조회 VO
├── pes-messaging       전송 무관 인바운드 파이프라인(디스패처·이벤트 발행·전송 포트)
├── pes-idempotency     correlationId 기반 중복 처리 방지(@Primary 데코레이터)
├── pes-lot-service     LOT 도메인 (DTO/엔티티/리포지토리/워크플로우/서비스/REST)
├── pes-wf-service      WF 도메인 (동일 패턴)
├── pes-durable-service DURABLE 도메인 (동일 패턴)
├── pes-hub             HubDB 적재 서비스/스케줄러/잠금·소스 SPI
├── pes-hub-jdbc        HubDB 2nd DataSource 어댑터(MAS/HIS 소스, 영속 커서, 비관적 잠금)
├── pes-app             부트스트랩 (스캔·Oracle/JPA·Flyway·actuator·OpenAPI·통합테스트)
└── pes-rv-tibco        TIBCO RV 바인딩 (profile: rv-tibco, 상용 jar 필요)
```

base package: `com.playtogether.pes`

---

## 빌드 & 실행

### 빌드/테스트
```bash
mvn clean test        # 통합테스트(H2) 포함 전체 검증
mvn -DskipTests package
```

### 실행 (Oracle 연결, 환경변수 기반)
```bash
PES_DB_URL=jdbc:oracle:thin:@//host:1521/PESPDB \
PES_DB_USER=pes PES_DB_PASSWORD=*** \
java -jar pes-app/target/pes-app-0.0.2.jar
# 기동 시 Flyway 가 스키마 적용 → Hibernate validate
```

### Docker (GHCR public 이미지)
릴리즈마다 `ghcr.io/x0005274/pes-boot` 에 이미지가 게시됩니다(공개, 인증 불필요).
```bash
docker pull ghcr.io/x0005274/pes-boot:v0.0.2     # 또는 :latest

docker run -d -p 8080:8080 \
  -e PES_DB_URL=jdbc:oracle:thin:@//host:1521/PESPDB \
  -e PES_DB_USER=pes -e PES_DB_PASSWORD=*** \
  ghcr.io/x0005274/pes-boot:v0.0.2
```
로컬 빌드로 실행하려면 [`Dockerfile`](./Dockerfile) / [`docker-compose.yml`](./docker-compose.yml) 참조.

### 주요 환경변수
| 변수 | 기본값 | 설명 |
|---|---|---|
| `PES_DB_URL/USER/PASSWORD` | 로컬 | PES Oracle 접속 |
| `PES_JPA_DDL_AUTO` | `validate` | 운영은 Flyway 가 스키마 관리 |
| `PES_FLYWAY_ENABLED` | `true` | 마이그레이션 적용 |
| `PES_HUB_ENABLED` | `false` | HubDB 적재 어댑터 활성화 |
| `PES_HUB_DB_*` | - | HubDB 접속 |
| `PES_HUB_SCHED_ENABLED` | `false` | Hub 적재 스케줄러 |
| `PES_RV_SERVICE/NETWORK/DAEMON` | - | TIBCO RVD (profile rv) |

---

## 처리 흐름

```
UI/RV → PES.BIZ.<domain>.EVENT (인바운드)
  → PesInboundDispatcher (역직렬화·검증)
  → PesMessageProcessor (correlationId 멱등 검사/발급)
  → PesDomainRouter (entityType → 도메인 서비스)
  → *MessageService.handle (@Transactional)
      → 워크플로우 순차 처리 (created/released/changeSpec ...)
      → MAS 갱신 + HIS 적재(TIMEKEY 서버 생성)
      → 크로스 도메인 연계(createWf/createDurable/makeDurableInUse/applyToLot ...)
  → 성공: afterCommit 이벤트 push (PES.UI.<domain>.EVENT)
  → 실패: 전체 롤백(원자성) + 구조화된 결과 반환
```

- **멀티 step**: 한 요청의 `workflow[]`를 순차 처리, 한 step 실패 시 이후 SKIPPED + 전체 롤백
- **관측**: 처리 전 구간에 `correlationId` MDC 전파(로그 추적)

---

## REST API

명령(POST)·조회(GET) 분리(CQRS 스타일). 전체 명세는 기동 후 Swagger UI 참조.

| Method | Path | 설명 |
|---|---|---|
| POST | `/lot` `/wf` `/durable` | 워크플로우 메시지 처리 → `PesProcessResult` |
| GET | `/lot/{id}` 등 | 현재 상태(MAS) 조회 |
| GET | `/lot/{id}/history` 등 | 이력(HIS) 조회 — `page/size/method/from/to` |
| POST | `/hub/ingest?max=` | HubDB 적재 수동 트리거 |

- **OpenAPI**: `/v3/api-docs` · **Swagger UI**: `/swagger-ui/index.html`
- **에러**: `400`(검증/계약) · `404`(미존재) · `501`(도메인 핸들러 미등록), `ProblemDetail`
- **Actuator**: `/actuator/health|info|metrics`

### 예시 (LOT created+released)
```bash
curl -X POST http://localhost:8080/lot -H "Content-Type: application/json" -d '{
  "entityType":"LOT","lotId":"LOT12345","wfId":"WF56789","durableId":"DUR001",
  "workflow":[
    {"method":"created","options":{"createWf":true},"event":{"eventCd":"CREATED","statTyp":"NEW"}},
    {"method":"released","options":{"makeDurableInUse":true},"event":{"eventCd":"RELEASED","statTyp":"REL"}}
  ],
  "meta":{"srcSystem":"UI","userId":"UIUSER01","correlationId":"TX-1"}
}'
```

---

## 핵심 설계

- **공통 Workflow 스키마**: `entityType` + 도메인 ID + `workflow[]`(method/options/event) + `meta`
- **크로스 도메인**: 협력 SPI(`pes-common`)로 모듈 순환 회피, 실패 시 단일 트랜잭션 롤백
- **멱등성**: `correlationId` PK 마커(없으면 서버 `PES-<uuid>` 발급), 처리+마커 동일 트랜잭션
- **이벤트 발행**: `afterCommit` 동기화로 커밋 성공 시에만 발행(파생 도메인 포함)
- **메시징 분리**: 전송 포트(`PesMessageTransport`) — in-memory(기본) / TIBCO RV(profile)

### HubDB 적재
- **모드**: `pes.hub.mode=mas`(초기 적재) · `his`(이벤트 증분 replay)
- **영속 커서**: `PES_HUB_CURSOR`로 재시작 후 증분 이어가기
- **at-least-once**: 처리 성공 연속 구간까지만 커서 전진(실패는 재시도+멱등)
- **도메인별 병렬 비관적 잠금**: 도메인 간 병렬, 동일 도메인 직렬화

---

## 데이터 모델 (Oracle, Flyway)

| 테이블 | 설명 |
|---|---|
| `PES_{LOT,WF,DURABLE}_MAS` | 현재 상태(스냅샷), `@Version` 낙관적 잠금 |
| `PES_{LOT,WF,DURABLE}_HIS` | 이력, PK = (도메인ID, TIMEKEY) |
| `PES_PROCESSED_MSG` | 멱등 마커 (correlationId) |
| `PES_HUB_CURSOR` | Hub 적재 워터마크/잠금 |

---

## 테스트

```bash
mvn test     # 42 integration tests (H2, Flyway 비활성)
```
명령·조회·크로스도메인 원자성·이벤트 afterCommit·멱등·자동 correlationId·관측·Hub(MAS/HIS/커서/at-least-once/병렬잠금)·OpenAPI 커버.

---

## TIBCO RV (운영 메시징)

상용 `tibrvj.jar` 가 필요하며 profile 로 분리되어 기본 빌드에서 제외됩니다.
```bash
TIBRV_HOME=/opt/tibco/tibrv mvn -Prv-tibco -DskipTests package
java -Dspring.profiles.active=rv -jar pes-app/target/pes-app-0.0.1-SNAPSHOT.jar
```
인바운드 `PES.BIZ.<domain>.EVENT` 수신 → 처리 → RV INBOX reply + `PES.UI.<domain>.EVENT` push.

<!-- docs-only change: CI skip 실증 -->
