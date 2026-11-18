package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.DiaSalud;
import com.symmetry.desktop.model.MetaXp;
import com.symmetry.desktop.model.ObjetivosNutricionales;
import com.symmetry.desktop.model.PerfilUsuario;
import com.symmetry.desktop.model.RetoDiario;
import com.symmetry.desktop.model.Rutina;
import com.symmetry.desktop.service.GamificacionService;
import com.symmetry.desktop.service.GamificacionService.EstadoMeta;
import com.symmetry.desktop.service.GamificacionService.InfoNivel;
import com.symmetry.desktop.service.GestorDatos;
import com.symmetry.desktop.service.QuoteService;
import com.symmetry.desktop.util.AchievementVFX;
import javafx.application.Platform;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import com.symmetry.desktop.service.QuoteService.Frase;
import com.symmetry.desktop.util.Formato;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Modulo 1 - Panel de inicio: saludo personalizado, resumen del dia (rutina, macros,
 * hidratacion, creatina y descanso) y recomendaciones basadas en evidencia presentadas
 * como una cuadricula de tarjetas de consejos interactivas con iconos ilustrativos.
 */
public class DashboardController implements Initializable, VistaConRefresco {

    private static final String CLASE_VERDE = "progreso-verde";
    private static final String CLASE_AZUL = "progreso-azul";
    private static final String CLASE_COMPLETO = "progreso-completo";

    /** Siluetas vectoriales (viewBox 40x40) de cada icono de consejo. */
    private static final String SVG_SOBRECARGA =
            "M4,36 L11,36 L11,26 L4,26 Z M13,36 L20,36 L20,18 L13,18 Z M22,36 L29,36 L29,11 L22,11 Z "
            + "M30,36 L33,36 L33,15 L30,15 Z M26,15 L37,15 L31.5,5 Z";
    private static final String SVG_SUENO =
            "M35,21.3 A15,15 0 1 1 18.7,5 A11.7,11.7 0 0 0 35,21.3 Z";
    private static final String SVG_AGUA =
            "M20,4 C27,13 32,19 32,25 A12,12 0 1 1 8,25 C8,19 13,13 20,4 Z";
    private static final String SVG_PROTEINA =
            "M17,6 L23,6 L23,12 L27,14 L27,35 Q27,38 24,38 L16,38 Q13,38 13,35 L13,14 L17,12 Z";
    private static final String SVG_CREATINA =
            "M17,6 L23,6 L23,14 L30,30 A4,4 0 0 1 26.5,36 L13.5,36 A4,4 0 0 1 10,30 L17,14 Z";
    private static final String SVG_REGISTRO =
            "M8,21 L12,17 L17,22 L31,8 L35,12 L17,30 Z";
    private static final String SVG_RECUPERACION =
            "M32,7 C17,7 7,16 7,31 C7,33 8,34 10,34 C25,34 33,23 32,7 Z";

    private static final double LADO_ICONO = 40;
    private static final double ANCHO_TARJETA = 300;

    private final GestorDatos gestor = GestorDatos.getInstancia();

    @FXML
    private Label etiquetaSaludo;
    @FXML
    private Label etiquetaFecha;
    @FXML
    private Label etiquetaEstadoDia;
    @FXML
    private Label valorRutina;
    @FXML
    private Label detalleRutina;
    @FXML
    private Label valorProteina;
    @FXML
    private ProgressBar barraProteina;
    @FXML
    private Label detalleProteina;
    @FXML
    private Label valorCalorias;
    @FXML
    private ProgressBar barraCalorias;
    @FXML
    private Label detalleCalorias;
    @FXML
    private Label valorHidratacion;
    @FXML
    private ProgressBar barraHidratacion;
    @FXML
    private Label detalleHidratacion;
    @FXML
    private Label valorCreatina;
    @FXML
    private ProgressBar barraCreatina;
    @FXML
    private Label detalleCreatina;
    @FXML
    private Label valorSueno;
    @FXML
    private ProgressBar barraSueno;
    @FXML
    private Label detalleSueno;
    @FXML
    private FlowPane rejillaConsejos;
    @FXML
    private Label etiquetaFrase;
    @FXML
    private Label etiquetaAutorFrase;
    @FXML
    private Label etiquetaNivelNumero;
    @FXML
    private Label etiquetaNivelTitulo;
    @FXML
    private ProgressBar barraXp;
    @FXML
    private Label etiquetaXp;
    @FXML
    private Label etiquetaXpResumen;
    @FXML
    private VBox listaMetas;
    @FXML
    private FlowPane rejillaRetos;
    @FXML
    private Label etiquetaRetosEstado;

