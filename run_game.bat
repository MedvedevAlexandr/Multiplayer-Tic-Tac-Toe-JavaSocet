

javac GameServer.java
javac GameClient.java

start "Сервер игры" cmd /c "java GameServer && pause"

start "Клиент 1" cmd /c "java GameClient && pause"
start "Клиент 2" cmd /c "java GameClient && pause"
@REM start "Клиент 3" cmd /c "java GameClient && pause"
@REM start "Клиент 4" cmd /c "java GameClient && pause"