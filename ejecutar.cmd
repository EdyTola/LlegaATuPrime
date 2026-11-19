@echo off
rem Atajo de compatibilidad: delega en LlegaATuPrime.vbs (arranque con javaw, sin consola).
rem Para no ver ni un destello de CMD, abre directamente LlegaATuPrime.vbs.
start "" wscript.exe //Nologo "%~dp0LlegaATuPrime.vbs" %*
exit /b 0