    /** Reevalua cada minuto para renovar retos vencidos y actualizar las cuentas atras. */
    private Timeline relojRpg;

    /** Filas y tarjetas visibles, para dispararles la micro-interaccion al cumplirse. */
    private final Map<MetaXp, HBox> filasMeta = new EnumMap<>(MetaXp.class);
    private final Map<String, VBox> tarjetasReto = new HashMap<>();
    /** Logros ocurridos durante la ultima evaluacion (XP por meta / ids de retos). */
    private final Map<MetaXp, Integer> metasNuevas = new EnumMap<>(MetaXp.class);
    private final Map<String, Integer> retosNuevos = new HashMap<>();
    private boolean escuchaRegistrada;

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        refrescar();
    }

    @Override
    public void refrescar() {
        LocalDate hoy = LocalDate.now();
        PerfilUsuario perfil = gestor.getDatos().getPerfil();
        ObjetivosNutricionales objetivos = perfil.getObjetivos();
        DiaSalud dia = gestor.getDatos().buscarDia(hoy).orElseGet(() -> new DiaSalud(hoy));

        etiquetaSaludo.setText(saludo() + ", " + perfil.getPrimerNombre());
        etiquetaFecha.setText(Formato.fechaLarga(hoy));

        actualizarRutina(hoy);
        actualizarProteina(dia, objetivos);
        actualizarCalorias(dia, objetivos);
        actualizarHidratacion(dia, objetivos);
        actualizarCreatina(dia, objetivos);
        actualizarSueno(dia, objetivos);
        actualizarRecomendaciones(dia, objetivos, hoy);
        mostrarFrase(QuoteService.fraseDelDia(perfil, hoy));
        actualizarRpg();
        iniciarRelojRpg();
    }

    // ------------------------------------------------------------------
    // Frase de poder
    // ------------------------------------------------------------------

    private void mostrarFrase(Frase frase) {
        etiquetaFrase.setText(frase.texto());
        etiquetaAutorFrase.setText("\u2014 " + frase.autor());
        etiquetaAutorFrase.getStyleClass().remove("frase-autor-personal");
        if (frase.personal()) {
            etiquetaAutorFrase.getStyleClass().add("frase-autor-personal");
        }
    }

    /** Cambia a otra frase (distinta de la actual) sin salir del Inicio. */
    @FXML
    private void otraFrase() {
        mostrarFrase(QuoteService.siguiente(gestor.getDatos().getPerfil(), LocalDate.now()));
    }

    // ------------------------------------------------------------------
    // Gamificacion: nivel, metas del dia y retos
    // ------------------------------------------------------------------

    /** Observa los logros para animar las filas y tarjetas del propio panel. */
    private void registrarEscuchaVfx() {
        if (escuchaRegistrada) {
            return;
        }
        escuchaRegistrada = true;
        gestor.getGamificacion().agregarEscucha(new GamificacionService.Escucha() {
            @Override
            public void metaCumplida(MetaXp meta, int xp) {
                metasNuevas.put(meta, xp);
            }

            @Override
            public void retoCompletado(RetoDiario reto) {
                retosNuevos.put(reto.getId(), reto.getXpRecompensa());
            }
        });
    }

    private void iniciarRelojRpg() {
        if (relojRpg != null) {
            return;
        }
        relojRpg = new Timeline(new KeyFrame(Duration.minutes(1), evento -> {
            // Si la vista ya no esta en pantalla se pausa; refrescar() la reanuda al volver.
            if (rejillaRetos.getScene() == null) {
                relojRpg.stop();
                relojRpg = null;
                return;
            }
            actualizarRpg();
        }));
        relojRpg.setCycleCount(Animation.INDEFINITE);
        relojRpg.play();
    }

    private void actualizarRpg() {
        GamificacionService rpg = gestor.getGamificacion();
        registrarEscuchaVfx();
        metasNuevas.clear();
        retosNuevos.clear();
        rpg.evaluar();

        InfoNivel nivel = rpg.infoNivel();
        etiquetaNivelNumero.setText(String.valueOf(nivel.nivel()));
        etiquetaNivelTitulo.setText("Nivel " + nivel.nivel() + " · " + nivel.titulo());
        barraXp.setProgress(nivel.fraccion());
        etiquetaXp.setText(nivel.xpEnNivel() + " / " + nivel.xpParaSiguiente() + " XP para el nivel "
                + (nivel.nivel() + 1) + " (" + GamificacionService.titulo(nivel.nivel() + 1) + ")");
        etiquetaXpResumen.setText(nivel.xpTotal() + " XP totales · "
                + gestor.getDatos().getProgresoRpg().getRetosCompletados() + " retos completados");

        listaMetas.getChildren().clear();
        filasMeta.clear();
        for (EstadoMeta estado : rpg.metasDeHoy()) {
            HBox fila = filaMeta(estado);
            filasMeta.put(estado.meta(), fila);
            listaMetas.getChildren().add(fila);
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<RetoDiario> retos = rpg.retosVigentes();
        rejillaRetos.getChildren().clear();
        tarjetasReto.clear();
        for (RetoDiario reto : retos) {
            VBox tarjeta = tarjetaReto(reto, ahora);
            tarjetasReto.put(reto.getId(), tarjeta);
            rejillaRetos.getChildren().add(tarjeta);
        }
        celebrarLogrosNuevos();
        long completados = retos.stream().filter(RetoDiario::isCompletado).count();
        etiquetaRetosEstado.setText(completados + " de " + retos.size() + " completados");
    }

    /** Pulso de brillo y "+XP" flotante sobre cada fila o tarjeta recien cumplida. */
    private void celebrarLogrosNuevos() {
        if (metasNuevas.isEmpty() && retosNuevos.isEmpty()) {
            return;
        }
        Map<MetaXp, Integer> metas = new EnumMap<>(metasNuevas);
        Map<String, Integer> retos = new HashMap<>(retosNuevos);
        // Se espera al siguiente pulso para que las filas ya tengan posicion en pantalla.
        Platform.runLater(() -> {
            metas.forEach((meta, xp) -> {
                HBox fila = filasMeta.get(meta);
                if (fila != null && fila.getScene() != null) {
                    AchievementVFX.celebrarMeta(fila, "+" + xp + " XP");
                }
            });
            retos.forEach((id, xp) -> {
                VBox tarjeta = tarjetasReto.get(id);
                if (tarjeta != null && tarjeta.getScene() != null) {
                    AchievementVFX.celebrarMeta(tarjeta, "+" + xp + " XP · ¡Completado!");
                }
            });
        });
    }

    private static HBox filaMeta(EstadoMeta estado) {
        boolean hecha = estado.cumplida() || estado.reclamada();
        Label icono = new Label(hecha ? "\u2713" : "\u25CB");
        icono.getStyleClass().add("meta-icono");
        if (hecha) {
            icono.getStyleClass().add("meta-icono-hecha");
        }
        Label texto = new Label(estado.meta().getTitulo());
        texto.getStyleClass().add("meta-texto");
        if (hecha) {
            texto.getStyleClass().add("meta-texto-hecha");
        }
        Label xp = new Label("+" + estado.meta().getXp() + " XP");
        xp.getStyleClass().add("chip-xp");
        if (hecha) {
            xp.getStyleClass().add("chip-xp-hecho");
        }
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox fila = new HBox(8, icono, texto, espacio, xp);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("meta-fila");
        Tooltip.install(fila, new Tooltip(estado.meta().getDescripcion()));
        return fila;
    }

    private VBox tarjetaReto(RetoDiario reto, LocalDateTime ahora) {
        StringBuilder etiqueta = new StringBuilder(reto.isFlash() ? "FLASH · 12 H" : "DIARIO · 24 H");
        etiqueta.append("  ·  ").append(reto.getTipo().getEtiqueta().toUpperCase(Locale.ROOT));
        if (reto.getGrupoMuscular() != null) {
            etiqueta.append("  ·  ").append(reto.getGrupoMuscular().getEtiqueta().toUpperCase(Locale.ROOT));
        }
        Label tipo = new Label(etiqueta.toString());
        tipo.getStyleClass().add("reto-tipo");
        if (reto.isFlash()) {
            tipo.getStyleClass().add("reto-tipo-flash");
        }
        Label xp = new Label("+" + reto.getXpRecompensa() + " XP");
        xp.getStyleClass().add("chip-xp");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(8, tipo, espacio, xp);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label(reto.getTitulo());
        titulo.getStyleClass().add("reto-titulo");
        titulo.setWrapText(true);
        Label descripcion = new Label(reto.getDescripcion());
        descripcion.getStyleClass().add("reto-descripcion");
        descripcion.setWrapText(true);

        ProgressBar barra = new ProgressBar(reto.getFraccion());
        barra.setMaxWidth(Double.MAX_VALUE);
        barra.getStyleClass().add("barra-xp");

        Label estado = new Label(reto.isCompletado()
                ? "\u2713 Completado · XP otorgada"
                : reto.getProgresoTexto() + " · expira en " + reto.restante(ahora));
        estado.getStyleClass().add(reto.isCompletado() ? "reto-completado" : "texto-secundario");

        VBox tarjeta = new VBox(7, cabecera, titulo, descripcion, barra, estado);
        tarjeta.getStyleClass().add("tarjeta-reto");
        if (reto.isFlash()) {
            tarjeta.getStyleClass().add("tarjeta-reto-flash");
        }
        if (reto.esManual()) {
            tarjeta.getStyleClass().add("tarjeta-reto-recuperacion");
        }
        if (reto.isCompletado()) {
            tarjeta.getStyleClass().add("tarjeta-reto-completo");
        } else if (reto.esManual()) {
            Button confirmar = new Button("Marcar como hecho");
            confirmar.getStyleClass().add("boton-reto-completar");
            confirmar.setMaxWidth(Double.MAX_VALUE);
            confirmar.setOnAction(evento -> {
                gestor.getGamificacion().completarReto(reto.getId());
                actualizarRpg();
            });
            tarjeta.getChildren().add(confirmar);
        }
        tarjeta.setPrefWidth(ANCHO_TARJETA);
        tarjeta.setMinWidth(ANCHO_TARJETA - 40);
        return tarjeta;
    }

    private String saludo() {
        int hora = LocalTime.now().getHour();
        if (hora < 12) {
            return "Buenos días";
        }
        if (hora < 19) {
            return "Buenas tardes";
        }
        return "Buenas noches";
    }

    private void actualizarRutina(LocalDate hoy) {
        Optional<Rutina> rutina = gestor.rutinaDe(hoy);
        if (rutina.isPresent()) {
            Rutina r = rutina.get();
            valorRutina.setText(r.getNombre());
            detalleRutina.setText(r.getDiaSemana().getEtiqueta() + " · "
                    + r.getEjercicios().size() + " ejercicios · " + r.getSeriesTotales() + " series");
            etiquetaEstadoDia.setText("Día de entrenamiento");
        } else {
            valorRutina.setText("Día de descanso");
            detalleRutina.setText("Sin rutina programada para hoy. Aprovecha para recuperar.");
            etiquetaEstadoDia.setText("Día de recuperación");
        }
    }

    private void actualizarProteina(DiaSalud dia, ObjetivosNutricionales objetivos) {
        double total = dia.getTotalProteina();
        double meta = objetivos.getProteinaGramos();
        valorProteina.setText(Formato.numero(total) + " g");
        detalleProteina.setText("Meta " + Formato.numero(meta) + " g · faltan "
                + Formato.numero(Math.max(0, meta - total)) + " g");
        pintar(barraProteina, fraccion(total, meta));
    }

    private void actualizarCalorias(DiaSalud dia, ObjetivosNutricionales objetivos) {
        double total = dia.getTotalCalorias();
        double meta = objetivos.getCalorias();
        valorCalorias.setText(Formato.numero(total) + " kcal");
        detalleCalorias.setText("Meta " + Formato.numero(meta) + " kcal · "
                + Formato.numero(Math.max(0, meta - total)) + " restantes");
        pintar(barraCalorias, fraccion(total, meta));
    }

    private void actualizarHidratacion(DiaSalud dia, ObjetivosNutricionales objetivos) {
        int total = dia.getTotalHidratacionMl();
        int meta = objetivos.getAguaMililitros();
        valorHidratacion.setText(total + " ml");
        detalleHidratacion.setText("Meta " + meta + " ml · " + (total * 100 / Math.max(1, meta)) + "%");
        pintar(barraHidratacion, fraccion(total, meta));
    }

    private void actualizarCreatina(DiaSalud dia, ObjetivosNutricionales objetivos) {
        boolean tomada = dia.isCreatinaTomada();
        valorCreatina.setText(Formato.numero(objetivos.getCreatinaGramos()) + " g");
        detalleCreatina.setText(tomada ? "Dosis registrada hoy" : "Pendiente de tomar");
        pintar(barraCreatina, tomada ? 1 : 0);
    }

    private void actualizarSueno(DiaSalud dia, ObjetivosNutricionales objetivos) {
        double horas = dia.getHorasSueno();
        double meta = objetivos.getSuenoMinimoHoras();
        valorSueno.setText(Formato.numero(horas) + " h");
        if (horas <= 0) {
            detalleSueno.setText("Objetivo " + objetivos.getSuenoTexto() + " · sin registrar");
        } else if (horas < meta) {
            detalleSueno.setText("Objetivo " + objetivos.getSuenoTexto() + " · falta descanso");
        } else {
            detalleSueno.setText("Objetivo " + objetivos.getSuenoTexto() + " · cumplido");
        }
        pintar(barraSueno, fraccion(horas, meta));
    }

    // ------------------------------------------------------------------
    // Recomendaciones: cuadricula de tarjetas de consejos interactivas
    // ------------------------------------------------------------------

    private void actualizarRecomendaciones(DiaSalud dia, ObjetivosNutricionales objetivos, LocalDate hoy) {
        List<Consejo> consejos = new ArrayList<>();

        // Consejos dinamicos segun el estado real del dia (prioridad alta).
        if (dia.getTotalProteina() < objetivos.getProteinaGramos()) {
            consejos.add(new Consejo(TipoConsejo.PROTEINA, "Reparte la proteína",
                    "3-4 comidas de 30-45 g maximizan la síntesis proteica muscular a lo largo del día.",
                    "Hoy te faltan " + Formato.numero(Math.max(0, objetivos.getProteinaGramos() - dia.getTotalProteina()))
                            + " g para tu meta."));
        }
        if (!dia.isCreatinaTomada()) {
            consejos.add(new Consejo(TipoConsejo.CREATINA, "Creatina diaria",
                    "5 g de creatina monohidrato son más efectivos con constancia diaria que con dosis puntuales.",
                    "El momento del día importa poco: lo que cuenta es no fallar la toma."));
        }
        if (dia.getTotalHidratacionMl() < objetivos.getAguaMililitros()) {
            consejos.add(new Consejo(TipoConsejo.HIDRATACION, "Hidratación fraccionada",
                    "Bebe a lo largo del día y añade 500-750 ml extra en los días de entrenamiento.",
                    "Te faltan " + Math.max(0, objetivos.getAguaMililitros() - dia.getTotalHidratacionMl())
                            + " ml para tu objetivo."));
        }
        if (dia.getHorasSueno() > 0 && dia.getHorasSueno() < objetivos.getSuenoMinimoHoras()) {
            consejos.add(new Consejo(TipoConsejo.SUENO, "Prioriza el descanso",
                    "Dormir menos de 7 h reduce la recuperación y la fuerza; estabiliza tu rutina de sueño.",
                    "Objetivo actual: " + objetivos.getSuenoTexto() + "."));
        }
        if (gestor.rutinaDe(hoy).isEmpty()) {
            consejos.add(new Consejo(TipoConsejo.RECUPERACION, "Recuperación activa",
                    "Sin fuerza hoy: caminata suave, movilidad o descanso activo favorecen la recuperación.",
                    "El crecimiento ocurre en la recuperación, no durante la sesión."));
        }

        // Principios de entrenamiento basados en evidencia (siempre visibles).
        consejos.add(new Consejo(TipoConsejo.SOBRECARGA, "Sobrecarga progresiva",
                "Cuando alcances el rango alto de repeticiones con el RIR objetivo, sube ligeramente la carga.",
                "Subidas del 2,5-5 % en ejercicios básicos mantienen la progresión sin sacrificar la técnica."));
        consejos.add(new Consejo(TipoConsejo.RIR, "Proximidad al fallo (RIR)",
                "Entrena la mayoría de series con RIR 1-2: cerca del fallo, sin llegar a él, para acumular volumen de calidad.",
                "Las series efectivas cercanas al fallo son el principal motor de hipertrofia."));
        consejos.add(new Consejo(TipoConsejo.REGISTRO, "Documenta tu progresión",
                "Anota peso y repeticiones de cada serie: la progresión registrada garantiza resultados a medio plazo.",
                "Ninguna serie se recuerda mejor que un registro objetivo."));
        consejos.add(new Consejo(TipoConsejo.RECUPERACION, "Recuperación de 48 h",
                "Deja al menos 48 h entre sesiones que trabajen el mismo grupo muscular.",
                "Programar los grupos con antelación evita solapamientos de fatiga."));

        rejillaConsejos.getChildren().clear();
        int indice = 0;
        for (Consejo consejo : consejos) {
            TarjetaConsejo tarjeta = new TarjetaConsejo(consejo);
            rejillaConsejos.getChildren().add(tarjeta);
            animarEntrada(tarjeta, indice++);
        }
    }

    /** Aparicion escalonada de cada tarjeta para dar sensacion de orden y fluidez. */
    private static void animarEntrada(Node nodo, int indice) {
        nodo.setOpacity(0);
        nodo.setTranslateY(12);
        Timeline entrada = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(nodo.opacityProperty(), 0),
                        new KeyValue(nodo.translateYProperty(), 12)),
                new KeyFrame(Duration.millis(300 + indice * 70L),
                        new KeyValue(nodo.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(nodo.translateYProperty(), 0, Interpolator.EASE_OUT)));
        entrada.play();
    }

    private static double fraccion(double valor, double meta) {
        if (meta <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, valor / meta));
    }

    private static void pintar(ProgressBar barra, double fraccion) {
        barra.setProgress(fraccion);
        barra.getStyleClass().removeAll(CLASE_VERDE, CLASE_AZUL, CLASE_COMPLETO);
        if (fraccion >= 0.999) {
            barra.getStyleClass().add(CLASE_COMPLETO);
        } else if (fraccion >= 0.5) {
            barra.getStyleClass().add(CLASE_VERDE);
        } else {
            barra.getStyleClass().add(CLASE_AZUL);
        }
    }

    // ------------------------------------------------------------------
    // Modelo interno del consejo y su tarjeta
    // ------------------------------------------------------------------

    /** Familia de un consejo: define su icono, color y animacion. */
    private enum TipoConsejo {
        SOBRECARGA, RIR, SUENO, HIDRATACION, PROTEINA, CREATINA, REGISTRO, RECUPERACION
    }

    /** Contenido de un consejo: categoria, titulo breve, texto y detalle ampliado. */
    private record Consejo(TipoConsejo tipo, String titulo, String texto, String detalle) {
    }

    /** Par de nodo grafico y animacion asociada del icono de un consejo. */
    private record Icono(Node nodo, Animation animacion) {
    }

    /**
     * Tarjeta de un consejo interactivo: icono ilustrativo animado, titulo, texto y un
     * detalle que se despliega al tocar la tarjeta.
     */
    private static final class TarjetaConsejo extends VBox {

        private final Label detalle = new Label();
        private boolean abierto;

        TarjetaConsejo(Consejo consejo) {
            getStyleClass().add("tarjeta-consejo");
            setSpacing(8);
            setPrefWidth(ANCHO_TARJETA);
            setMinWidth(ANCHO_TARJETA - 40);

            Icono icono = crearIcono(consejo.tipo());
            StackPane marco = new StackPane(icono.nodo());
            marco.getStyleClass().add("consejo-icono-marco");
            marco.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

            Label titulo = new Label(consejo.titulo());
            titulo.getStyleClass().add("consejo-titulo");
            titulo.setWrapText(true);

            Label texto = new Label(consejo.texto());
            texto.getStyleClass().add("consejo-texto");
            texto.setWrapText(true);

            detalle.setText(consejo.detalle());
            detalle.getStyleClass().add("consejo-detalle");
            detalle.setWrapText(true);
            detalle.setVisible(false);
            detalle.setManaged(false);

            HBox cabecera = new HBox(10, marco, titulo);
            cabecera.setAlignment(Pos.CENTER_LEFT);

            getChildren().addAll(cabecera, texto, detalle);
            setOnMouseClicked(evento -> alternarDetalle());

            icono.animacion().play();
        }

        /** Muestra u oculta el detalle ampliado al tocar la tarjeta. */
        private void alternarDetalle() {
            abierto = !abierto;
            detalle.setVisible(abierto);
            detalle.setManaged(abierto);
            getStyleClass().remove("consejo-abierto");
            if (abierto) {
                getStyleClass().add("consejo-abierto");
            }
        }
    }

    // ------------------------------------------------------------------
    // Iconos vectoriales animados
    // ------------------------------------------------------------------

    /** Construye el icono del consejo junto a la animacion que lo mantiene vivo. */
    private static Icono crearIcono(TipoConsejo tipo) {
        if (tipo == TipoConsejo.RIR) {
            return iconoEsfuerzo();
        }
        String svg = switch (tipo) {
            case SOBRECARGA -> SVG_SOBRECARGA;
            case SUENO -> SVG_SUENO;
            case HIDRATACION -> SVG_AGUA;
            case PROTEINA -> SVG_PROTEINA;
            case CREATINA -> SVG_CREATINA;
            case REGISTRO -> SVG_REGISTRO;
            case RECUPERACION -> SVG_RECUPERACION;
            case RIR -> SVG_SOBRECARGA;
        };
        String color = switch (tipo) {
            case SOBRECARGA, HIDRATACION -> "icono-consejo-cian";
            case PROTEINA, RECUPERACION -> "icono-consejo-menta";
            case SUENO -> "icono-consejo-morado";
            case CREATINA -> "icono-consejo-teal";
            default -> "icono-consejo-acento";
        };

        SVGPath ruta = silueta(svg, color);
        return new Icono(contenedor(ruta), animacionSegunTipo(tipo, ruta));
    }

    /** Indicador de esfuerzo (RIR): arco con aguja que oscila como una balanza. */
    private static Icono iconoEsfuerzo() {
        SVGPath arco = new SVGPath();
        arco.setContent("M7,31 A13,13 0 0 1 33,31");
        arco.getStyleClass().add("icono-consejo-arco");

        SVGPath aguja = new SVGPath();
        aguja.setContent("M20,31 L30,20");
        aguja.getStyleClass().add("icono-consejo-aguja");
        Rotate pivote = new Rotate(0, 20, 31);
        aguja.getTransforms().add(pivote);

        Timeline giro = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(pivote.angleProperty(), -18, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(1100), new KeyValue(pivote.angleProperty(), 18, Interpolator.EASE_BOTH)));
        giro.setAutoReverse(true);
        giro.setCycleCount(Animation.INDEFINITE);
        return new Icono(contenedor(arco, aguja), giro);
    }

    /** Animacion sutil y continua propia de cada familia de consejo. */
    private static Animation animacionSegunTipo(TipoConsejo tipo, SVGPath ruta) {
        switch (tipo) {
            case HIDRATACION, REGISTRO: {
                TranslateTransition flotar = new TranslateTransition(Duration.millis(1500), ruta);
                flotar.setFromY(0);
                flotar.setToY(-4);
                flotar.setInterpolator(Interpolator.EASE_BOTH);
                flotar.setAutoReverse(true);
                flotar.setCycleCount(Animation.INDEFINITE);
                return flotar;
            }
            case SUENO: {
                FadeTransition brillo = new FadeTransition(Duration.millis(1600), ruta);
                brillo.setFromValue(0.7);
                brillo.setToValue(1);
                brillo.setInterpolator(Interpolator.EASE_BOTH);
                brillo.setAutoReverse(true);
                brillo.setCycleCount(Animation.INDEFINITE);
                return brillo;
            }
            default: {
                ScaleTransition latido = new ScaleTransition(Duration.millis(1000), ruta);
                latido.setFromX(1);
                latido.setFromY(1);
                latido.setToX(1.08);
                latido.setToY(1.08);
                latido.setInterpolator(Interpolator.EASE_BOTH);
                latido.setAutoReverse(true);
                latido.setCycleCount(Animation.INDEFINITE);
                return latido;
            }
        }
    }

    private static SVGPath silueta(String contenido, String claseColor) {
        SVGPath ruta = new SVGPath();
        ruta.setContent(contenido);
        ruta.getStyleClass().add(claseColor);
        return ruta;
    }

    /** Caja de tamano fijo para que todos los iconos ocupen el mismo espacio. */
    private static Pane contenedor(Node... hijos) {
        Pane lienzo = new Pane(hijos);
        lienzo.setPrefSize(LADO_ICONO, LADO_ICONO);
        lienzo.setMinSize(LADO_ICONO, LADO_ICONO);
        lienzo.setMaxSize(LADO_ICONO, LADO_ICONO);
        return lienzo;
    }
}
