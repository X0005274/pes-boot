@echo off
rem ==========================================================
rem PES production environment variables (edit per server)
rem ==========================================================

rem JDK 17 home
set "JAVA_HOME=C:\Program Files\Java\jdk-17"

rem PES install root
set "PES_HOME=C:\pes"

rem Spring profile (use "prod,rv" to enable TIBCO RV)
set "SPRING_PROFILES_ACTIVE=prod"

rem PES Oracle datasource
set "PES_DB_URL=jdbc:oracle:thin:@//db-host:1521/PESPDB"
set "PES_DB_USER=pes"
set "PES_DB_PASSWORD=changeme"

rem Schema management
set "PES_FLYWAY_ENABLED=true"
set "PES_JPA_DDL_AUTO=validate"

rem Actuator endpoints
set "PES_ACTUATOR_ENDPOINTS=health,info,metrics"

rem HubDB ingestion (optional)
set "PES_HUB_ENABLED=false"
set "PES_HUB_DB_URL=jdbc:oracle:thin:@//hub-host:1521/HUBPDB"
set "PES_HUB_DB_USER=hub"
set "PES_HUB_DB_PASSWORD=changeme"
set "PES_HUB_SCHED_ENABLED=false"

rem TIBCO RV (only when profile includes rv)
set "PES_RV_SERVICE=7500"
set "PES_RV_NETWORK=;"
set "PES_RV_DAEMON=tcp:7500"

rem JVM options
set "JAVA_OPTS=-Xms512m -Xmx1024m -Dfile.encoding=UTF-8"
