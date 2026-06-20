# PES 운영 배포 가이드

`pes-app` 모듈이 **단일 실행 가능 jar**(Spring Boot fat jar)로 빌드되며, 이 하나만 배포하면 됩니다.
설정은 모두 **환경변수 / 외부 config 파일**로 주입합니다(이미지/jar 재빌드 불필요).

## 1. 권장 디렉터리 구조 (운영 서버)

```
C:\pes\
├── app\
│   └── pes-app.jar                 # 빌드 산출물 (배포 대상)
├── config\
│   └── application-prod.yml         # 외부 설정(선택, 환경변수보다 우선순위 낮게 사용)
├── logs\                            # 로그 출력
└── bin\
    ├── env.bat                      # 환경변수 (DB 접속 등) — 서버별로 채움
    ├── start.bat                    # 콘솔 실행(개발/점검용)
    ├── stop.bat                     # 콘솔 실행 종료
    ├── pes-app.xml                  # WinSW 서비스 정의
    └── pes-app.exe                  # WinSW 실행파일(winsw.exe 를 복사/리네임)
```

> 빌드: `mvn -DskipTests package` → `pes-app/target/pes-app-0.0.1-SNAPSHOT.jar` 를
> `C:\pes\app\pes-app.jar` 로 복사.

## 2. 실행 방식

### (A) Windows 서비스 — 운영 권장 (WinSW)
[WinSW](https://github.com/winsw/winsw) `WinSW.NET4.exe` 를 받아 `bin\pes-app.exe` 로 복사 후:

```bat
cd C:\pes\bin
pes-app.exe install      :: 서비스 등록 (자동 시작)
pes-app.exe start        :: 시작
pes-app.exe stop         :: 중지
pes-app.exe restart      :: 재시작
pes-app.exe uninstall    :: 제거
```
- 부팅 시 자동 시작, 실패 시 자동 재시작, 로그 롤링을 WinSW 가 처리.
- DB 접속정보 등은 `pes-app.xml` 의 `<env>` 또는 `env.bat` 참조로 설정.

### (B) 콘솔 실행 — 개발/점검용
```bat
cd C:\pes\bin
start.bat       :: 포그라운드 실행 (Ctrl+C 종료)
stop.bat        :: 별도 창에서 종료
```

### (C) Linux — systemd (대안)
`deploy/systemd/pes-app.service` 참조. `/opt/pes` 기준, `EnvironmentFile=/opt/pes/bin/pes.env`.

## 3. 환경변수 (핵심)

| 변수 | 설명 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` (RV 사용 시 `prod,rv`) |
| `PES_DB_URL/USER/PASSWORD` | PES Oracle 접속 |
| `PES_JPA_DDL_AUTO` | 운영 `validate` (Flyway 가 스키마 관리) |
| `PES_FLYWAY_ENABLED` | `true` |
| `PES_HUB_ENABLED` / `PES_HUB_DB_*` | HubDB 적재 사용 시 |
| `PES_HUB_SCHED_ENABLED` | Hub 적재 스케줄러 |
| `PES_RV_SERVICE/NETWORK/DAEMON` | TIBCO RVD (profile rv) |

## 4. 기동 확인

- 헬스: `GET http://<host>:8080/actuator/health` → `{"status":"UP"}`
- API 문서: `http://<host>:8080/swagger-ui/index.html`
- 기동 시 Flyway 가 스키마 적용 → Hibernate `validate` 통과해야 정상.

## 5. TIBCO RV (운영 메시징) 사용 시

기본 jar 에는 상용 `tibrvj.jar` 가 포함되지 않습니다. RV 를 쓰려면:
```bat
set TIBRV_HOME=C:\tibco\tibrv
mvn -Prv-tibco -DskipTests package
```
- 실행 시 `SPRING_PROFILES_ACTIVE=prod,rv`
- TIBRV 네이티브 라이브러리 경로를 `-Djava.library.path=%TIBRV_HOME%\bin` 로 지정
- `tibrvj.jar` 를 클래스패스에 포함(또는 fat jar 에 포함되도록 의존 구성)
