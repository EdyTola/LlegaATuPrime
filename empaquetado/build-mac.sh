#!/usr/bin/env bash
set -euo pipefail

NOMBRE="Llega a tu Prime"
NOMBRE_MENU="LlegaATuPrime"
VERSION="1.0.0"
PROVEEDOR="bey"
IDENTIFICADOR="com.bey.llegaatuprime"
CATEGORIA="public.app-category.healthcare-fitness"
DESCRIPCION="Entrenamiento, nutricion y habitos saludables"
CLASE_PRINCIPAL="com.bey.desktop.beyLanzador"

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
EMPAQUETADO="$RAIZ/empaquetado"
TRABAJO="$RAIZ/target/instalador-mac"
ENTRADA="$TRABAJO/entrada"
DESTINO="$RAIZ/dist-mac"

paso() {
    printf '\n==> %s\n' "$1"
}

fallar() {
    printf 'ERROR: %s\n' "$1" >&2
    exit 1
}

[ "$(uname -s)" = "Darwin" ] || fallar "Este script solo funciona en macOS: jpackage crea el .dmg con las herramientas del sistema operativo donde se ejecuta."

case "$(uname -m)" in
    arm64) ARQUITECTURA="arm64" ;;
    x86_64) ARQUITECTURA="x64" ;;
    *) fallar "Arquitectura no soportada: $(uname -m)" ;;
esac

buscar_jdk() {
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/jpackage" ]; then
        echo "$JAVA_HOME"
        return 0
    fi
    if /usr/libexec/java_home -v 17+ >/dev/null 2>&1; then
        local candidato
        candidato="$(/usr/libexec/java_home -v 17+)"
        if [ -x "$candidato/bin/jpackage" ]; then
            echo "$candidato"
            return 0
        fi
    fi
    if command -v jpackage >/dev/null 2>&1; then
        dirname "$(dirname "$(command -v jpackage)")"
        return 0
    fi
    return 1
}

paso "Buscando JDK (jpackage)"
JDK="$(buscar_jdk)" || fallar "No se encontro un JDK 17 o superior con jpackage. Instala uno (por ejemplo Temurin 21) y define JAVA_HOME."
export JAVA_HOME="$JDK"
echo "$JDK"

[ -f "$EMPAQUETADO/bey.icns" ] || fallar "Falta empaquetado/bey.icns (icono de macOS)."

paso "Compilando la aplicacion con Maven ($ARQUITECTURA)"
rm -rf "$TRABAJO" "$DESTINO"
mkdir -p "$ENTRADA" "$DESTINO"
if [ -f "$RAIZ/mvnw" ]; then
    MAVEN=(sh "$RAIZ/mvnw")
else
    MAVEN=(mvn)
fi
(cd "$RAIZ" && "${MAVEN[@]}" -q compile jar:jar dependency:copy-dependencies "-DoutputDirectory=$ENTRADA" -DincludeScope=runtime)

JAR="$(ls "$RAIZ"/target/bey-desktop-*.jar 2>/dev/null | head -n 1)"
[ -n "$JAR" ] || fallar "No se genero el archivo JAR de la aplicacion."
cp "$JAR" "$ENTRADA/"

ls "$ENTRADA"/javafx-graphics-*mac*.jar >/dev/null 2>&1 || fallar "Las librerias de JavaFX copiadas no son de macOS. Ejecuta este script en una Mac (o en un runner macos de GitHub Actions)."

paso "Calculando los modulos de Java necesarios"
MODULOS="$("$JAVA_HOME/bin/jdeps" --ignore-missing-deps --multi-release base --print-module-deps --class-path "$ENTRADA/*" "$JAR" 2>/dev/null | head -n 1 || true)"
MODULOS="${MODULOS},java.desktop,java.logging,java.xml,java.net.http,java.prefs,jdk.unsupported,jdk.crypto.ec"
MODULOS="$(printf '%s' "$MODULOS" | tr ',' '\n' | grep -v '^$' | sort -u | paste -sd, -)"
echo "$MODULOS"

paso "Creando la aplicacion con su propio Java (jpackage)"
"$JAVA_HOME/bin/jpackage" \
    --type app-image \
    --name "$NOMBRE" \
    --app-version "$VERSION" \
    --vendor "$PROVEEDOR" \
    --description "$DESCRIPCION" \
    --icon "$EMPAQUETADO/bey.icns" \
    --input "$ENTRADA" \
    --main-jar "$(basename "$JAR")" \
    --main-class "$CLASE_PRINCIPAL" \
    --add-modules "$MODULOS" \
    --java-options "-Dfile.encoding=UTF-8" \
    --java-options "--enable-native-access=ALL-UNNAMED" \
    --mac-package-name "$NOMBRE_MENU" \
    --mac-package-identifier "$IDENTIFICADOR" \
    --mac-app-category "$CATEGORIA" \
    --dest "$TRABAJO" \
    --temp "$TRABAJO/temporal"

APP="$TRABAJO/$NOMBRE.app"
[ -d "$APP" ] || fallar "jpackage no genero $APP"

paso "Aplicando firma ad hoc (necesaria para ejecutar en Apple Silicon)"
codesign --force --deep --sign - "$APP"
codesign --verify --deep --strict "$APP"

paso "Creando el .dmg"
"$JAVA_HOME/bin/jpackage" \
    --type dmg \
    --app-image "$APP" \
    --name "$NOMBRE" \
    --app-version "$VERSION" \
    --mac-package-identifier "$IDENTIFICADOR" \
    --dest "$DESTINO" \
    --temp "$TRABAJO/temporal-dmg"

BASE="LlegaATuPrime-$VERSION-macOS-$ARQUITECTURA"
DMG_GENERADO="$(ls "$DESTINO"/*.dmg | head -n 1)"
mv "$DMG_GENERADO" "$DESTINO/$BASE.dmg"

paso "Creando el .zip de la aplicacion (alternativa al .dmg)"
ditto -c -k --sequesterRsrc --keepParent "$APP" "$DESTINO/$BASE.zip"

paso "Calculando sumas de verificacion"
(cd "$DESTINO" && shasum -a 256 "$BASE.dmg" "$BASE.zip" > "$BASE.sha256")

paso "Listo"
ls -lh "$DESTINO"
