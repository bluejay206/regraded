@echo off
setlocal enabledelayedexpansion

echo ========================================
echo    ReGraded - Prepare for build and runtime
echo ========================================

:: Parse arguments
set "NORUN=0"
set "CLEAR=0"
set "TARGET=mc1211"

for %%a in (%*) do (
    if /i "%%a" == "-norun" set NORUN=1
    if /i "%%a" == "-clear" set CLEAR=1
    if /i "%%a" == "-1211" set TARGET=mc1211
    if /i "%%a" == "-262" set TARGET=mc262
    if /i "%%a" == "-263" set TARGET=mc263
    if /i "%%a" == "-all" set TARGET=all
)

echo [Cleanup] Archiving old build logs and source zips...
if not exist old_logs mkdir old_logs
if not exist old_builds mkdir old_builds

for %%f in (build_*.log run_output.log) do (
    if exist "%%f" move "%%f" old_logs\ >nul
)

for %%f in (src*.zip) do (
    if exist "%%f" move "%%f" old_builds\ >nul
)

set BUILD_NUM_FILE=build_number.txt
if not exist %BUILD_NUM_FILE% echo 000 > %BUILD_NUM_FILE%
set /p BUILD_NUM=<%BUILD_NUM_FILE%
set /a BUILD_NUM+=1
set BUILD_NUM=000!BUILD_NUM!
set BUILD_NUM=!BUILD_NUM:~-3!

for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "TIMESTAMP=%dt:~0,4%-%dt:~4,2%-%dt:~6,2% %dt:~8,2%:%dt:~10,2%:%dt:~12,2%"

set GIT_HASH=unknown
if exist .git (
    for /f %%a in ('git rev-parse --short HEAD 2^>nul') do set GIT_HASH=%%a
)

set "ZIPFILE=src_%dt:~2,6%_%dt:~8,4%_b%BUILD_NUM%.zip"

echo Timestamp: %TIMESTAMP%
echo Build number: %BUILD_NUM%
echo Target: %TARGET%
echo Target ZIP: %ZIPFILE%

if %CLEAR%==1 (
    echo Clearing .gradle and build folders...
    taskkill /f /im java.exe 2>nul
    if exist .gradle rd /s /q .gradle 2>nul
    if exist build rd /s /q build 2>nul
    if exist mc1211\build rd /s /q mc1211\build 2>nul
    if exist mc262\build rd /s /q mc262\build 2>nul
    if exist mc263\build rd /s /q mc263\build 2>nul
    if exist core\build rd /s /q core\build 2>nul
)

echo Running clean...
call gradlew clean --quiet

set OVERALL=pass
set RESULT_MC1211=skip
set RESULT_MC262=skip
set RESULT_MC263=skip

