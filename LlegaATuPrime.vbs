Option Explicit

Const CLASE_PRINCIPAL = "com.bey.desktop.beyMainApp"

Dim sh, fso, raiz, objetivo, archivoCp, archivoClase, registro
Set sh = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

raiz = fso.GetParentFolderName(WScript.ScriptFullName)
sh.CurrentDirectory = raiz
objetivo = raiz & "\target"
archivoCp = objetivo & "\classpath.txt"
archivoClase = objetivo & "\classes\com\bey\desktop\beyMainApp.class"
registro = objetivo & "\lanzador.log"

Dim javaw, forzar, opcion
javaw = BuscarJavaw()
If javaw = "" Then
    Salir "No se encontro Java (javaw.exe)." & vbCrLf & "Instala un JDK 17 o superior o define JAVA_HOME."
End If

opcion = ""
If WScript.Arguments.Count > 0 Then opcion = LCase(WScript.Arguments(0))

If opcion = "/acceso" Then
    CrearAcceso
    WScript.Quit 0
End If

forzar = (opcion = "/build")
If Not fso.FolderExists(objetivo) Then fso.CreateFolder objetivo

If forzar Or NecesitaCompilar() Then
    Compilar
End If

Iniciar
WScript.Quit 0

Sub Iniciar()
    Dim cp, partes, i, rutaModulos, rutaClases, argumentos
    cp = LeerArchivo(archivoCp)
    cp = Replace(Replace(cp, vbCr, ""), vbLf, "")
    partes = Split(cp, ";")
    rutaModulos = ""
    rutaClases = objetivo & "\classes"
    For i = 0 To UBound(partes)
        If Trim(partes(i)) <> "" Then
            If InStr(LCase(partes(i)), "org\openjfx") > 0 Or InStr(LCase(partes(i)), "org/openjfx") > 0 Then
                rutaModulos = rutaModulos & partes(i) & ";"
            Else
                rutaClases = rutaClases & ";" & partes(i)
            End If
        End If
    Next
    argumentos = "-Dfile.encoding=UTF-8 --enable-native-access=ALL-UNNAMED" _
        & " --module-path """ & rutaModulos & """" _
        & " --add-modules javafx.controls,javafx.fxml,javafx.media" _
        & " -cp """ & rutaClases & """ " & CLASE_PRINCIPAL

    CreateObject("Shell.Application").ShellExecute javaw, argumentos, raiz, "open", 1
End Sub

Function NecesitaCompilar()
    NecesitaCompilar = True
    If Not fso.FileExists(archivoCp) Then Exit Function
    If Not fso.FileExists(archivoClase) Then Exit Function
    Dim fuenteMasNueva, fechaClase
    fechaClase = fso.GetFile(archivoClase).DateLastModified
    fuenteMasNueva = FechaMasNueva(fso.GetFolder(raiz & "\src"))
    If fuenteMasNueva > fechaClase Then Exit Function
    If fso.GetFile(raiz & "\pom.xml").DateLastModified > fso.GetFile(archivoCp).DateLastModified Then Exit Function
    NecesitaCompilar = False
End Function

Function FechaMasNueva(carpeta)
    Dim f, sub1, mayor, d
    mayor = CDate("2000-01-01")
    For Each f In carpeta.Files
        If f.DateLastModified > mayor Then mayor = f.DateLastModified
    Next
    For Each sub1 In carpeta.SubFolders
        d = FechaMasNueva(sub1)
        If d > mayor Then mayor = d
    Next
    FechaMasNueva = mayor
End Function

Sub Compilar()
    Dim mvn, comando, codigo, env, q
    mvn = BuscarMaven()
    If mvn = "" Then
        Salir "No se encontro Maven (mvn.cmd) y la aplicacion necesita compilarse." & vbCrLf & _
              "Instala Maven y vuelve a abrir este archivo."
    End If

    Set env = sh.Environment("Process")
    If env("JAVA_HOME") = "" Then env("JAVA_HOME") = fso.GetParentFolderName(fso.GetParentFolderName(javaw))
    If fso.FileExists(archivoCp) Then fso.DeleteFile archivoCp, True
    q = Chr(34)
    comando = "cmd.exe /c " & q & q & mvn & q & " -q compile dependency:build-classpath" & _
              " -Dmdep.outputFile=" & q & archivoCp & q & " > " & q & registro & q & " 2>&1" & q

    codigo = sh.Run(comando, 0, True)
    If codigo <> 0 Or Not fso.FileExists(archivoCp) Or Not fso.FileExists(archivoClase) Then
        Salir "No se pudo compilar la aplicacion (codigo " & codigo & ")." & vbCrLf & _
              "Revisa los detalles en:" & vbCrLf & registro
    End If
End Sub

Function BuscarJavaw()
    Dim candidatos, c, ruta, base, carpeta, sub1
    BuscarJavaw = ""
    ruta = sh.Environment("Process")("JAVA_HOME")
    If ruta <> "" Then
        If fso.FileExists(ruta & "\bin\javaw.exe") Then BuscarJavaw = ruta & "\bin\javaw.exe": Exit Function
    End If
    For Each base In Array(sh.ExpandEnvironmentStrings("%ProgramFiles%") & "\Java", _
                           sh.ExpandEnvironmentStrings("%ProgramFiles%") & "\Eclipse Adoptium", _
                           sh.ExpandEnvironmentStrings("%ProgramFiles%") & "\Microsoft")
        If fso.FolderExists(base) Then
            For Each sub1 In fso.GetFolder(base).SubFolders
                If fso.FileExists(sub1.Path & "\bin\javaw.exe") Then BuscarJavaw = sub1.Path & "\bin\javaw.exe"
            Next
            If BuscarJavaw <> "" Then Exit Function
        End If
    Next
    BuscarJavaw = BuscarEnPath("javaw.exe")
End Function

Function BuscarMaven()
    Dim ruta
    BuscarMaven = BuscarEnPath("mvn.cmd")
    If BuscarMaven <> "" Then Exit Function
    ruta = sh.ExpandEnvironmentStrings("%ProgramFiles%") & "\apache-maven-3.9.14\bin\mvn.cmd"
    If fso.FileExists(ruta) Then BuscarMaven = ruta: Exit Function
    ruta = sh.ExpandEnvironmentStrings("%TEMP%") & "\bey-buildtools\apache-maven-3.9.9\bin\mvn.cmd"
    If fso.FileExists(ruta) Then BuscarMaven = ruta
End Function

Function BuscarEnPath(nombre)
    Dim carpetas, c
    BuscarEnPath = ""
    carpetas = Split(sh.ExpandEnvironmentStrings("%PATH%"), ";")
    For Each c In carpetas
        If Trim(c) <> "" Then
            If fso.FileExists(c & "\" & nombre) Then BuscarEnPath = c & "\" & nombre: Exit Function
        End If
    Next
End Function

Function LeerArchivo(ruta)
    Dim f
    Set f = fso.OpenTextFile(ruta, 1, False)
    LeerArchivo = f.ReadAll
    f.Close
End Function

Sub CrearAcceso()
    Dim escritorio, enlace
    escritorio = sh.SpecialFolders("Desktop")
    Set enlace = sh.CreateShortcut(escritorio & "\Llega a tu Prime.lnk")
    enlace.TargetPath = sh.ExpandEnvironmentStrings("%SystemRoot%") & "\System32\wscript.exe"
    enlace.Arguments = """" & WScript.ScriptFullName & """"
    enlace.WorkingDirectory = raiz
    enlace.WindowStyle = 7
    enlace.Description = "Llega a tu Prime"
    enlace.IconLocation = javaw & ",0"
    enlace.Save
    MsgBox "Acceso directo creado en el Escritorio: Llega a tu Prime", 64, "Llega a tu Prime"
End Sub

Sub Salir(mensaje)
    MsgBox mensaje, 16, "Llega a tu Prime"
    WScript.Quit 1
End Sub
