@echo off
setlocal
if exist build rmdir /s /q build
if not exist dist mkdir dist
if exist dist\InventoryManager.jar del /q dist\InventoryManager.jar
mkdir build
javac -encoding UTF-8 -d build Inventory.java
if errorlevel 1 exit /b 1
jar --create --file dist\InventoryManager.jar --main-class Inventory -C build .
if errorlevel 1 exit /b 1
echo Build complete: dist\InventoryManager.jar
