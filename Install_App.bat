@echo off
setlocal enabledelayedexpansion
title Secure Password Manager - Mobile Wi-Fi & USB APK Installer
color 0A

set ADB_PATH=D:\sdk\platform-tools\adb.exe
set APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk
set RELEASE_APK_PATH=%~dp0app\build\outputs\apk\release\app-release.apk
set IP_CACHE_FILE=%TEMP%\mypassword_saved_ip.txt
set IP_LAST_FILE=%TEMP%\mypassword_last_digit.txt
set PACKAGE_NAME=jmd.shiv.mypassword
set LAUNCH_ACTIVITY=jmd.shiv.mypassword/.activities.SplashActivity
set DEFAULT_BASE_IP=192.168.1.

if not exist "%ADB_PATH%" (
    echo [ERROR] ADB not found at: %ADB_PATH%
    pause
    exit /b
)

set PORT_CACHE_FILE=%TEMP%\mypassword_saved_port.txt

if exist "%IP_LAST_FILE%" set /p SAVED_LAST_DIGIT=<"%IP_LAST_FILE%"
if "!SAVED_LAST_DIGIT!"=="" set SAVED_LAST_DIGIT=4
if exist "%PORT_CACHE_FILE%" set /p SAVED_PORT=<"%PORT_CACHE_FILE%"
if "!SAVED_PORT!"=="" set SAVED_PORT=5555

set CURRENT_DEVICE_IP=%DEFAULT_BASE_IP%!SAVED_LAST_DIGIT!
set SAVED_DEVICE_IP=!CURRENT_DEVICE_IP!:!SAVED_PORT!

:MENU
cls
echo ================================================================
echo             SECURE PASSWORD MANAGER - 1-CLICK APK INSTALLER     
echo             Package: %PACKAGE_NAME%
echo ================================================================
echo [*] Mobile IP (Fixed) : %DEFAULT_BASE_IP%!SAVED_LAST_DIGIT!
echo [*] Current Port      : !SAVED_PORT!
echo [*] Target Address    : %DEFAULT_BASE_IP%!SAVED_LAST_DIGIT!:!SAVED_PORT!
echo ================================================================
echo.
echo   [1] Fast Install via Wi-Fi (Confirm / Update Port ^& Install)
echo   [2] Change Port Only (Jab bhi Wireless Debugging on/off ho)
echo   [3] Change IP Last Digit (Current: !SAVED_LAST_DIGIT! -^> 192.168.1.!SAVED_LAST_DIGIT!)
echo   [4] First-Time Wi-Fi Pairing (Pairing Code + Port)
echo   [5] Fix Port to 5555 via USB (One-Time Setup: Port kabhi nahi badlega!)
echo   [6] Install via USB Cable
echo   [7] Rebuild Debug APK
echo   [8] Build Signed Release APK
echo   [9] Exit
echo.
echo ================================================================
echo.
set /p CHOICE="Select an option (1-9): "

if "%CHOICE%"=="1" goto WIFI_CONNECT
if "%CHOICE%"=="2" goto CHANGE_PORT
if "%CHOICE%"=="3" goto CHANGE_IP
if "%CHOICE%"=="4" goto WIFI_PAIR
if "%CHOICE%"=="5" goto USB_FIX_PORT
if "%CHOICE%"=="6" goto USB_INSTALL
if "%CHOICE%"=="7" goto REBUILD_DEBUG
if "%CHOICE%"=="8" goto REBUILD_RELEASE
if "%CHOICE%"=="9" exit /b
goto MENU

:CHANGE_PORT
cls
echo ================================================================
echo          UPDATE WIRELESS DEBUGGING PORT ONLY                    
echo ================================================================
echo Target Mobile: %DEFAULT_BASE_IP%!SAVED_LAST_DIGIT!
echo Current Port: !SAVED_PORT!
set /p NEW_PORT="Enter New 5-digit Port: "
if not "!NEW_PORT!"=="" (
    set SAVED_PORT=!NEW_PORT!
    echo !SAVED_PORT!>"%PORT_CACHE_FILE%"
    set SAVED_DEVICE_IP=%DEFAULT_BASE_IP%!SAVED_LAST_DIGIT!:!SAVED_PORT!
    echo !SAVED_DEVICE_IP!>"%IP_CACHE_FILE%"
)
goto WIFI_CONNECT

:CHANGE_IP
cls
echo ================================================================
echo          ENTER MOBILE IP LAST DIGIT ^& PORT                      
echo ================================================================
echo Base IP: %DEFAULT_BASE_IP%xxx
echo Enter Last Digit:Port (e.g. 4:42351) ya sirf Last Digit (e.g. 4):
set /p USER_INPUT="Input [Default: !SAVED_LAST_DIGIT!]: "
if "!USER_INPUT!"=="" set USER_INPUT=!SAVED_LAST_DIGIT!

for /f "tokens=1,2 delims=:" %%a in ("!USER_INPUT!") do (
    set PART1=%%a
    set PART2=%%b
)

