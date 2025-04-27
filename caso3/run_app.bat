@echo off
echo Compiling the application...
javac -d . src\main\java\com\infracomp\caso3\security\*.java
javac -d . src\main\java\com\infracomp\caso3\server\*.java
javac -d . src\main\java\com\infracomp\caso3\client\*.java
javac -d . src\main\java\com\infracomp\caso3\test\*.java
javac -d . src\main\java\com\infracomp\caso3\Main.java

echo Running the application...
java com.infracomp.caso3.Main

echo Test completed.
pause 