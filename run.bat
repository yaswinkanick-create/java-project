@echo off
setlocal
if not exist dist\InventoryManager.jar call build.bat
if errorlevel 1 exit /b 1
java -jar dist\InventoryManager.jar
