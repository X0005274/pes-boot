# CLAUDE.md – PES Project Guide

## 1. Overview

PES(Project Execution System)는 반도체 제조용 MES 영역에서 LOT 중심의 이벤트를 관리하는 시스템입니다.  
이 프로젝트는 기존 TIBCO Rendezvous 기반 인프라 위에, C# .NET UI와 Spring Boot 기반 Biz 서비스를 결합해 LOT, WF, DURABLE 도메인의 이벤트를 통합적으로 처리하는 것을 목표로 합니다.

PES는 다음과 같은 흐름을 가집니다.

- C# UI(.NET Framework 4.8)에서 TIBCO Rendezvous를 통해 PES.UI 레이어로 LOT 관련 요청을 전송합니다.[web:21][web:20]
- PES.UI 레이어는 수신한 메시지를 검증하고, 동일한 메시지 스키마를 유지한 채 PES.BIZ 레이어로 전달합니다.
- PES.BIZ 레이어(Spring Boot 4.1 기반)는 LOT 중심의 워크플로우(workflow 배열)를 해석하여, LOT/WF/DURABLE 관련 JPA 엔티티를 갱신하고 Oracle 26ai에 이벤트 이력을 적재합니다.[web:1][web:9][web:33]
- HubDB는 PES와 동일한 스키마를 갖는 읽기 전용 입력 소스로, HubDB에서 읽어온 데이터를 PES DB에 적재할 때도 동일한 Biz 레이어와 메시지 스키마를 사용합니다.

이 문서는 주로 LOT 도메인 중심으로 설계된 메시징/워크플로우 모델을 설명하며, WF와 DURABLE은 동일 패턴으로 확장할 수 있도록 여지를 남겨둡니다.

---

## 2. Tech Stack & Constraints

### 2.1 Languages & Runtimes

- **Java**
  - Java 17 LTS
  - Spring Boot 4.1.x (Maven Central 기준 latest stable)[web:1][web:37]
- **C#**
  - .NET Framework 4.8
  - Visual Studio 2026 (IDE)[web:31][web:32][web:36]

### 2.2 Frameworks & Libraries

- **Backend (Biz Layer)**
  - Spring Boot 4.1.x
    - Spring Web, Spring Data JPA, Spring Validation
    - Spring Boot 4.1 라인은 OpenTelemetry SDK, HTTP SSRF 완화 등 관찰/보안 기능이 강화된 릴리스입니다.[web:1][web:7][web:37]
- **Database**
  - Oracle AI Database 26ai
    - 버전: 23.26.1.0.0 계열 GA[web:9][web:33]
    - LOT/WF/DURABLE 관련 테이블은 PES 전용 스키마에 위치
- **Messaging**
  - TIBCO Rendezvous 8.4.x
  - UI와 Biz는 RV NetTransport를 통해 RVD에 접속하고, Request/Reply에서는 Rendezvous INBOX 메커니즘을 사용합니다.[web:27][page:1]

### 2.3 Project Layout

- **Domain & Package**
  - Java base package: `com.playtogether.pes`
  - LOT/WF/DURABLE 도메인별로 모듈 또는 패키지 분리
- **Build**
  - Maven (3.9.x latest)
  - 멀티 모듈 프로젝트 구조 (예: `pes-common`, `pes-lot-service`, `pes-wf-service`, `pes-durable-service`)

### 2.4 Constraints for Code Generation

