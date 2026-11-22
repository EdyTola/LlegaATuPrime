package com.bey.desktop.controller;

import com.bey.desktop.model.beyDiaSalud;
import com.bey.desktop.model.beyMetaXp;
import com.bey.desktop.model.beyObjetivosNutricionales;
import com.bey.desktop.model.beyPerfilUsuario;
import com.bey.desktop.model.beyRetoDiario;
import com.bey.desktop.model.beyRutina;
import com.bey.desktop.service.beyGamificacionService;
import com.bey.desktop.service.beyGamificacionService.beyEstadoMeta;
import com.bey.desktop.service.beyGamificacionService.beyInfoNivel;
import com.bey.desktop.service.beyGestorDatos;
import com.bey.desktop.service.beyQuoteService;
import com.bey.desktop.util.beyAchievementVFX;
import javafx.application.Platform;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import com.bey.desktop.service.beyQuoteService.beyFrase;
import com.bey.desktop.util.beyFormato;
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

public class beyDashboardController implements Initializable, beyVistaConRefresco {
    private static final String CLASE_VERDE = "progreso-verde";
    private static final String CLASE_AZUL = "progreso-azul";
    private static final String CLASE_COMPLETO = "progreso-completo";

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

    private final beyGestorDatos beyGestor = beyGestorDatos.beyGetInstancia();

    @FXML
    private Label beyEtiquetaSaludo;
    @FXML
    private Label beyEtiquetaFecha;
    @FXML
    private Label beyEtiquetaEstadoDia;
    @FXML
    private Label beyValorRutina;
    @FXML
    private Label beyDetalleRutina;
    @FXML
    private Label beyValorProteina;
    @FXML
    private ProgressBar beyBarraProteina;
    @FXML
    private Label beyDetalleProteina;
    @FXML
    private Label beyValorCalorias;
    @FXML
    private ProgressBar beyBarraCalorias;
    @FXML
    private Label beyDetalleCalorias;
    @FXML
    private Label beyValorHidratacion;
    @FXML
    private ProgressBar beyBarraHidratacion;
    @FXML
    private Label beyDetalleHidratacion;
    @FXML
    private Label beyValorCreatina;
    @FXML
    private ProgressBar beyBarraCreatina;
    @FXML
    private Label beyDetalleCreatina;
    @FXML
    private Label beyValorSueno;
    @FXML
    private ProgressBar beyBarraSueno;
    @FXML
    private Label beyDetalleSueno;
    @FXML
    private FlowPane beyRejillaConsejos;
    @FXML
    private Label beyEtiquetaFrase;
    @FXML
    private Label beyEtiquetaAutorFrase;
    @FXML
    private Label beyEtiquetaNivelNumero;
    @FXML
    private Label beyEtiquetaNivelTitulo;
    @FXML
    private ProgressBar beyBarraXp;
    @FXML
    private Label beyEtiquetaXp;
    @FXML
    private Label beyEtiquetaXpResumen;
    @FXML
    private VBox beyListaMetas;
    @FXML
    private FlowPane beyRejillaRetos;
    @FXML
    private Label beyEtiquetaRetosEstado;

    private Timeline beyRelojRpg;

    private final Map<beyMetaXp, HBox> beyFilasMeta = new EnumMap<>(beyMetaXp.class);
    private final Map<String, VBox> beyTarjetasReto = new HashMap<>();

