; ============================================================
;  MediCare - Pharmacy Management System  |  Inno Setup 6
;  Developed by: Zohaib Asghar
;  Support: zohaiblashari154@gmail.com
; ============================================================

#define AppName      "MediCare Pharmacy Management System"
#define AppShortName "MediCare"
#define AppVersion   "1.0.0"
#define AppPublisher "Zohaib Asghar"
#define AppYear      "2026"

[Setup]
AppId={{E7D6C5B4-A3B2-4C1D-8E9F-0A1B2C3D4E5F}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL=mailto:zohaiblashari154@gmail.com
AppSupportURL=mailto:zohaiblashari154@gmail.com
AppCopyright=Copyright (C) {#AppYear} {#AppPublisher}
DefaultDirName={autopf}\{#AppShortName}
DefaultGroupName={#AppShortName}
AllowNoIcons=no
DisableDirPage=no
DisableProgramGroupPage=no
OutputDir=C:\Users\User\Desktop
OutputBaseFilename=MediCare-Setup-v1.0.0
SetupIconFile=C:\Users\User\Desktop\pharmacy-system\src\main\resources\rxpro.ico
LicenseFile=C:\Users\User\Desktop\pharmacy-system\installer\LICENSE.rtf
WizardStyle=modern
WizardSizePercent=120
WizardImageFile=C:\Users\User\Desktop\pharmacy-system\installer\wizard_side.bmp
WizardSmallImageFile=C:\Users\User\Desktop\pharmacy-system\installer\wizard_header.bmp
WizardImageStretch=yes
Compression=lzma2/ultra64
SolidCompression=yes
LZMAUseSeparateProcess=yes
LZMANumBlockThreads=4
PrivilegesRequired=admin
PrivilegesRequiredOverridesAllowed=dialog
ShowLanguageDialog=no
UsePreviousAppDir=yes
UsePreviousGroup=yes
CreateUninstallRegKey=yes
Uninstallable=yes
UninstallDisplayName={#AppName}
UninstallDisplayIcon={app}\MediCare.exe
VersionInfoVersion={#AppVersion}
VersionInfoCompany={#AppPublisher}
VersionInfoDescription={#AppName} Installer
VersionInfoCopyright=Copyright (C) {#AppYear} {#AppPublisher}
VersionInfoProductName={#AppName}
VersionInfoProductVersion={#AppVersion}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop shortcut"; GroupDescription: "Additional shortcuts:"
Name: "startupicon"; Description: "Start MediCare automatically at &Windows startup"; GroupDescription: "Additional shortcuts:"

[Files]
Source: "C:\Users\User\Desktop\pharmacy-system\MediCareLauncher.exe"; DestDir: "{app}"; DestName: "MediCare.exe"; Flags: ignoreversion
Source: "C:\Users\User\Desktop\pharmacy-system\target\medicare-system-1.0.0.jar"; DestDir: "{app}\target"; Flags: ignoreversion
Source: "C:\Users\User\Desktop\pharmacy-system\target\javafx-lib\*"; DestDir: "{app}\target\javafx-lib"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "C:\Users\User\Desktop\pharmacy-system\src\main\resources\rxpro.ico"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\User\Desktop\pharmacy-system\src\*"; DestDir: "{app}\src"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "C:\Users\User\Desktop\pharmacy-system\pom.xml"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\User\Desktop\pharmacy-system\maven-dist\*"; DestDir: "{app}\maven-dist"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\MediCare - Pharmacy Management System"; Filename: "{app}\MediCare.exe"; IconFilename: "{app}\rxpro.ico"; Comment: "Launch MediCare Pharmacy Management System"
Name: "{group}\Uninstall MediCare"; Filename: "{uninstallexe}"; IconFilename: "{app}\rxpro.ico"
Name: "{commondesktop}\MediCare Pharmacy"; Filename: "{app}\MediCare.exe"; IconFilename: "{app}\rxpro.ico"; Comment: "Launch MediCare"; Tasks: desktopicon
Name: "{userstartup}\MediCare Pharmacy"; Filename: "{app}\MediCare.exe"; Tasks: startupicon

[Registry]
Root: HKLM; Subkey: "SOFTWARE\Zohaib Asghar\MediCare"; ValueType: string; ValueName: "InstallPath"; ValueData: "{app}"; Flags: uninsdeletekey
Root: HKLM; Subkey: "SOFTWARE\Zohaib Asghar\MediCare"; ValueType: string; ValueName: "Version"; ValueData: "{#AppVersion}"
Root: HKLM; Subkey: "SOFTWARE\Zohaib Asghar\MediCare"; ValueType: string; ValueName: "Publisher"; ValueData: "Zohaib Asghar"
Root: HKLM; Subkey: "SOFTWARE\Zohaib Asghar\MediCare"; ValueType: string; ValueName: "Support"; ValueData: "zohaiblashari154@gmail.com"

[Run]
Filename: "{app}\MediCare.exe"; Description: "★ Launch MediCare Pharmacy Management System"; Flags: nowait postinstall skipifsilent

[Code]

var
  FeaturesPage:    TWizardPage;
  JavaCheckPage:   TWizardPage;
  CreditsPage:     TWizardPage;
  JavaStatusLabel: TLabel;
  JavaRetryBtn:    TButton;
  JavaOkLabel:     TLabel;

const
  GREEN_MAIN = $00378B3D;
  GREEN_DARK = $002B5C1E;
  GREEN_SEP  = $00C8DFC8;
  GREY_TEXT  = $00555555;

procedure MakeHeader(Parent: TWinControl; Txt: String; X,Y,W,H: Integer);
var L: TLabel;
begin
  L := TLabel.Create(WizardForm);
  L.Parent := Parent;
  L.Caption := Txt;
  L.Left := X; L.Top := Y; L.Width := W; L.Height := H;
  L.Font.Size := 11; L.Font.Style := [fsBold]; L.Font.Color := GREEN_MAIN;
  L.WordWrap := True;
end;

procedure MakeBody(Parent: TWinControl; Txt: String; X,Y,W,H: Integer);
var L: TLabel;
begin
  L := TLabel.Create(WizardForm);
  L.Parent := Parent;
  L.Caption := Txt;
  L.Left := X; L.Top := Y; L.Width := W; L.Height := H;
  L.Font.Size := 9; L.Font.Color := GREY_TEXT;
  L.WordWrap := True;
end;

procedure MakeSep(Parent: TWinControl; Y: Integer);
var P: TBevel;
begin
  P := TBevel.Create(WizardForm);
  P.Parent := Parent;
  P.Left := 0; P.Top := Y; P.Width := 390; P.Height := 2;
  P.Shape := bsTopLine;
end;

function JavaOK: Boolean;
var RC: Integer; Buf: AnsiString; Tmp: String; Loaded: Boolean;
begin
  Result := False;
  Buf := '';
  Tmp := ExpandConstant('{tmp}\jv.txt');
  Exec(ExpandConstant('{cmd}'), '/C java -version > "' + Tmp + '" 2>&1',
       '', SW_HIDE, ewWaitUntilTerminated, RC);
  Loaded := LoadStringFromFile(Tmp, Buf);
  if Loaded then begin
    if Pos('"17', String(Buf)) > 0 then Result := True;
    if Pos('"18', String(Buf)) > 0 then Result := True;
    if Pos('"19', String(Buf)) > 0 then Result := True;
    if Pos('"20', String(Buf)) > 0 then Result := True;
    if Pos('"21', String(Buf)) > 0 then Result := True;
    if Pos('"22', String(Buf)) > 0 then Result := True;
    if Pos('"23', String(Buf)) > 0 then Result := True;
    if Pos('"24', String(Buf)) > 0 then Result := True;
  end;
  DeleteFile(Tmp);
end;

procedure OpenJavaDL(Sender: TObject);
var EC: Integer;
begin
  ShellExec('open', 'https://adoptium.net/temurin/releases/?version=17', '', '', SW_SHOW, ewNoWait, EC);
end;

procedure DoRetry(Sender: TObject);
begin
  if JavaOK then begin
    JavaStatusLabel.Caption := '  Java 17+ detected - ready to install!';
    JavaStatusLabel.Font.Color := GREEN_MAIN;
    JavaOkLabel.Visible := True;
    JavaRetryBtn.Visible := False;
  end else begin
    JavaStatusLabel.Caption := '  Java 17+ still not found. Please install and retry.';
    JavaStatusLabel.Font.Color := $000000BB;
  end;
end;

procedure InitializeWizard;
var
  P: TWinControl;
  i: Integer;
  Lbl: TLabel;
  Btn: TButton;
  Titles: array[0..6] of String;
  Descs: array[0..6] of String;
begin
  { PAGE 1 - Features }
  FeaturesPage := CreateCustomPage(wpLicense, 'System Capabilities', 'Professional tools included with MediCare v1.0.0');
  P := FeaturesPage.Surface;
  MakeHeader(P, 'Professional Pharmacy Management Suite', 0, 0, 390, 24);
  MakeBody(P, 'A complete offline solution for pharmacies, medical stores, and healthcare businesses.', 0, 30, 390, 40);
  MakeSep(P, 68);

  Titles[0] := '★ Smart POS & Billing';
  Titles[1] := '★ Medicine Database';
  Titles[2] := '★ Inventory Control';
  Titles[3] := '★ Patient Records';
  Titles[4] := '★ Supplier Management';
  Titles[5] := '★ Purchase Management';
  Titles[6] := '★ Security & User Roles';
  Descs[0] := 'Full POS with cart, GST, discounts, cash/card/insurance payments and printable receipts.';
  Descs[1] := 'Add, edit and import medications with dosage, schedule type, and GST pricing.';
  Descs[2] := 'Batch-level stock tracking, expiry alerts, reorder thresholds, shelf locations.';
  Descs[3] := 'CNIC-based patient records with allergies, blood group, chronic conditions.';
  Descs[4] := 'Full supplier directory with contact info, balances, and payment terms.';
  Descs[5] := 'Create and track purchase orders, record deliveries, monitor payments.';
  Descs[6] := 'Role-based access: Admin, Pharmacist, Cashier, Store Manager.';

  for i := 0 to 6 do begin
    Lbl := TLabel.Create(WizardForm);
    Lbl.Parent := P;
    Lbl.Caption := '  ' + Titles[i];
    Lbl.Left := 0; Lbl.Top := 76 + (i*30); Lbl.Width := 390; Lbl.Height := 16;
    Lbl.Font.Size := 9; Lbl.Font.Style := [fsBold]; Lbl.Font.Color := GREEN_DARK;
    Lbl := TLabel.Create(WizardForm);
    Lbl.Parent := P;
    Lbl.Caption := '     ' + Descs[i];
    Lbl.Left := 0; Lbl.Top := 90 + (i*30); Lbl.Width := 390; Lbl.Height := 14;
    Lbl.Font.Size := 8; Lbl.Font.Color := GREY_TEXT;
  end;

  { PAGE 2 - Java Check }
  JavaCheckPage := CreateCustomPage(wpSelectDir, 'System Requirements', 'Checking required software components');
  P := JavaCheckPage.Surface;
  MakeHeader(P, 'Java 17 or Higher Required', 0, 0, 390, 22);
  MakeBody(P, 'MediCare runs on Java. This installer checks whether Java 17 or later is present on your system.', 0, 28, 390, 36);
  MakeSep(P, 72);

  JavaStatusLabel := TLabel.Create(WizardForm);
  JavaStatusLabel.Parent := P;
  JavaStatusLabel.Left := 0; JavaStatusLabel.Top := 84;
  JavaStatusLabel.Width := 390; JavaStatusLabel.Height := 20;
  JavaStatusLabel.Caption := 'Checking Java installation...';
  JavaStatusLabel.Font.Size := 10; JavaStatusLabel.Font.Style := [fsBold];
  JavaStatusLabel.Font.Color := $00888888;

  MakeSep(P, 112);
  MakeBody(P, 'If Java is not detected, click "Download Java 17" to download free. Install it, then click Verify Again.', 0, 120, 390, 36);

  Btn := TButton.Create(WizardForm);
  Btn.Parent := P; Btn.Left := 0; Btn.Top := 164;
  Btn.Width := 150; Btn.Height := 28;
  Btn.Caption := 'Download Java 17';
  Btn.OnClick := @OpenJavaDL;

  JavaRetryBtn := TButton.Create(WizardForm);
  JavaRetryBtn.Parent := P; JavaRetryBtn.Left := 158; JavaRetryBtn.Top := 164;
  JavaRetryBtn.Width := 100; JavaRetryBtn.Height := 28;
  JavaRetryBtn.Caption := 'Verify Again';
  JavaRetryBtn.OnClick := @DoRetry;

  JavaOkLabel := TLabel.Create(WizardForm);
  JavaOkLabel.Parent := P;
  JavaOkLabel.Left := 0; JavaOkLabel.Top := 200;
  JavaOkLabel.Width := 390; JavaOkLabel.Height := 18;
  JavaOkLabel.Caption := '  Java detected. Click Next to continue.';
  JavaOkLabel.Font.Size := 9; JavaOkLabel.Font.Color := GREEN_MAIN;
  JavaOkLabel.Visible := False;

  { PAGE 3 - Credits }
  CreditsPage := CreateCustomPage(wpSelectTasks, 'About MediCare', 'Product information, support and licensing');
  P := CreditsPage.Surface;
  MakeHeader(P, 'MediCare - Pharmacy Management System', 0, 0, 390, 24);
  MakeSep(P, 30);
  MakeBody(P, 'Developed by:', 0, 40, 390, 16);

  Lbl := TLabel.Create(WizardForm);
  Lbl.Parent := P; Lbl.Left := 0; Lbl.Top := 58;
  Lbl.Width := 390; Lbl.Height := 22;
  Lbl.Caption := 'Zohaib Asghar';
  Lbl.Font.Size := 14; Lbl.Font.Style := [fsBold]; Lbl.Font.Color := GREEN_MAIN;

  MakeSep(P, 88);
  MakeBody(P, 'MediCare is a fully offline, enterprise-ready Pharmacy Management System built with ❤️.', 0, 96, 390, 56);
  MakeSep(P, 158);
  MakeBody(P, 'Technical Support and Licensing:', 0, 166, 390, 16);

  Lbl := TLabel.Create(WizardForm);
  Lbl.Parent := P; Lbl.Left := 0; Lbl.Top := 184;
  Lbl.Width := 390; Lbl.Height := 16;
  Lbl.Caption := 'zohaiblashari154@gmail.com';
  Lbl.Font.Size := 10; Lbl.Font.Style := [fsBold]; Lbl.Font.Color := GREEN_DARK;

  MakeSep(P, 208);
  MakeBody(P, 'Default Login:   Username: admin   |   Password: admin123', 0, 216, 390, 20);
  MakeBody(P, 'Please change your password immediately after the first login.', 0, 240, 390, 32);
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  if CurPageID = JavaCheckPage.ID then begin
    if JavaOK then begin
      JavaStatusLabel.Caption := '  Java 17+ detected - Click Next!';
      JavaStatusLabel.Font.Color := GREEN_MAIN;
      JavaOkLabel.Visible := True;
      JavaRetryBtn.Visible := False;
    end else begin
      JavaStatusLabel.Caption := '  Java 17+ not found on this computer.';
      JavaStatusLabel.Font.Color := $000000BB;
      JavaOkLabel.Visible := False;
      JavaRetryBtn.Visible := True;
    end;
  end;
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if CurPageID = JavaCheckPage.ID then begin
    if not JavaOK then begin
      MsgBox('Java 17 or higher was not detected.' + #13#10 + #13#10 + 'Please install OpenJDK 17 from adoptium.net, then click Verify Again.', mbError, MB_OK);
      Result := False;
    end;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  AppDir, Bat: String;
begin
  if CurStep = ssPostInstall then begin
    AppDir := ExpandConstant('{app}');
    Bat := AppDir + '\run.bat';
    SaveStringToFile(Bat,
      '@echo off' + #13#10 +
      'title MediCare - Pharmacy Management System' + #13#10 +
      'cd /d "%~dp0"' + #13#10 +
      'javaw --module-path "' + AppDir + '\target\javafx-lib" --add-modules javafx.controls,javafx.web,javafx.graphics,javafx.base,javafx.media --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED --add-opens javafx.web/com.sun.webkit=ALL-UNNAMED -jar "' + AppDir + '\target\medicare-system-1.0.0.jar"' + #13#10,
      False);
  end;
end;
