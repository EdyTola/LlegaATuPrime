package com.symmetry.desktop.service;

import com.symmetry.desktop.model.BaseDatos;
import com.symmetry.desktop.model.DiaSalud;
import com.symmetry.desktop.model.MetaXp;
import com.symmetry.desktop.model.ObjetivosNutricionales;
import com.symmetry.desktop.model.ProgresoRpg;
import com.symmetry.desktop.model.RetoDiario;
import com.symmetry.desktop.model.SesionEntrenamiento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Sistema de experiencia y niveles (RPG). Reglas:
 * <ul>
 *   <li>Cada meta diaria otorga XP <b>una sola vez por dia</b> (aunque se borre y se vuelva a
 *       registrar el dato), por lo que no se puede "farmear" experiencia.</li>
 *   <li>Los retos dinamicos otorgan XP extra al completarse dentro de su ventana.</li>
 *   <li>XP acumulada para alcanzar el nivel N = 50 * N * (N - 1): nivel 2 a los 100 XP,
 *       nivel 5 a los 1000 XP y nivel 10 a los 4500 XP.</li>
 * </ul>
 * {@link #evaluar()} es idempotente y se invoca tras cada cambio de datos relevante.
 */
public class GamificacionService {

    /** Foto del nivel del usuario: nivel, titulo y avance dentro del nivel actual. */
    public record InfoNivel(int nivel, String titulo, int xpTotal, int xpEnNivel, int xpParaSiguiente) {

        public double fraccion() {
            return xpParaSiguiente <= 0 ? 0 : Math.min(1.0, (double) xpEnNivel / xpParaSiguiente);
        }
    }

    /** Estado de una meta diaria para mostrarla en la interfaz. */
    public record EstadoMeta(MetaXp meta, boolean cumplida, boolean reclamada) {
    }

    /** Observador de eventos de gamificacion (todos se emiten en el hilo de la interfaz). */
    public interface Escucha {

        default void xpGanada(int xp, String motivo) {
        }

        default void retoCompletado(RetoDiario reto) {
        }

        /** Una meta diaria acaba de cumplirse por primera vez hoy (otorgo {@code xp}). */
        default void metaCumplida(MetaXp meta, int xp) {
        }

        default void nivelSubido(InfoNivel nivel) {
        }
    }

    private record Premio(int xp, String motivo, RetoDiario reto, MetaXp meta) {
    }

    private final GestorDatos gestor;
    private final List<Escucha> escuchas = new ArrayList<>();
    private boolean evaluando;

    public GamificacionService(GestorDatos gestor) {
        this.gestor = gestor;
    }

    public void quitarEscucha(Escucha escucha) {
        escuchas.remove(escucha);
    }

    public void agregarEscucha(Escucha escucha) {
        if (escucha != null && !escuchas.contains(escucha)) {
            escuchas.add(escucha);
        }
    }

    // ------------------------------------------------------------------
    // Niveles
    // ------------------------------------------------------------------

    /** XP total necesaria para alcanzar el nivel indicado. */
    public static int xpParaNivel(int nivel) {
        return 50 * nivel * (nivel - 1);
    }

    public static int nivelParaXp(int xp) {
        int nivel = (int) ((1 + Math.sqrt(1 + 4.0 * Math.max(0, xp) / 50.0)) / 2);
        nivel = Math.max(1, nivel);
        while (xp >= xpParaNivel(nivel + 1)) {
            nivel++;
        }
        while (nivel > 1 && xp < xpParaNivel(nivel)) {
            nivel--;
        }
        return nivel;
    }

    /** Titulo asociado al nivel (se desbloquea al llegar al nivel indicado). */
    public static String titulo(int nivel) {
        if (nivel >= 30) {
            return "Leyenda del Prime";
        }
        if (nivel >= 20) {
            return "Élite";
        }
        if (nivel >= 15) {
            return "Imparable";
        }
        if (nivel >= 10) {
            return "En \"El Prime\"";
        }
        if (nivel >= 7) {
            return "Constante";
        }
        if (nivel >= 5) {
            return "Disciplinado";
        }
        if (nivel >= 3) {
            return "Aprendiz";
        }
        return "Novato";
    }

    public static InfoNivel infoPara(int xpTotal) {
        int nivel = nivelParaXp(xpTotal);
        int base = xpParaNivel(nivel);
        return new InfoNivel(nivel, titulo(nivel), xpTotal, xpTotal - base, xpParaNivel(nivel + 1) - base);
    }

    public InfoNivel infoNivel() {
        return infoPara(progreso().getXpTotal());
    }

    // ------------------------------------------------------------------
    // Consulta de estado
    // ------------------------------------------------------------------

    /** Metas del dia de hoy con su estado actual. */
    public List<EstadoMeta> metasDeHoy() {
        LocalDate hoy = LocalDate.now();
        Map<MetaXp, Boolean> cumplidas = estadoMetas(gestor.getDatos(), hoy);
        List<EstadoMeta> lista = new ArrayList<>();
        for (MetaXp meta : MetaXp.values()) {
            lista.add(new EstadoMeta(meta, cumplidas.get(meta), progreso().yaReclamada(hoy, meta)));
        }
        return lista;
    }

    public List<RetoDiario> retosVigentes() {
        return List.copyOf(progreso().getRetos());
    }

    private ProgresoRpg progreso() {
        return gestor.getDatos().getProgresoRpg();
    }

    // ------------------------------------------------------------------
    // Evaluacion
    // ------------------------------------------------------------------

    /**
     * Sincroniza el estado con los datos actuales: renueva los retos vencidos, mide su
     * progreso, otorga la XP de las metas recien cumplidas y notifica a los observadores.
     */
    public void evaluar() {
        if (evaluando) {
            return;
        }
        evaluando = true;
        try {
            BaseDatos datos = gestor.getDatos();
            ProgresoRpg progreso = datos.getProgresoRpg();
            LocalDateTime ahora = LocalDateTime.now();
            LocalDate hoy = ahora.toLocalDate();
            List<Premio> premios = new ArrayList<>();
            boolean cambio = renovarRetos(datos, progreso, ahora);

            Map<MetaXp, Boolean> cumplidas = estadoMetas(datos, hoy);
            for (MetaXp meta : MetaXp.values()) {
                if (cumplidas.get(meta) && !progreso.yaReclamada(hoy, meta)) {
                    progreso.marcarReclamada(hoy, meta);
                    premios.add(new Premio(meta.getXp(), meta.getTitulo(), null, meta));
                    cambio = true;
                }
            }

            for (RetoDiario reto : progreso.getRetos()) {
                if (reto.isCompletado()) {
                    continue;
                }
                int avance = GeneradorRetosService.calcularProgreso(reto, datos);
                if (avance != reto.getProgreso()) {
                    reto.setProgreso(avance);
                    cambio = true;
                }
                if (avance >= reto.getObjetivo()) {
                    reto.setCompletado(true);
                    progreso.setRetosCompletados(progreso.getRetosCompletados() + 1);
                    premios.add(new Premio(reto.getXpRecompensa(), "Reto: " + reto.getTitulo(), reto, null));
                    cambio = true;
                }
            }

            if (premios.isEmpty()) {
                if (cambio) {
                    gestor.guardarAutomatico();
                }
                return;
            }

            int nivelAntes = nivelParaXp(progreso.getXpTotal());
            int total = premios.stream().mapToInt(Premio::xp).sum();
            progreso.setXpTotal(progreso.getXpTotal() + total);
            gestor.guardarAutomatico();

            for (Premio premio : premios) {
                for (Escucha escucha : List.copyOf(escuchas)) {
                    escucha.xpGanada(premio.xp(), premio.motivo());
                    if (premio.reto() != null) {
                        escucha.retoCompletado(premio.reto());
                    }
                    if (premio.meta() != null) {
                        escucha.metaCumplida(premio.meta(), premio.xp());
                    }
                }
            }
            InfoNivel despues = infoNivel();
            if (despues.nivel() > nivelAntes) {
                for (Escucha escucha : List.copyOf(escuchas)) {
                    escucha.nivelSubido(despues);
                }
            }
        } finally {
            evaluando = false;
        }
    }

    /** Elimina los retos vencidos y genera los de la ventana en curso si aun no existen. */
    private boolean renovarRetos(BaseDatos datos, ProgresoRpg progreso, LocalDateTime ahora) {
        boolean cambio = progreso.getRetos().removeIf(r -> r.getExpira() == null || !ahora.isBefore(r.getExpira()));
        // Limpieza del registro de metas muy antiguas para que el archivo no crezca sin limite.
        String limite = ahora.toLocalDate().minusDays(90).toString();
        cambio |= progreso.getMetasReclamadas().keySet().removeIf(fecha -> fecha.compareTo(limite) < 0);

        var modalidad = datos.getConfiguracion().getModalidad();
        String diaria = GeneradorRetosService.idVentanaDiaria(ahora);
        if (progreso.getRetos().stream().noneMatch(r -> diaria.equals(r.getVentanaId()))) {
            progreso.getRetos().addAll(GeneradorRetosService.generarDiarios(datos, modalidad, ahora));
            cambio = true;
        }
        String flash = GeneradorRetosService.idVentanaFlash(ahora);
        if (progreso.getRetos().stream().noneMatch(r -> flash.equals(r.getVentanaId()))) {
            progreso.getRetos().add(GeneradorRetosService.generarFlash(datos, modalidad, ahora));
            cambio = true;
        }
        return cambio;
    }

    /** Evalua, con los datos de la fecha dada, que metas estan cumplidas (sin otorgar nada). */
    private static Map<MetaXp, Boolean> estadoMetas(BaseDatos datos, LocalDate fecha) {
        ObjetivosNutricionales objetivos = datos.getPerfil().getObjetivos();
        DiaSalud dia = datos.buscarDia(fecha).orElse(null);
        Map<MetaXp, Boolean> estado = new EnumMap<>(MetaXp.class);

        estado.put(MetaXp.PROTEINA, dia != null && objetivos.getProteinaGramos() > 0
                && dia.getTotalProteina() >= objetivos.getProteinaGramos());
        estado.put(MetaXp.AGUA, dia != null && objetivos.getAguaMililitros() > 0
                && dia.getTotalHidratacionMl() >= objetivos.getAguaMililitros());
        estado.put(MetaXp.CREATINA, dia != null && dia.isCreatinaTomada());
        estado.put(MetaXp.SUENO, dia != null && dia.getHorasSueno() > 0
                && dia.getHorasSueno() >= objetivos.getSuenoMinimoHoras());
        estado.put(MetaXp.ENTRENAMIENTO, datos.getSesiones().stream().anyMatch(s -> sesionDelDia(s, fecha)));

        boolean perfecto = estado.values().stream().allMatch(Boolean::booleanValue);
        estado.put(MetaXp.DIA_PERFECTO, perfecto);
        return estado;
    }

    private static boolean sesionDelDia(SesionEntrenamiento sesion, LocalDate fecha) {
        if (sesion.getFechaFin() == null || sesion.getSeriesCompletadas() <= 0) {
            return false;
        }
        return fecha.equals(sesion.getFechaFin().toLocalDate());
    }
}
