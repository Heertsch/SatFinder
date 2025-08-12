@echo off
echo Erstelle Git Tag 0.3 für SatFinder Projekt
echo =========================================

cd /d "D:\Users\heert\StudioProjects\SatFinder_02"

echo Prüfe Git Status...
git status

echo.
echo Füge alle Änderungen hinzu...
git add .

echo.
echo Committe aktuelle Änderungen...
git commit -m "Version 0.3 - PowerPoint Kompatibilität dokumentiert"

echo.
echo Erstelle Git Tag 0.3...
git tag -a v0.3 -m "Version 0.3: SatFinder mit PowerPoint-Kompatibilitäts-Dokumentation"

echo.
echo Zeige erstellte Tags...
git tag -l

echo.
echo Pushe Commits zu GitHub...
git push origin main

echo.
echo Pushe Tags zu GitHub...
git push origin --tags

echo.
echo Fertig! Tag v0.3 wurde erfolgreich erstellt und zu GitHub gepusht.
pause
