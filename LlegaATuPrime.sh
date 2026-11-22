#!/usr/bin/env bash
# Lanzador de Llega a tu Prime para macOS / Linux
# Uso:
#   ./LlegaATuPrime.sh          -> compila si es necesario y lanza
#   ./LlegaATuPrime.sh /build   -> fuerza recompilar antes de lanzar

set -euo pipefail

CLASE_PRINCIPAL="com.bey.desktop.beyMainApp"
RAIZ="$(cd "$(dirname "$0")" && pwd)"
OBJETIVO="$RAIZ/target"
ARCHIVO_CP="$OBJETIVO/classpath.txt"
ARCHIVO_CLASE="$OBJETIVO/classes/com/bey/desktop/beyMainApp.class"
REGISTRO="$OBJETIVO/lanzador.log"

# ── Funciones de utilidad ────────────────────────────────────────────────────

salir() {
    echo "ERROR: $1" >&2
    # Mostrar ventana de error si hay entorno gráfico disponible
    if command -v osascript &>/dev/null; then
        osascript -e "display dialog \"$1\" with title \"Llega a tu Prime\" buttons {\"OK\"} default button \"OK\" with icon stop" &>/dev/null || true
    fi
    exit 1
}

buscar_java() {
    local java_cmd=""

    # 1. JAVA_HOME definido por el usuario
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        java_cmd="$JAVA_HOME/bin/java"

    # 2. Ubicaciones comunes en macOS (Homebrew, Adoptium, Oracle, Azul)
    elif [ -d "/Library/Java/JavaVirtualMachines" ]; then
        for jvm in /Library/Java/JavaVirtualMachines/*/Contents/Home; do
            if [ -x "$jvm/bin/java" ]; then
                java_cmd="$jvm/bin/java"
            fi
        done

    # 3. java en el PATH
    elif command -v java &>/dev/null; then
        java_cmd="$(command -v java)"
    fi

    echo "$java_cmd"
}

necesita_compilar() {
    [ -f "$ARCHIVO_CP" ]    || return 0
    [ -f "$ARCHIVO_CLASE" ] || return 0

    local fuente_nueva
    fuente_nueva="$(find "$RAIZ/src" -name "*.java" -newer "$ARCHIVO_CLASE" | head -1)"
    [ -z "$fuente_nueva" ] || return 0

    [ "$RAIZ/pom.xml" -nt "$ARCHIVO_CP" ] && return 0

    return 1
}

compilar() {
    local mvn=""

    # 1. Maven Wrapper del propio proyecto
    if [ -x "$RAIZ/mvnw" ]; then
        mvn="$RAIZ/mvnw"
    # 2. mvn en el PATH del sistema
    elif command -v mvn &>/dev/null; then
        mvn="$(command -v mvn)"
    # 3. Homebrew
    elif [ -x "/usr/local/bin/mvn" ]; then
        mvn="/usr/local/bin/mvn"
    elif [ -x "/opt/homebrew/bin/mvn" ]; then
        mvn="/opt/homebrew/bin/mvn"
    fi

    if [ -z "$mvn" ]; then
        salir "No se encontro Maven. Asegurate de que mvnw exista en la carpeta del proyecto."
    fi

    # Exportar JAVA_HOME si no está definido
    if [ -z "${JAVA_HOME:-}" ] && [ -n "$JAVA_CMD" ]; then
        export JAVA_HOME
        JAVA_HOME="$(dirname "$(dirname "$JAVA_CMD")")"
    fi

    mkdir -p "$OBJETIVO"
    rm -f "$ARCHIVO_CP"

    echo "Compilando la aplicacion, esto puede tardar unos segundos..."
    if ! "$mvn" -q compile dependency:build-classpath \
        -Dmdep.outputFile="$ARCHIVO_CP" \
        > "$REGISTRO" 2>&1; then
        salir "No se pudo compilar la aplicacion. Revisa los detalles en: $REGISTRO"
    fi

    if [ ! -f "$ARCHIVO_CP" ] || [ ! -f "$ARCHIVO_CLASE" ]; then
        salir "La compilacion termino pero faltan archivos. Revisa: $REGISTRO"
    fi
}

iniciar() {
    local cp ruta_modulos ruta_clases entrada

    cp="$(cat "$ARCHIVO_CP" | tr -d '\r\n')"
    ruta_modulos=""
    ruta_clases="$OBJETIVO/classes"

    # Separar JARs de JavaFX (modulos) del resto (classpath)
    IFS=':' read -ra partes <<< "$cp"
    for entrada in "${partes[@]}"; do
        if [[ "$entrada" == *"org/openjfx"* ]]; then
            ruta_modulos="${ruta_modulos}${entrada}:"
        else
            ruta_clases="${ruta_clases}:${entrada}"
        fi
    done
    # Quitar : final
    ruta_modulos="${ruta_modulos%:}"

    "$JAVA_CMD" \
        -Dfile.encoding=UTF-8 \
        --enable-native-access=ALL-UNNAMED \
        --module-path "$ruta_modulos" \
        --add-modules javafx.controls,javafx.fxml,javafx.media \
        -cp "$ruta_clases" \
        "$CLASE_PRINCIPAL" &
}

# ── Main ─────────────────────────────────────────────────────────────────────

OPCION="${1:-}"
FORZAR=false
[ "$OPCION" = "/build" ] && FORZAR=true

JAVA_CMD="$(buscar_java)"
if [ -z "$JAVA_CMD" ]; then
    salir "No se encontro Java. Instala un JDK 17 o superior desde https://adoptium.net"
fi

mkdir -p "$OBJETIVO"

if $FORZAR || necesita_compilar; then
    compilar
fi

iniciar
