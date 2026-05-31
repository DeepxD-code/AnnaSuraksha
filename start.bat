@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

set APP_PORT=8081
set APP_VERSION=5.0.0
set APP_JAR=target\annasuraksha-%APP_VERSION%.jar
set TOOLS_DIR=%CD%\.tools
set JAVA_DIR=%TOOLS_DIR%\jdk-21
set JAVA_EXE=java
set MAVEN_DIR=%TOOLS_DIR%\maven
set MVN_CMD=mvn

:: ── Detect ANSI support ──────────────────────────────────────────────
set "RED=" & set "GREEN=" & set "YELLOW=" & set "CYAN=" & set "BOLD=" & set "RESET="
for /f %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"
if not "%ESC%"=="" (
    set "RED=%ESC%[31m"
    set "GREEN=%ESC%[32m"
    set "YELLOW=%ESC%[33m"
    set "CYAN=%ESC%[36m"
    set "BOLD=%ESC%[1m"
    set "RESET=%ESC%[0m"
)

echo.
echo %BOLD%%CYAN%  ╔══════════════════════════════════════════════════╗
echo  ║     AnnaSuraksha v%APP_VERSION% — PDS Fraud Platform     ║
echo  ╚══════════════════════════════════════════════════════╝%RESET%

echo %BOLD%%CYAN%  :: [1/5] Checking prerequisites...%RESET%

:: ─── Java check ─────────────────────────────────────────────────────
set JAVA_OK=0
java -version 2>&1 | findstr /b /c:"openjdk version" | findstr "21" >nul && set JAVA_OK=1
if %JAVA_OK%==0 java -version 2>&1 | findstr /b /c:"java version" | findstr "21" >nul && set JAVA_OK=1

if %JAVA_OK%==1 (
    echo  %GREEN%[OK]%RESET% Java 21 found in PATH
) else (
    echo  %YELLOW%[!]%RESET% Java 21 not found. Checking local JDK...
    if exist "%JAVA_DIR%\bin\java.exe" (
        set "JAVA_EXE=%JAVA_DIR%\bin\java"
        set "PATH=%JAVA_DIR%\bin;%PATH%"
        echo  %GREEN%[OK]%RESET% Using local JDK at %JAVA_DIR%
    ) else (
        echo  %YELLOW%[^>]%RESET% Downloading Eclipse Temurin JDK 21...
        if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"
        curl -#L -o "%TOOLS_DIR%\jdk21.zip" "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse"
        if !ERRORLEVEL! NEQ 0 (
            echo  %RED%[X]%RESET% Download failed. Install Java 21 from https://adoptium.net
            pause & exit /b 1
        )
        echo  %YELLOW%[^>]%RESET% Extracting...
        powershell -Command "Expand-Archive -Path '%TOOLS_DIR%\jdk21.zip' -DestinationPath '%TOOLS_DIR%' -Force" >nul
        if !ERRORLEVEL! NEQ 0 (
            echo  %RED%[X]%RESET% Extraction failed. Corrupted download.
            pause & exit /b 1
        )
        for /d %%i in ("%TOOLS_DIR%\jdk-*") do if not "%%i"=="%JAVA_DIR%" move "%%i" "%JAVA_DIR%" >nul 2>&1
        if not exist "%JAVA_DIR%\bin\java.exe" (
            for /d %%i in ("%TOOLS_DIR%\*") do if exist "%%i\bin\java.exe" move "%%i" "%JAVA_DIR%" >nul 2>&1
        )
        del "%TOOLS_DIR%\jdk21.zip" 2>nul
        set "JAVA_EXE=%JAVA_DIR%\bin\java"
        set "PATH=%JAVA_DIR%\bin;%PATH%"
        echo  %GREEN%[OK]%RESET% JDK 21 installed locally
    )
)

java -version 2>&1 | findstr "version" >nul
if %ERRORLEVEL% NEQ 0 (
    echo  %RED%[X]%RESET% Java unavailable. Install Java 21 manually.
    pause & exit /b 1
)

:: ─── Maven check ──────────────────────────────────────────────────
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo  %YELLOW%[!]%RESET% Maven not found. Downloading...
    if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"
    curl -#L -o "%TOOLS_DIR%\maven.zip" "https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"
    if !ERRORLEVEL! NEQ 0 (
        echo  %RED%[X]%RESET% Maven download failed. Install from https://maven.apache.org
        pause & exit /b 1
    )
    echo  %YELLOW%[^>]%RESET% Extracting...
    powershell -Command "Expand-Archive -Path '%TOOLS_DIR%\maven.zip' -DestinationPath '%TOOLS_DIR%' -Force" >nul
    if !ERRORLEVEL! NEQ 0 (
        echo  %RED%[X]%RESET% Extraction failed.
        pause & exit /b 1
    )
    for /d %%i in ("%TOOLS_DIR%\apache-maven-*") do if not "%%i"=="%MAVEN_DIR%" move "%%i" "%MAVEN_DIR%" >nul 2>&1
    del "%TOOLS_DIR%\maven.zip" 2>nul
    set "PATH=%MAVEN_DIR%\bin;%PATH%"
    set "MVN_CMD=%MAVEN_DIR%\bin\mvn"
    echo  %GREEN%[OK]%RESET% Maven 3.9.9 installed locally
) else (
    set "MVN_CMD=mvn"
    echo  %GREEN%[OK]%RESET% Maven found in PATH
)

