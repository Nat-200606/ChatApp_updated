@echo off
cd /d "%~dp0"

echo Compilando o cliente...
javac --enable-preview --release 21 -d bin -encoding UTF-8 Client.java MyFrame.java

if errorlevel 1 (
    echo.
    echo Erro ao compilar. Verifique o codigo.
    pause
    exit /b 1
)

echo Iniciando o cliente...
java --enable-preview -cp bin Client

pause
