#define NombreApp "Llega a tu Prime"
#define Version "1.0.0"
#define Editor "bey"
#define Ejecutable "Llega a tu Prime.exe"

[Setup]
AppId={{B7C2E5A1-4D3F-4E8A-9C61-0A5D7F3E2B48}
AppName={#NombreApp}
AppVersion={#Version}
AppVerName={#NombreApp} {#Version}
AppPublisher={#Editor}
VersionInfoVersion={#Version}
VersionInfoDescription=Instalador de {#NombreApp}
DefaultDirName={autopf}\{#NombreApp}
DefaultGroupName={#NombreApp}
DisableProgramGroupPage=yes
DisableWelcomePage=no
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir={#Salida}
OutputBaseFilename=LlegaATuPrime_Setup
SetupIconFile=bey.ico
UninstallDisplayIcon={app}\{#Ejecutable}
UninstallDisplayName={#NombreApp}
WizardStyle=modern
WizardImageFile=asistente-grande-164.bmp,asistente-grande-246.bmp,asistente-grande-328.bmp
WizardSmallImageFile=asistente-pequena-55.bmp,asistente-pequena-110.bmp
Compression=lzma2/max
SolidCompression=yes
CloseApplications=yes
RestartApplications=no
SetupLogging=yes

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "{#Origen}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#NombreApp}"; Filename: "{app}\{#Ejecutable}"
Name: "{group}\Desinstalar {#NombreApp}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#NombreApp}"; Filename: "{app}\{#Ejecutable}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#Ejecutable}"; Description: "{cm:LaunchProgram,{#NombreApp}}"; Flags: nowait postinstall skipifsilent