:: ─── Port check ───────────────────────────────────────────────────
echo %BOLD%%CYAN%  :: [2/5] Checking port %APP_PORT%...%RESET%
netstat -ano | findstr ":%APP_PORT% " >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo  %RED%[X]%RESET% Port %APP_PORT% is in use.
    pause & exit /b 1
)
echo  %GREEN%[OK]%RESET% Port %APP_PORT% is free

:: ─── Print versions ────────────────────────────────────────────────
echo %BOLD%%CYAN%  :: [3/5] Tool versions:%RESET%
for /f "tokens=1-3" %%v in ('java -version 2^>^&1 ^| findstr /b "java openjdk"') do echo    Java: %%v %%w %%x
call %MVN_CMD% --version 2>&1 | findstr "Apache Maven"

:: ─── Build ──────────────────────────────────────────────────────────
echo %BOLD%%CYAN%  :: [4/5] Building application...%RESET%
call %MVN_CMD% -DskipTests clean package > build.log 2>&1
if %ERRORLEVEL% NEQ 0 (
    type build.log
    echo  %RED%[X]%RESET% Build failed. See build.log for details.
    pause & exit /b 1
)
echo  %GREEN%[OK]%RESET% Build successful

:: ─── Set env & launch ───────────────────────────────────────────────
echo %BOLD%%CYAN%  :: [5/5] Launching AnnaSuraksha...%RESET%

set SPRING_H2_CONSOLE_ENABLED=true
set ANNASURAKSHA_SEED_DEMO=true
set JWT_SECRET=ANNASURAKSHA_V5_JWT_SECRET_KEY_32CHARS_MIN
if not defined DEMO_ADMIN_PW set DEMO_ADMIN_PW=Admin@123
if not defined DEMO_OFFICER_PW set DEMO_OFFICER_PW=Officer@123
if not defined DEMO_AUDIT_PW set DEMO_AUDIT_PW=Audit@123
if not defined DEMO_FPS_PW set DEMO_FPS_PW=Fps@123

echo.
echo  %CYAN%┌─────────────────────────────────────────────────────────┐%RESET%
echo  %CYAN%│  Starting server on http://localhost:%APP_PORT%                │%RESET%
echo  %CYAN%│  Swagger UI: http://localhost:%APP_PORT%/swagger-ui.html          │%RESET%
echo  %CYAN%└─────────────────────────────────────────────────────────┘%RESET%
echo.

start /b "" "%JAVA_EXE%" -jar "%APP_JAR%" --server.port=%APP_PORT% --spring.h2.console.enabled=true --annasuraksha.seed.demo-data=true

:: ─── Wait for health ────────────────────────────────────────────────
set WAIT_S=0
echo  %CYAN%[*]%RESET% Waiting for app to become healthy...
echo  %CYAN%[*]%RESET% (this may take 15-30s for first startup)
:CHECK_HEALTH
timeout /t 2 /nobreak >nul
set /a WAIT_S+=2
>nul 2>&1 curl.exe -sf "http://localhost:%APP_PORT%/actuator/health" && goto HEALTHY
>nul 2>&1 powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:%APP_PORT%/actuator/health' -UseBasicParsing -TimeoutSec 2; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" && goto HEALTHY
if %WAIT_S% GEQ 60 (
    echo  %RED%[X]%RESET% App did not start within 60 seconds.
    echo.
    echo  %YELLOW%[^>]%RESET% Run manually: "%JAVA_EXE%" -jar "%APP_JAR%" --server.port=%APP_PORT% --spring.h2.console.enabled=true --annasuraksha.seed.demo-data=true
    pause & exit /b 1
)
goto CHECK_HEALTH

:HEALTHY
echo  %GREEN%[OK]%RESET% Server is healthy and ready!

echo.
echo  %BOLD%%GREEN%╔══════════════════════════════════════════════════╗%RESET%
echo  %BOLD%%GREEN%║        AnnaSuraksha is running!              ║%RESET%
echo  %BOLD%%GREEN%╠══════════════════════════════════════════════════╣%RESET%
echo  %BOLD%%GREEN%║  URL:  http://localhost:%APP_PORT%               ║%RESET%
echo  %BOLD%%GREEN%║  Docs: http://localhost:%APP_PORT%/swagger-ui.html ║%RESET%
echo  %BOLD%%GREEN%╠══════════════════════════════════════════════════╣%RESET%
echo  %BOLD%%GREEN%║  Demo Accounts                                 ║%RESET%
echo  %BOLD%%GREEN%║  admin@annasuraksha.gov.in  (ADMIN)            ║%RESET%
echo  %BOLD%%GREEN%║  officer@up.gov.in          (OFFICER)          ║%RESET%
echo  %BOLD%%GREEN%║  auditor@cag.gov.in         (AUDITOR)          ║%RESET%
echo  %BOLD%%GREEN%║  fps@mh.gov.in              (FPS_OPERATOR)     ║%RESET%
echo  %BOLD%%GREEN%╚══════════════════════════════════════════════════╝%RESET%
echo.

timeout /t 1 /nobreak >nul
start http://localhost:%APP_PORT%

echo  %CYAN%Quick demo steps:%RESET%
echo    curl -X POST http://localhost:%APP_PORT%/api/simulate/fraud-case/all
echo    curl http://localhost:%APP_PORT%/api/simulate/accuracy
echo    curl http://localhost:%APP_PORT%/api/fraud/summary
echo.
echo  %YELLOW%The server is running in the background. Press Ctrl+C to stop.%RESET%
echo.
pause
goto :EOF
