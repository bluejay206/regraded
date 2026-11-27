@echo off
setlocal enabledelayedexpansion

echo ========================================
echo    ReGraded - Prepare for build and runtime
echo ========================================

:: Parse arguments
set "NORUN=0"
set "CLEAR=0"
set "TARGET=mc1211"
set "BUILD_ALL=0"

for %%a in (%*) do (
    if /i "%%a" == "-norun" set NORUN=1
    if /i "%%a" == "-clear" set CLEAR=1
    if /i "%%a" == "-1211" set TARGET=mc1211
    if /i "%%a" == "-2612" set TARGET=mc2612
    if /i "%%a" == "-all" (
        set BUILD_ALL=1
        set NORUN=1
    )
)

echo [Cleanup] Archiving old build logs and source zips...
if not exist old_logs mkdir old_logs
if not exist old_builds mkdir old_builds

for %%f in (build_*.log run_output.log) do (
    if exist "%%f" move "%%f" old_logs\ >nul
)

for %%f in (src*.zip RG_src*.zip) do (
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

set "ZIPFILE=RG_src_%dt:~2,6%_%dt:~8,4%_b%BUILD_NUM%.zip"

echo Timestamp: %TIMESTAMP%
echo Build number: %BUILD_NUM%
echo Target: %TARGET%
if %BUILD_ALL%==1 echo Mode: -all (mc1211 + mc2612, no launch)
echo Target ZIP: %ZIPFILE%

if %CLEAR%==1 (
    echo Clearing .gradle and build folders...
    taskkill /f /im java.exe 2>nul
    if exist .gradle rd /s /q .gradle 2>nul
    if exist build rd /s /q build 2>nul
    if exist mc1211\build rd /s /q mc1211\build 2>nul
    if exist mc2612\build rd /s /q mc2612\build 2>nul
    if exist core\build rd /s /q core\build 2>nul
)

echo Running clean...
call gradlew clean --quiet

set OVERALL=pass
set RESULT_MC1211=skip
set RESULT_MC2612=skip

if %BUILD_ALL%==1 (
    call :build_target mc1211 RESULT_MC1211
    call :build_target mc2612 RESULT_MC2612
) else (
    if /i "%TARGET%"=="mc2612" (
        call :build_target mc2612 RESULT_MC2612
    ) else (
        call :build_target mc1211 RESULT_MC1211
        if %NORUN%==0 (
            if "!RESULT_MC1211!"=="pass" (
                echo.
                echo Launching :mc1211:runClient...
                call gradlew :mc1211:runClient > run_output.log 2>&1
            )
        )
    )
)

set CSVFILE=Build_Status.csv
if not exist %CSVFILE% (
    echo timestamp,build_number,overall,mc1211,mc2612,zip_filename,git_short_hash > %CSVFILE%
)
echo %TIMESTAMP%,%BUILD_NUM%,%OVERALL%,%RESULT_MC1211%,%RESULT_MC2612%,%ZIPFILE%,%GIT_HASH% >> %CSVFILE%

echo.
echo Creating zip: %ZIPFILE%
set "STAGE=%TEMP%\rg_stage_%BUILD_NUM%"
if exist "%STAGE%" rd /s /q "%STAGE%"
mkdir "%STAGE%"

for %%f in (build.gradle gradle.properties settings.gradle prepareBuild.bat README.md LICENSE) do (
    if exist "%%f" copy /y "%%f" "%STAGE%\" >nul
)

for %%d in (core mc1211 mc2612) do (
    if exist "%%d" (
        robocopy "%%d" "%STAGE%\%%d" /E /XD build .gradle run /NFL /NDL /NJH /NJS /NC /NS >nul
    )
)

if exist "mc1211\build\libs" (
    mkdir "%STAGE%\mc1211\libs" 2>nul
    copy /y "mc1211\build\libs\regraded-*.jar" "%STAGE%\mc1211\libs\" >nul 2>&1
)
if exist "mc2612\build\libs" (
    mkdir "%STAGE%\mc2612\libs" 2>nul
    copy /y "mc2612\build\libs\regraded-*.jar" "%STAGE%\mc2612\libs\" >nul 2>&1
)

if exist "build_mc1211_*.log" (
    mkdir "%STAGE%\mc1211\logs" 2>nul
    copy /y "build_mc1211_*.log" "%STAGE%\mc1211\logs\" >nul 2>&1
)
if exist "build_mc2612_*.log" (
    mkdir "%STAGE%\mc2612\logs" 2>nul
    copy /y "build_mc2612_*.log" "%STAGE%\mc2612\logs\" >nul 2>&1
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
echo Build #%BUILD_NUM% overall=%OVERALL%  mc1211=%RESULT_MC1211%  mc2612=%RESULT_MC2612%
echo Previous logs archived to old_logs\
echo Previous source zips archived to old_builds\
goto :eof

:build_target
set "TGT=%~1"
set "RESVAR=%~2"
echo.
echo ========== Building :%TGT% ==========
set "LOG=build_%TGT%_%dt:~2,6%_%dt:~8,4%_b%BUILD_NUM%.log"
call gradlew :%TGT%:build > "!LOG!" 2>&1
if !errorlevel! equ 0 (
    set %RESVAR%=pass
    echo [%TGT% SUCCESS]
) else (
    set %RESVAR%=fail
    set OVERALL=fail
    echo [%TGT% FAILED] log: !LOG!
)
goto :eof