if "%TARGET%"=="all" (
    echo.
    echo ========== Building :mc1211 ==========
    set "LOG_MC1211=build_mc1211_%dt:~2,6%_%dt:~8,4%_b%BUILD_NUM%.log"
    call gradlew :mc1211:build > "!LOG_MC1211!" 2>&1
    if !errorlevel! equ 0 (
        set RESULT_MC1211=pass
        echo [mc1211 SUCCESS]
    ) else (
        set RESULT_MC1211=fail
        set OVERALL=fail
        echo [mc1211 FAILED] log: !LOG_MC1211!
    )

    echo.
    echo ========== Building :mc262 ==========
    set "LOG_MC262=build_mc262_%dt:~2,6%_%dt:~8,4%_b%BUILD_NUM%.log"
    call gradlew :mc262:build > "!LOG_MC262!" 2>&1
    if !errorlevel! equ 0 (
        set RESULT_MC262=pass
        echo [mc262 SUCCESS]
    ) else (
        set RESULT_MC262=fail
        set OVERALL=fail
        echo [mc262 FAILED] log: !LOG_MC262!
    )

    echo.
    echo ========== Building :mc263 ==========
    set "LOG_MC263=build_mc263_%dt:~2,6%_%dt:~8,4%_b%BUILD_NUM%.log"
    call gradlew :mc263:build > "!LOG_MC263!" 2>&1
    if !errorlevel! equ 0 (
        set RESULT_MC263=pass
        echo [mc263 SUCCESS]
    ) else (
        set RESULT_MC263=fail
        set OVERALL=fail
        echo [mc263 FAILED] log: !LOG_MC263!
    )
) else (
    echo.
    echo ========== Building :%TARGET% ==========
    set "LOG_SINGLE=build_%TARGET%_%dt:~2,6%_%dt:~8,4%_b%BUILD_NUM%.log"
    call gradlew :%TARGET%:build > "!LOG_SINGLE!" 2>&1
    if !errorlevel! equ 0 (
        set RESULT_%TARGET%=pass
        echo [%TARGET% SUCCESS]
        if %NORUN%==0 (
            echo.
            echo Launching :%TARGET%:runClient...
            call gradlew :%TARGET%:runClient > run_output.log 2>&1
        )
    ) else (
        set RESULT_%TARGET%=fail
        set OVERALL=fail
        echo [%TARGET% FAILED] log: !LOG_SINGLE!
    )
)

set CSVFILE=Build_Status.csv
if not exist %CSVFILE% (
    echo timestamp,build_number,overall,mc1211,mc262,mc263,zip_filename,git_short_hash > %CSVFILE%
)
echo %TIMESTAMP%,%BUILD_NUM%,%OVERALL%,%RESULT_MC1211%,%RESULT_MC262%,%RESULT_MC263%,%ZIPFILE%,%GIT_HASH% >> %CSVFILE%

echo.
echo Creating zip: %ZIPFILE%
set "STAGE=%TEMP%\rg_stage_%BUILD_NUM%"
if exist "%STAGE%" rd /s /q "%STAGE%"
mkdir "%STAGE%"

for %%f in (build.gradle gradle.properties settings.gradle prepareBuild.bat README.md LICENSE) do (
    if exist "%%f" copy /y "%%f" "%STAGE%\" >nul
)

for %%d in (core mc1211 mc262 mc263) do (
    if exist "%%d" (
        robocopy "%%d" "%STAGE%\%%d" /E /XD build .gradle run /NFL /NDL /NJH /NJS /NC /NS >nul
    )
)

for %%t in (mc1211 mc262 mc263) do (
    if exist "%%t\build\libs" (
        mkdir "%STAGE%\%%t\libs" 2>nul
        copy /y "%%t\build\libs\regraded-*.jar" "%STAGE%\%%t\libs\" >nul 2>&1
    )
)

for %%t in (mc1211 mc262 mc263) do (
    if exist "build_%%t_*.log" (
        mkdir "%STAGE%\%%t\logs" 2>nul
        copy /y "build_%%t_*.log" "%STAGE%\%%t\logs\" >nul 2>&1
    )
)
if exist run_output.log (
    mkdir "%STAGE%\logs" 2>nul
    copy /y run_output.log "%STAGE%\logs\" >nul
)

if exist "%ZIPFILE%" del /f /q "%ZIPFILE%"
powershell -NoProfile -Command "Compress-Archive -Path '%STAGE%\*' -DestinationPath '%ZIPFILE%' -Force"
rd /s /q "%STAGE%"

echo %BUILD_NUM% > %BUILD_NUM_FILE%

if exist "%ZIPFILE%" (
    echo Success: %ZIPFILE%
) else (
    echo ERROR: zip was not created.
)

if exist run_output.log del run_output.log
taskkill /f /im java.exe 2>nul

echo.
echo Build #%BUILD_NUM% overall=%OVERALL%  mc1211=%RESULT_MC1211%  mc262=%RESULT_MC262%  mc263=%RESULT_MC263%
echo Previous logs archived to old_logs\