if not "!PART2!"=="" (
    set TARGET_PORT=!PART2!
    echo !PART1! | findstr /C:"." >nul
    if !ERRORLEVEL! equ 0 (
        set TARGET_IP=!PART1!
    ) else (
        set TARGET_IP=%DEFAULT_BASE_IP%!PART1!
        set SAVED_LAST_DIGIT=!PART1!
    )
) else (
    echo !PART1! | findstr /C:"." >nul
    if !ERRORLEVEL! equ 0 (
        set TARGET_IP=!PART1!
    ) else (
        set TARGET_IP=%DEFAULT_BASE_IP%!PART1!
        set SAVED_LAST_DIGIT=!PART1!
    )
    set /p TARGET_PORT="Enter Wireless Debugging Port [Default: !SAVED_PORT!]: "
    if "!TARGET_PORT!"=="" set TARGET_PORT=!SAVED_PORT!
)

set SAVED_PORT=!TARGET_PORT!
set SAVED_DEVICE_IP=!TARGET_IP!:!TARGET_PORT!
echo !SAVED_DEVICE_IP!>"%IP_CACHE_FILE%"
echo !SAVED_LAST_DIGIT!>"%IP_LAST_FILE%"
echo !SAVED_PORT!>"%PORT_CACHE_FILE%"
goto WIFI_CONNECT

:WIFI_CONNECT
cls
if not defined SAVED_DEVICE_IP goto CHANGE_IP
if not exist "%APK_PATH%" (
    echo [INFO] APK not found. Building Debug APK first...
    call "%~dp0gradlew.bat" assembleDebug
)
echo [*] Connecting to Target Mobile: !SAVED_DEVICE_IP!
"%ADB_PATH%" connect !SAVED_DEVICE_IP!
echo [*] Installing APK to Device...
"%ADB_PATH%" -s !SAVED_DEVICE_IP! install -r "%APK_PATH%"
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Secure Password Manager Successfully Installed!
    "%ADB_PATH%" -s !SAVED_DEVICE_IP! shell am start -n %LAUNCH_ACTIVITY%
) else (
    echo [ERROR] Installation failed. Check device connection.
)
pause
goto MENU

:WIFI_PAIR
cls
echo ================================================================
echo             FIRST-TIME WIRELESS DEBUGGING PAIRING               
echo ================================================================
set /p PAIR_INPUT="Pairing Last Digit:Port (e.g. 4:38291) [Default: !SAVED_LAST_DIGIT!]: "
if "!PAIR_INPUT!"=="" set PAIR_INPUT=!SAVED_LAST_DIGIT!
for /f "tokens=1,2 delims=:" %%a in ("!PAIR_INPUT!") do (
    set P_PART1=%%a
    set P_PART2=%%b
)
if not "!P_PART2!"=="" (
    set PAIR_PORT=!P_PART2!
    set PAIR_IP=%DEFAULT_BASE_IP%!P_PART1!
    set SAVED_LAST_DIGIT=!P_PART1!
) else (
    set PAIR_IP=%DEFAULT_BASE_IP%!P_PART1!
    set SAVED_LAST_DIGIT=!P_PART1!
    set /p PAIR_PORT="Enter 5-digit Pairing Port: "
)
set /p PAIR_CODE="Enter 6-digit Pairing Code: "
"%ADB_PATH%" pair !PAIR_IP!:!PAIR_PORT! !PAIR_CODE!

set /p MAIN_PORT="Enter Main Wireless Debugging Port: "
set SAVED_PORT=!MAIN_PORT!
set SAVED_DEVICE_IP=!PAIR_IP!:!MAIN_PORT!
echo !SAVED_DEVICE_IP!>"%IP_CACHE_FILE%"
echo !SAVED_LAST_DIGIT!>"%IP_LAST_FILE%"
echo !SAVED_PORT!>"%PORT_CACHE_FILE%"
"%ADB_PATH%" connect !SAVED_DEVICE_IP!
"%ADB_PATH%" -s !SAVED_DEVICE_IP! install -r "%APK_PATH%"
if !ERRORLEVEL! equ 0 (
    "%ADB_PATH%" -s !SAVED_DEVICE_IP! shell am start -n %LAUNCH_ACTIVITY%
)
pause
goto MENU

:USB_FIX_PORT
cls
echo [*] Connecting USB device and fixing wireless port to 5555...
"%ADB_PATH%" tcpip 5555
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Wireless Port permanently fixed to 5555!
    set SAVED_PORT=5555
    echo 5555>"%PORT_CACHE_FILE%"
    set SAVED_DEVICE_IP=%DEFAULT_BASE_IP%!SAVED_LAST_DIGIT!:5555
    echo !SAVED_DEVICE_IP!>"%IP_CACHE_FILE%"
)
pause
goto MENU

:USB_INSTALL
cls
echo [*] Installing via USB...
if not exist "%APK_PATH%" (
    echo [INFO] Building Debug APK first...
    call "%~dp0gradlew.bat" assembleDebug
)
"%ADB_PATH%" devices
"%ADB_PATH%" install -r "%APK_PATH%"
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Successfully Installed via USB!
    "%ADB_PATH%" shell am start -n %LAUNCH_ACTIVITY%
)
pause
goto MENU

:REBUILD_DEBUG
cls
echo [*] Compiling and Building Debug APK...
call "%~dp0gradlew.bat" assembleDebug
pause
goto MENU

:REBUILD_RELEASE
cls
echo [*] Compiling and Building Signed Release APK...
call "%~dp0gradlew.bat" assembleRelease
pause
goto MENU
