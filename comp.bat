@echo off
chcp 1251 > nul
title Сборка проекта в JAR файлы
echo ============================================
echo        СОБОРКА ПРОЕКТА В JAR ФАЙЛЫ
echo ============================================
echo.

echo 1. Создаем структуру папок...
mkdir build 2>nul
mkdir build\classes 2>nul
mkdir build\jar 2>nul
mkdir build\manifest 2>nul

echo 2. Создаем файлы манифеста...
echo Manifest-Version: 1.0> build\manifest\manifest-client.txt
echo Main-Class: view.GameClient>> build\manifest\manifest-client.txt
echo Class-Path: .>> build\manifest\manifest-client.txt
echo Created-By: TicTacToe Game>> build\manifest\manifest-client.txt

echo Manifest-Version: 1.0> build\manifest\manifest-server.txt
echo Main-Class: Main>> build\manifest\manifest-server.txt
echo Class-Path: .>> build\manifest\manifest-server.txt
echo Created-By: TicTacToe Game>> build\manifest\manifest-server.txt

echo 3. Компилируем все исходные файлы...
javac -d build\classes -sourcepath src ^
    src\model\GameBoard.java ^
    src\model\GameRoom.java ^
    src\controller\GameServer.java ^
    src\controller\ClientHandler.java ^
    src\controller\ClientController.java ^
    src\view\GameClient.java ^
    src\view\GamePanel.java ^
    src\view\LobbyPanel.java ^
    src\Main.java

if %errorlevel% neq 0 (
    echo Ошибка компиляции!
    pause
    exit /b
)

echo 4. Создаем JAR файл для клиента...
cd build\classes
jar cfm ..\jar\tic-tac-toe-client.jar ..\manifest\manifest-client.txt ^
    model\*.class ^
    controller\*.class ^
    view\*.class ^
    Main.class
cd ..\..

echo 5. Создаем JAR файл для сервера...
cd build\classes
jar cfm ..\jar\tic-tac-toe-server.jar ..\manifest\manifest-server.txt ^
    model\*.class ^
    controller\*.class ^
    Main.class
cd ..\..

echo 6. Создаем универсальный JAR файл (все вместе)...
cd build\classes
jar cfm ..\jar\tic-tac-toe-full.jar ..\manifest\manifest-server.txt *
cd ..\..

echo.
echo ============================================
echo        СБОРКА ЗАВЕРШЕНА УСПЕШНО!
echo ============================================
echo.
echo Созданы JAR файлы:
echo   - build\jar\tic-tac-toe-client.jar   (клиент)
echo   - build\jar\tic-tac-toe-server.jar   (сервер)
echo   - build\jar\tic-tac-toe-full.jar     (полный)
echo.
echo Команды запуска:
echo   Сервер: java -jar build\jar\tic-tac-toe-server.jar server 5555
echo   Клиент: java -jar build\jar\tic-tac-toe-client.jar
echo.
pause