    private final Map<beyMetaXp, Integer> beyMetasNuevas = new EnumMap<>(beyMetaXp.class);
    private final Map<String, Integer> beyRetosNuevos = new HashMap<>();
    private boolean beyEscuchaRegistrada;

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        beyRefrescar();
    }

    @Override
    public void beyRefrescar() {
        LocalDate hoy = LocalDate.now();
        beyPerfilUsuario perfil = beyGestor.beyGetDatos().getPerfil();
        beyObjetivosNutricionales objetivos = perfil.getObjetivos();
        beyDiaSalud dia = beyGestor.beyGetDatos().buscarDia(hoy).orElseGet(() -> new beyDiaSalud(hoy));

        beyEtiquetaSaludo.setText(beySaludo() + ", " + perfil.getPrimerNombre());
        beyEtiquetaFecha.setText(beyFormato.beyFechaLarga(hoy));

        beyActualizarRutina(hoy);
        beyActualizarProteina(dia, objetivos);
        beyActualizarCalorias(dia, objetivos);
        beyActualizarHidratacion(dia, objetivos);
        beyActualizarCreatina(dia, objetivos);
        beyActualizarSueno(dia, objetivos);
        beyActualizarRecomendaciones(dia, objetivos, hoy);
        beyMostrarFrase(beyQuoteService.beyFraseDelDia(perfil, hoy));
        beyActualizarRpg();
        beyIniciarRelojRpg();
    }

    private void beyMostrarFrase(beyFrase frase) {
        beyEtiquetaFrase.setText(frase.texto());
        beyEtiquetaAutorFrase.setText("\u2014 " + frase.autor());
        beyEtiquetaAutorFrase.getStyleClass().remove("frase-autor-personal");
        if (frase.personal()) {
            beyEtiquetaAutorFrase.getStyleClass().add("frase-autor-personal");
        }
    }

    @FXML
    private void beyOtraFrase() {
        beyMostrarFrase(beyQuoteService.beySiguiente(beyGestor.beyGetDatos().getPerfil(), LocalDate.now()));
    }

    private void beyRegistrarEscuchaVfx() {
        if (beyEscuchaRegistrada) {
            return;
        }
        beyEscuchaRegistrada = true;
        beyGestor.beyGetGamificacion().beyAgregarEscucha(new beyGamificacionService.beyEscucha() {
            @Override
            public void beyMetaCumplida(beyMetaXp meta, int xp) {
                beyMetasNuevas.put(meta, xp);
            }

            @Override
            public void beyRetoCompletado(beyRetoDiario reto) {
                beyRetosNuevos.put(reto.getId(), reto.getXpRecompensa());
            }
        });
    }

    private void beyIniciarRelojRpg() {
        if (beyRelojRpg != null) {
            return;
        }
        beyRelojRpg = new Timeline(new KeyFrame(Duration.minutes(1), evento -> {
            if (beyRejillaRetos.getScene() == null) {
                beyRelojRpg.stop();
                beyRelojRpg = null;
                return;
            }
            beyActualizarRpg();
        }));
        beyRelojRpg.setCycleCount(Animation.INDEFINITE);
        beyRelojRpg.play();
    }

    private void beyActualizarRpg() {
        beyGamificacionService rpg = beyGestor.beyGetGamificacion();
        beyRegistrarEscuchaVfx();
        beyMetasNuevas.clear();
        beyRetosNuevos.clear();
        rpg.beyEvaluar();

        beyInfoNivel nivel = rpg.beyInfoNivel();
        beyEtiquetaNivelNumero.setText(String.valueOf(nivel.nivel()));
        beyEtiquetaNivelTitulo.setText("Nivel " + nivel.nivel() + " · " + nivel.titulo());
        beyBarraXp.setProgress(nivel.fraccion());
        beyEtiquetaXp.setText(nivel.xpEnNivel() + " / " + nivel.xpParaSiguiente() + " XP para el nivel "
                + (nivel.nivel() + 1) + " (" + beyGamificacionService.beyTitulo(nivel.nivel() + 1) + ")");
        beyEtiquetaXpResumen.setText(nivel.xpTotal() + " XP totales · "
                + beyGestor.beyGetDatos().getProgresoRpg().getRetosCompletados() + " retos completados");

        beyListaMetas.getChildren().clear();
        beyFilasMeta.clear();
        for (beyEstadoMeta estado : rpg.beyMetasDeHoy()) {
            HBox fila = beyFilaMeta(estado);
            beyFilasMeta.put(estado.meta(), fila);
            beyListaMetas.getChildren().add(fila);
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<beyRetoDiario> retos = rpg.beyRetosVigentes();
        beyRejillaRetos.getChildren().clear();
        beyTarjetasReto.clear();
        for (beyRetoDiario reto : retos) {
            VBox tarjeta = beyTarjetaReto(reto, ahora);
            beyTarjetasReto.put(reto.getId(), tarjeta);
            beyRejillaRetos.getChildren().add(tarjeta);
        }
        beyCelebrarLogrosNuevos();
        long completados = retos.stream().filter(beyRetoDiario::isCompletado).count();
        beyEtiquetaRetosEstado.setText(completados + " de " + retos.size() + " completados");
    }

    private void beyCelebrarLogrosNuevos() {
        if (beyMetasNuevas.isEmpty() && beyRetosNuevos.isEmpty()) {
            return;
        }
        Map<beyMetaXp, Integer> metas = new EnumMap<>(beyMetasNuevas);
        Map<String, Integer> retos = new HashMap<>(beyRetosNuevos);

        Platform.runLater(() -> {
            metas.forEach((meta, xp) -> {
                HBox fila = beyFilasMeta.get(meta);
                if (fila != null && fila.getScene() != null) {
                    beyAchievementVFX.beyCelebrarMeta(fila, "+" + xp + " XP");
                }
            });
            retos.forEach((id, xp) -> {
                VBox tarjeta = beyTarjetasReto.get(id);
                if (tarjeta != null && tarjeta.getScene() != null) {
                    beyAchievementVFX.beyCelebrarMeta(tarjeta, "+" + xp + " XP · ¡Completado!");
                }
            });
        });
    }

    private static HBox beyFilaMeta(beyEstadoMeta estado) {
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

    private VBox beyTarjetaReto(beyRetoDiario reto, LocalDateTime ahora) {
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
                beyGestor.beyGetGamificacion().beyCompletarReto(reto.getId());
                beyActualizarRpg();
            });
            tarjeta.getChildren().add(confirmar);
        }
        tarjeta.setPrefWidth(ANCHO_TARJETA);
        tarjeta.setMinWidth(ANCHO_TARJETA - 40);
        return tarjeta;
    }

    private String beySaludo() {
        int hora = LocalTime.now().getHour();
        if (hora < 12) {
            return "Buenos días";
        }
        if (hora < 19) {
            return "Buenas tardes";
        }
        return "Buenas noches";
    }

    private void beyActualizarRutina(LocalDate hoy) {
        Optional<beyRutina> rutina = beyGestor.beyRutinaDe(hoy);
        if (rutina.isPresent()) {
            beyRutina r = rutina.get();
            beyValorRutina.setText(r.getNombre());
            beyDetalleRutina.setText(r.getDiaSemana().getEtiqueta() + " · "
                    + r.getEjercicios().size() + " ejercicios · " + r.getSeriesTotales() + " series");
            beyEtiquetaEstadoDia.setText("Día de entrenamiento");
        } else {
            beyValorRutina.setText("Día de descanso");
            beyDetalleRutina.setText("Sin rutina programada para hoy. Aprovecha para recuperar.");
            beyEtiquetaEstadoDia.setText("Día de recuperación");
        }
    }

    private void beyActualizarProteina(beyDiaSalud dia, beyObjetivosNutricionales objetivos) {
        double total = dia.getTotalProteina();
        double meta = objetivos.getProteinaGramos();
        beyValorProteina.setText(beyFormato.beyNumero(total) + " g");
        beyDetalleProteina.setText("Meta " + beyFormato.beyNumero(meta) + " g · faltan "
                + beyFormato.beyNumero(Math.max(0, meta - total)) + " g");
        beyPintar(beyBarraProteina, beyFraccion(total, meta));
    }

    private void beyActualizarCalorias(beyDiaSalud dia, beyObjetivosNutricionales objetivos) {
        double total = dia.getTotalCalorias();
        double meta = objetivos.getCalorias();
        beyValorCalorias.setText(beyFormato.beyNumero(total) + " kcal");
        beyDetalleCalorias.setText("Meta " + beyFormato.beyNumero(meta) + " kcal · "
                + beyFormato.beyNumero(Math.max(0, meta - total)) + " restantes");
        beyPintar(beyBarraCalorias, beyFraccion(total, meta));
    }

    private void beyActualizarHidratacion(beyDiaSalud dia, beyObjetivosNutricionales objetivos) {
        int total = dia.getTotalHidratacionMl();
        int meta = objetivos.getAguaMililitros();
        beyValorHidratacion.setText(total + " ml");
        beyDetalleHidratacion.setText("Meta " + meta + " ml · " + (total * 100 / Math.max(1, meta)) + "%");
        beyPintar(beyBarraHidratacion, beyFraccion(total, meta));
    }

    private void beyActualizarCreatina(beyDiaSalud dia, beyObjetivosNutricionales objetivos) {
        boolean tomada = dia.isCreatinaTomada();
        beyValorCreatina.setText(beyFormato.beyNumero(objetivos.getCreatinaGramos()) + " g");
        beyDetalleCreatina.setText(tomada ? "Dosis registrada hoy" : "Pendiente de tomar");
        beyPintar(beyBarraCreatina, tomada ? 1 : 0);
    }

    private void beyActualizarSueno(beyDiaSalud dia, beyObjetivosNutricionales objetivos) {
        double horas = dia.getHorasSueno();
        double meta = objetivos.getSuenoMinimoHoras();
        beyValorSueno.setText(beyFormato.beyNumero(horas) + " h");
        if (horas <= 0) {
            beyDetalleSueno.setText("Objetivo " + objetivos.getSuenoTexto() + " · sin registrar");
        } else if (horas < meta) {
            beyDetalleSueno.setText("Objetivo " + objetivos.getSuenoTexto() + " · falta descanso");
        } else {
            beyDetalleSueno.setText("Objetivo " + objetivos.getSuenoTexto() + " · cumplido");
        }
        beyPintar(beyBarraSueno, beyFraccion(horas, meta));
    }

    private void beyActualizarRecomendaciones(beyDiaSalud dia, beyObjetivosNutricionales objetivos, LocalDate hoy) {
        List<beyConsejo> consejos = new ArrayList<>();

        if (dia.getTotalProteina() < objetivos.getProteinaGramos()) {
            consejos.add(new beyConsejo(beyTipoConsejo.PROTEINA, "Reparte la proteína",
                    "3-4 comidas de 30-45 g maximizan la síntesis proteica muscular a lo largo del día.",
                    "Hoy te faltan " + beyFormato.beyNumero(Math.max(0, objetivos.getProteinaGramos() - dia.getTotalProteina()))
                            + " g para tu meta."));
        }
        if (!dia.isCreatinaTomada()) {
            consejos.add(new beyConsejo(beyTipoConsejo.CREATINA, "Creatina diaria",
                    "5 g de creatina monohidrato son más efectivos con constancia diaria que con dosis puntuales.",
                    "El momento del día importa poco: lo que cuenta es no fallar la toma."));
        }
        if (dia.getTotalHidratacionMl() < objetivos.getAguaMililitros()) {
            consejos.add(new beyConsejo(beyTipoConsejo.HIDRATACION, "Hidratación fraccionada",
                    "Bebe a lo largo del día y añade 500-750 ml extra en los días de entrenamiento.",
                    "Te faltan " + Math.max(0, objetivos.getAguaMililitros() - dia.getTotalHidratacionMl())
                            + " ml para tu objetivo."));
        }
        if (dia.getHorasSueno() > 0 && dia.getHorasSueno() < objetivos.getSuenoMinimoHoras()) {
            consejos.add(new beyConsejo(beyTipoConsejo.SUENO, "Prioriza el descanso",
                    "Dormir menos de 7 h reduce la recuperación y la fuerza; estabiliza tu rutina de sueño.",
                    "Objetivo actual: " + objetivos.getSuenoTexto() + "."));
        }
        if (beyGestor.beyRutinaDe(hoy).isEmpty()) {
            consejos.add(new beyConsejo(beyTipoConsejo.RECUPERACION, "Recuperación activa",
                    "Sin fuerza hoy: caminata suave, movilidad o descanso activo favorecen la recuperación.",
                    "El crecimiento ocurre en la recuperación, no durante la sesión."));
        }

        consejos.add(new beyConsejo(beyTipoConsejo.SOBRECARGA, "Sobrecarga progresiva",
                "Cuando alcances el rango alto de repeticiones con el RIR objetivo, sube ligeramente la carga.",
                "Subidas del 2,5-5 % en ejercicios básicos mantienen la progresión sin sacrificar la técnica."));
        consejos.add(new beyConsejo(beyTipoConsejo.RIR, "Proximidad al fallo (RIR)",
                "Entrena la mayoría de series con RIR 1-2: cerca del fallo, sin llegar a él, para acumular volumen de calidad.",
                "Las series efectivas cercanas al fallo son el principal motor de hipertrofia."));
        consejos.add(new beyConsejo(beyTipoConsejo.REGISTRO, "Documenta tu progresión",
                "Anota peso y repeticiones de cada serie: la progresión registrada garantiza resultados a medio plazo.",
                "Ninguna serie se recuerda mejor que un registro objetivo."));
        consejos.add(new beyConsejo(beyTipoConsejo.RECUPERACION, "Recuperación de 48 h",
                "Deja al menos 48 h entre sesiones que trabajen el mismo grupo muscular.",
                "Programar los grupos con antelación evita solapamientos de fatiga."));

        beyRejillaConsejos.getChildren().clear();
        int indice = 0;
        for (beyConsejo consejo : consejos) {
            beyTarjetaConsejo tarjeta = new beyTarjetaConsejo(consejo);
            beyRejillaConsejos.getChildren().add(tarjeta);
            beyAnimarEntrada(tarjeta, indice++);
        }
    }

    private static void beyAnimarEntrada(Node nodo, int indice) {
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

    private static double beyFraccion(double valor, double meta) {
        if (meta <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, valor / meta));
    }

    private static void beyPintar(ProgressBar barra, double fraccion) {
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

    private enum beyTipoConsejo {
        SOBRECARGA, RIR, SUENO, HIDRATACION, PROTEINA, CREATINA, REGISTRO, RECUPERACION
    }

    private record beyConsejo(beyTipoConsejo tipo, String titulo, String texto, String detalle) {
    }

    private record beyIcono(Node nodo, Animation animacion) {
    }

    private static final class beyTarjetaConsejo extends VBox {
        private final Label beyDetalle = new Label();
        private boolean beyAbierto;

        beyTarjetaConsejo(beyConsejo consejo) {
            getStyleClass().add("tarjeta-consejo");
            setSpacing(8);
            setPrefWidth(ANCHO_TARJETA);
            setMinWidth(ANCHO_TARJETA - 40);

            beyIcono icono = beyCrearIcono(consejo.tipo());
            StackPane marco = new StackPane(icono.nodo());
            marco.getStyleClass().add("consejo-icono-marco");
            marco.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

            Label titulo = new Label(consejo.titulo());
            titulo.getStyleClass().add("consejo-titulo");
            titulo.setWrapText(true);

            Label texto = new Label(consejo.texto());
            texto.getStyleClass().add("consejo-texto");
            texto.setWrapText(true);

            beyDetalle.setText(consejo.detalle());
            beyDetalle.getStyleClass().add("consejo-detalle");
            beyDetalle.setWrapText(true);
            beyDetalle.setVisible(false);
            beyDetalle.setManaged(false);

            HBox cabecera = new HBox(10, marco, titulo);
            cabecera.setAlignment(Pos.CENTER_LEFT);

            getChildren().addAll(cabecera, texto, beyDetalle);
            setOnMouseClicked(evento -> beyAlternarDetalle());

            icono.animacion().play();
        }

        private void beyAlternarDetalle() {
            beyAbierto = !beyAbierto;
            beyDetalle.setVisible(beyAbierto);
            beyDetalle.setManaged(beyAbierto);
            getStyleClass().remove("consejo-abierto");
            if (beyAbierto) {
                getStyleClass().add("consejo-abierto");
            }
        }
    }

    private static beyIcono beyCrearIcono(beyTipoConsejo tipo) {
        if (tipo == beyTipoConsejo.RIR) {
            return beyIconoEsfuerzo();
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

        SVGPath ruta = beySilueta(svg, color);
        return new beyIcono(beyContenedor(ruta), beyAnimacionSegunTipo(tipo, ruta));
    }

    private static beyIcono beyIconoEsfuerzo() {
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
        return new beyIcono(beyContenedor(arco, aguja), giro);
    }

    private static Animation beyAnimacionSegunTipo(beyTipoConsejo tipo, SVGPath ruta) {
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

    private static SVGPath beySilueta(String contenido, String claseColor) {
        SVGPath ruta = new SVGPath();
        ruta.setContent(contenido);
        ruta.getStyleClass().add(claseColor);
        return ruta;
    }

    private static Pane beyContenedor(Node... hijos) {
        Pane lienzo = new Pane(hijos);
        lienzo.setPrefSize(LADO_ICONO, LADO_ICONO);
        lienzo.setMinSize(LADO_ICONO, LADO_ICONO);
        lienzo.setMaxSize(LADO_ICONO, LADO_ICONO);
        return lienzo;
    }
}