- **공통 규칙**
  - var 키워드 사용 금지 (Java/C# 모두)
  - Nullable/Optional 처리 시 명시적인 타입 사용
- **Java (Biz Layer)**
  - JPA 사용 필수 (LOT, WF, DURABLE의 MAS/HIS 테이블 매핑)
  - Oracle 26ai용 DataSource 및 JPA 설정은 Spring Boot 4.1 방식에 맞게 구성
  - **Lombok 사용 허용** (최신 버전, Spring Boot BOM 의 `lombok.version` 오버라이드)
    - 적용 대상: 가변 클래스(JPA 엔티티 `*Mas`/`*His`, `@Embeddable` VO, 복합키 `*HisId`, `@ConfigurationProperties` POJO 등)
      - `@Getter` / 가변 필드 `@Setter` / `@NoArgsConstructor(access = PROTECTED)`(JPA용) / `@AllArgsConstructor` / 복합키는 `@EqualsAndHashCode`
      - PK·`@Version` 필드는 setter 미생성, 의미 있는 생성자(예: `Ctor(id)`)와 정적 팩토리(`from`/`toModel`)는 유지
    - **미적용 대상**: 메시지 DTO·뷰·결과 등 `record` 는 이미 접근자/생성자/equals 를 제공하므로 Lombok 을 쓰지 않는다
    - 의존성은 `optional` (annotation processor, 실행 jar 에서 제외)
  - 환경변수 기반 설정:
    - RVD Service / Network / Daemon / Default Subjects를 OS 환경변수로 정의
- **C# (UI Layer)**
  - .NET Framework 4.8 프로젝트
  - TIBCO Rendezvous 클라이언트 래퍼를 사용해 PES.UI.* Subject로 메시지 송수신
- **Messaging Contracts**
  - 메시지는 공통 Workflow 기반 스키마를 반드시 따른다 (본 문서 4장 참조)
  - Subject 네이밍은 PES.UI / PES.BIZ 규칙을 따른다 (본 문서 4.2 참조)

Claude가 코드를 생성할 때는 위 제약을 반드시 준수해야 합니다.

---

## 4. Messaging Model (TIBCO RV + Workflow)

### 4.1 Core Pattern

PES는 TIBCO Rendezvous를 사용하며, 모든 도메인(LOT, WF, DURABLE)은 공통된 **Workflow 기반 메시지 구조**를 사용합니다.[page:0][page:1]

- 각 메시지는 다음 상위 필드를 갖습니다.
  - `entityType`: `"LOT"`, `"WF"`, `"DURABLE"` 중 하나
  - 도메인별 ID: `lotId`, `wfId`, `durableId` 중 해당되는 것
  - `workflow`: 수행할 작업(step)들의 배열
  - `meta`: 공통 메타데이터 (srcSystem, userId, correlationId 등)
- `workflow` 배열의 각 요소는 **단일 행위**를 표현합니다.
  - `method`: 작업 이름 (예: `created`, `released`, `changeSpec`, `makeInUse`)
  - `options`: 해당 작업 수행 시 사용할 옵션 플래그
  - `event`: HIS 테이블에 기록할 이벤트 정보 (eventCd, eventDesc, statTyp 등)

한 요청에 여러 step을 포함할 수 있으며, PES.BIZ 레이어는 `workflow` 배열을 **순차적으로** 처리합니다.

### 4.2 Subject Conventions

PES에서는 TIBCO Rendezvous Subject를 다음 패턴에 따라 사용합니다.

- 기본 형태: `PES.<layer>.<domain>.<type>`[page:0]
  - `PES`: 시스템명
  - `<layer>`: `UI` 또는 `BIZ`
  - `<domain>`: `LOT`, `WF`, `DURABLE`
  - `<type>`: `REQUEST`, `EVENT` 등 역할

#### UI Layer Subjects

- Request (UI → PES.UI)
  - `PES.UI.LOT.REQUEST`
  - `PES.UI.WF.REQUEST`
  - `PES.UI.DURABLE.REQUEST`
- Event Push (Biz → UI)
  - `PES.UI.LOT.EVENT`
  - `PES.UI.WF.EVENT`
  - `PES.UI.DURABLE.EVENT`
- Reply:
  - TIBCO Rendezvous의 표준 Request/Reply 메커니즘을 사용하며, Reply Subject는 `_INBOX...` 형식으로 자동 생성되는 인박스를 사용합니다.[page:1][web:26]

#### Biz Layer Subjects

- Orchestrator / Domain Events
  - `PES.BIZ.LOT.EVENT`
  - `PES.BIZ.WF.EVENT`
  - `PES.BIZ.DURABLE.EVENT`

PES.UI 레이어는 `PES.UI.*` Subject로 들어온 LOT/WF/DURABLE 메시지를 검증한 후, 동일한 Body를 유지한 채 `PES.BIZ.*` Subject로 전달하는 역할을 합니다.

### 4.3 LOT Message Flow Example

아래는 LOT에 대해 `created`와 `released`를 한 번의 요청으로 순차 실행하는 예시입니다.

```json
{
  "entityType": "LOT",
  "lotId": "LOT12345",
  "wfId": "WF56789",
  "durableId": "DUR001",
  "workflow": [
    {
      "method": "created",
      "options": {
        "createWf": true
      },
      "event": {
        "eventCd": "CREATED",
        "eventDesc": "LOT created from UI",
        "statTyp": "NEW"
      }
    },
    {
      "method": "released",
      "options": {
        "makeDurableInUse": true
      },
      "event": {
        "eventCd": "RELEASED",
        "eventDesc": "LOT released and FOUP bound",
        "statTyp": "REL"
      }
    }
  ],
  "meta": {
    "srcSystem": "UI",
    "userId": "UIUSER01",
    "correlationId": "TX-20260620-0001"
  }
}
```

- UI → PES.UI: `PES.UI.LOT.REQUEST` 로 위와 같은 메시지를 전송
- PES.UI → PES.BIZ: `PES.BIZ.LOT.EVENT` 로 동일 메시지를 전달
- PES.BIZ: `workflow` 배열을 순차적으로 처리 (created → released)
- Biz 처리 결과는 RV Reply(INBOX subject)를 통해 UI로 반환되며, 상태 변경은 `PES.UI.LOT.EVENT`, `PES.UI.DURABLE.EVENT` 등을 통해 Push 할 수 있습니다.

---

## 5. LOT Domain – Contract Summary

### 5.1 Purpose

LOT 도메인은 반도체 제조 공정에서 LOT(웨이퍼 묶음)의 생성, 상태 변경, 스펙 변경 등을 관리합니다.  
PES에서는 LOT 이벤트를 중심으로 WF(Workflow), DURABLE(FOUP 등)과 연계된 일련의 동작을 처리하며, 모든 LOT 관련 이벤트는 Oracle 26ai 기반의 LOT_MAS / LOT_HIS 테이블에 기록됩니다.[web:9][web:33][web:12]

이 섹션은 LOT 도메인에 대해 Claude가 **깨면 안 되는 계약(Contract)** 만을 요약하고, DTO/Service/Enum 등의 구체 구현은 Claude에게 위임하기 위한 기준을 제공합니다.

### 5.2 LOT Message Contract (Summary)

- `entityType` 은 LOT 메시지에서 항상 `"LOT"` 이어야 한다.
- LOT 메시지에는 최소한 다음 필드가 포함되어야 한다.
  - `entityType` (고정 값 `"LOT"`)
  - `lotId` (PES_LOT_MAS / PES_LOT_HIS LOT_ID)
  - `workflow` (하나 이상의 step)
  - `meta.srcSystem` (예: `"UI"`, `"HUB"`)

LOT 전용 필드:

- `lotId`: LOT 식별자
- `wfId`: LOT와 연계된 WF 식별자 (필요 시)
- `durableId`: LOT와 연계된 DURABLE(FOUP) 식별자 (Released / makeInUse 등에서 사용)

각 `workflow` step은:

- `method`: LOT에 대해 수행할 작업 이름 (단일 의미)
- `options`: LOT/WF/DURABLE 간 연계 동작을 제어하는 옵션
- `event`: LOT_HIS에 기록할 이벤트 정보(`PesEventInfo`)

### 5.3 LOT Methods (Allowed Values)

LOT 도메인의 `workflow[i].method` 필드는 최소 다음 값을 가진다.

- `created`
- `released`
- `changeSpec`

각 method는 항상 **하나의 의미만** 가져야 하며,  
여러 동작의 조합(예: Created+Released)은 `workflow` 배열의 여러 step으로 표현하고 `createdAndReleased` 같은 복합 method 이름은 사용하지 않는다.

### 5.4 LOT Options (Behavior Flags)

예시 옵션:

- `createWf` (bool, `created`)
- `createDurable` (bool, `created`)
- `makeDurableInUse` (bool, `released`)
- `syncToHub` (bool, 공통)

옵션의 의미는 추후 WF/DURABLE Contract와 일관성 있게 유지해야 한다.

### 5.5 LOT Event & Meta

`event` 블록(`PesEventInfo`)은 LOT_HIS의 EVENT 관련 컬럼과 매핑된다.

- `eventCd`, `eventTm`, `eventDesc`, `statTyp` (기본)
- HIS PK는 `LOT_ID + TIMEKEY` 조합이며, `TIMEKEY`는 서버에서 생성

`meta` 블록(`PesMeta`)은 다음 필드를 포함한다.

- `srcSystem` (필수)
- `userId`, `correlationId`, `requestTm`, `locale` (선택)

---

## 6. WF Domain – Contract Summary

- `entityType = "WF"`
- 필수: `wfId`, `workflow`, `meta.srcSystem`
- LOT와 동일한 Workflow/Meta/Event 패턴 사용
- method 예:
  - `created` (WF 생성, LOT에서 파생 가능)
  - `released`
  - `changeSpec`
- 옵션 예:
  - `inheritLotSpec` (created)
  - `applyToLot` (changeSpec)
  - `syncToHub` (공통)

WF 메시지 DTO/Service 설계 시 LOT 패턴을 그대로 재사용할 것.

---

## 7. DURABLE Domain – Contract Summary

- `entityType = "DURABLE"`
- 필수: `durableId`, `workflow`, `meta.srcSystem`
- LOT/WF와 동일한 Workflow/Meta/Event 패턴 사용
- method 예:
  - `created`
  - `makeInUse`
  - `changeSpec`
- 옵션 예:
  - `bindLot`
  - `syncToEquipment`
  - `syncToHub`

DURABLE 메시지 DTO/Service 설계 시 LOT 패턴을 그대로 재사용할 것.

---

## 8. Common Value Objects (PesMeta, PesEventInfo)

### 8.1 PesMeta (공통 메타데이터)

- 클래스명 제안: `PesMeta`
- 패키지 제안: `com.playtogether.pes.common.model`
- 필드(최소):
  - `String srcSystem`
  - `String userId`
  - `String correlationId`
  - `String requestTm`
  - `String locale`
- LOT/WF/DURABLE 메시지 DTO에서 공통으로 사용

### 8.2 PesEventInfo (공통 이벤트 정보)

- 클래스명 제안: `PesEventInfo`
- 패키지 제안: `com.playtogether.pes.common.model`
- 필드(최소):
  - `String eventCd`
  - `String eventTm`
  - `String eventDesc`
  - `String statTyp`
- LOT_HIS / WF_HIS / DURABLE_HIS에 공통 매핑 가능한 구조로 설계

Claude는 이 두 VO를 공통 모듈에 정의하고, LOT/WF/DURABLE 메시지 DTO와 JPA 엔티티에서 재사용하는 방향으로 구현해야 한다.