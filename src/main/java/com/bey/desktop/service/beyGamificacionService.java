package com.bey.desktop.service;

import com.bey.desktop.model.beyBaseDatos;
import com.bey.desktop.model.beyDiaSalud;
import com.bey.desktop.model.beyMetaXp;
import com.bey.desktop.model.beyObjetivosNutricionales;
import com.bey.desktop.model.beyProgresoRpg;
import com.bey.desktop.model.beyRetoDiario;
import com.bey.desktop.model.beySesionEntrenamiento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class beyGamificacionService {
    public record beyInfoNivel(int nivel, String titulo, int xpTotal, int xpEnNivel, int xpParaSiguiente) {
        public double fraccion() {
            return xpParaSiguiente <= 0 ? 0 : Math.min(1.0, (double) xpEnNivel / xpParaSiguiente);
        }
    }

    public record beyEstadoMeta(beyMetaXp meta, boolean cumplida, boolean reclamada) {
    }

    public interface beyEscucha {
        default void beyXpGanada(int xp, String motivo) {
        }

        default void beyRetoCompletado(beyRetoDiario reto) {
        }

        default void beyMetaCumplida(beyMetaXp meta, int xp) {
        }

        default void beyNivelSubido(beyInfoNivel nivel) {
        }
    }

    private record beyPremio(int xp, String motivo, beyRetoDiario reto, beyMetaXp meta) {
    }

    private final beyGestorDatos beyGestor;
    private final List<beyEscucha> beyEscuchas = new ArrayList<>();
    private boolean beyEvaluando;

    public beyGamificacionService(beyGestorDatos gestor) {
        this.beyGestor = gestor;
    }

    public void beyQuitarEscucha(beyEscucha escucha) {
        beyEscuchas.remove(escucha);
    }

    public void beyAgregarEscucha(beyEscucha escucha) {
        if (escucha != null && !beyEscuchas.contains(escucha)) {
            beyEscuchas.add(escucha);
        }
    }

    public static int beyXpParaNivel(int nivel) {
        return 50 * nivel * (nivel - 1);
    }

    public static int beyNivelParaXp(int xp) {
        int nivel = (int) ((1 + Math.sqrt(1 + 4.0 * Math.max(0, xp) / 50.0)) / 2);
        nivel = Math.max(1, nivel);
        while (xp >= beyXpParaNivel(nivel + 1)) {
            nivel++;
        }
        while (nivel > 1 && xp < beyXpParaNivel(nivel)) {
            nivel--;
        }
        return nivel;
    }

    public static String beyTitulo(int nivel) {
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

    public static beyInfoNivel beyInfoPara(int xpTotal) {
        int nivel = beyNivelParaXp(xpTotal);
        int base = beyXpParaNivel(nivel);
        return new beyInfoNivel(nivel, beyTitulo(nivel), xpTotal, xpTotal - base, beyXpParaNivel(nivel + 1) - base);
    }

    public beyInfoNivel beyInfoNivel() {
        return beyInfoPara(beyProgreso().getXpTotal());
    }

    public List<beyEstadoMeta> beyMetasDeHoy() {
        LocalDate hoy = LocalDate.now();
        Map<beyMetaXp, Boolean> cumplidas = beyEstadoMetas(beyGestor.beyGetDatos(), hoy);
        List<beyEstadoMeta> lista = new ArrayList<>();
        for (beyMetaXp meta : beyMetaXp.values()) {
            lista.add(new beyEstadoMeta(meta, cumplidas.get(meta), beyProgreso().yaReclamada(hoy, meta)));
        }
        return lista;
    }

    public List<beyRetoDiario> beyRetosVigentes() {
        return List.copyOf(beyProgreso().getRetos());
    }

    public void beyCompletarReto(String idReto) {
        if (idReto == null) {
            return;
        }
        beyRetoDiario reto = beyProgreso().getRetos().stream()
                .filter(r -> idReto.equals(r.getId()))
                .findFirst()
                .orElse(null);
        if (reto == null || reto.isCompletado() || !reto.esManual()) {
            return;
        }
        if (reto.getExpira() != null && !LocalDateTime.now().isBefore(reto.getExpira())) {
            return;
        }
        reto.setProgreso(reto.getObjetivo());
        beyEvaluar();
    }

    private beyProgresoRpg beyProgreso() {
        return beyGestor.beyGetDatos().getProgresoRpg();
    }

    public void beyEvaluar() {
        if (beyEvaluando) {
            return;
        }
        beyEvaluando = true;
        try {
            beyBaseDatos datos = beyGestor.beyGetDatos();
            beyProgresoRpg progreso = datos.getProgresoRpg();
            LocalDateTime ahora = LocalDateTime.now();
            LocalDate hoy = ahora.toLocalDate();
            List<beyPremio> premios = new ArrayList<>();
            boolean cambio = beyRenovarRetos(datos, progreso, ahora);

            Map<beyMetaXp, Boolean> cumplidas = beyEstadoMetas(datos, hoy);
            for (beyMetaXp meta : beyMetaXp.values()) {
                if (cumplidas.get(meta) && !progreso.yaReclamada(hoy, meta)) {
                    progreso.marcarReclamada(hoy, meta);
                    premios.add(new beyPremio(meta.getXp(), meta.getTitulo(), null, meta));
                    cambio = true;
                }
            }

            for (beyRetoDiario reto : progreso.getRetos()) {
                if (reto.isCompletado()) {
                    continue;
                }
                int avance = beyGeneradorRetosService.beyCalcularProgreso(reto, datos);
                if (avance != reto.getProgreso()) {
                    reto.setProgreso(avance);
                    cambio = true;
                }
                if (avance >= reto.getObjetivo()) {
                    reto.setCompletado(true);
                    progreso.setRetosCompletados(progreso.getRetosCompletados() + 1);
                    premios.add(new beyPremio(reto.getXpRecompensa(), "Reto: " + reto.getTitulo(), reto, null));
                    cambio = true;
                }
            }

            if (premios.isEmpty()) {
                if (cambio) {
                    beyGestor.beyGuardarAutomatico();
                }
                return;
            }

            int nivelAntes = beyNivelParaXp(progreso.getXpTotal());
            int total = premios.stream().mapToInt(beyPremio::xp).sum();
            progreso.setXpTotal(progreso.getXpTotal() + total);
            beyGestor.beyGuardarAutomatico();

            for (beyPremio premio : premios) {
                for (beyEscucha escucha : List.copyOf(beyEscuchas)) {
                    escucha.beyXpGanada(premio.xp(), premio.motivo());
                    if (premio.reto() != null) {
                        escucha.beyRetoCompletado(premio.reto());
                    }
                    if (premio.meta() != null) {
                        escucha.beyMetaCumplida(premio.meta(), premio.xp());
                    }
                }
            }
            beyInfoNivel despues = beyInfoNivel();
            if (despues.nivel() > nivelAntes) {
                for (beyEscucha escucha : List.copyOf(beyEscuchas)) {
                    escucha.beyNivelSubido(despues);
                }
            }
        } finally {
            beyEvaluando = false;
        }
    }

    private boolean beyRenovarRetos(beyBaseDatos datos, beyProgresoRpg progreso, LocalDateTime ahora) {
        boolean cambio = progreso.getRetos().removeIf(r -> r.getExpira() == null || !ahora.isBefore(r.getExpira()));

        String limite = ahora.toLocalDate().minusDays(90).toString();
        cambio |= progreso.getMetasReclamadas().keySet().removeIf(fecha -> fecha.compareTo(limite) < 0);

        String diaria = beyGeneradorRetosService.beyIdVentanaDiaria(ahora);
        if (progreso.getRetos().stream().noneMatch(r -> diaria.equals(r.getVentanaId()))) {
            progreso.getRetos().addAll(beyGeneradorRetosService.beyGenerarDiarios(datos, ahora));
            cambio = true;
        }
        String flash = beyGeneradorRetosService.beyIdVentanaFlash(ahora);
        if (progreso.getRetos().stream().noneMatch(r -> flash.equals(r.getVentanaId()))) {
            progreso.getRetos().add(beyGeneradorRetosService.beyGenerarFlash(datos, ahora));
            cambio = true;
        }
        return cambio;
    }

    private static Map<beyMetaXp, Boolean> beyEstadoMetas(beyBaseDatos datos, LocalDate fecha) {
        beyObjetivosNutricionales objetivos = datos.getPerfil().getObjetivos();
        beyDiaSalud dia = datos.buscarDia(fecha).orElse(null);
        Map<beyMetaXp, Boolean> estado = new EnumMap<>(beyMetaXp.class);

        estado.put(beyMetaXp.PROTEINA, dia != null && objetivos.getProteinaGramos() > 0
                && dia.getTotalProteina() >= objetivos.getProteinaGramos());
        estado.put(beyMetaXp.AGUA, dia != null && objetivos.getAguaMililitros() > 0
                && dia.getTotalHidratacionMl() >= objetivos.getAguaMililitros());
        estado.put(beyMetaXp.CREATINA, dia != null && dia.isCreatinaTomada());
        estado.put(beyMetaXp.SUENO, dia != null && dia.getHorasSueno() > 0
                && dia.getHorasSueno() >= objetivos.getSuenoMinimoHoras());
        estado.put(beyMetaXp.ENTRENAMIENTO, datos.getSesiones().stream().anyMatch(s -> beySesionDelDia(s, fecha)));

        boolean perfecto = estado.values().stream().allMatch(Boolean::booleanValue);
        estado.put(beyMetaXp.DIA_PERFECTO, perfecto);
        return estado;
    }

    private static boolean beySesionDelDia(beySesionEntrenamiento sesion, LocalDate fecha) {
        if (sesion.getFechaFin() == null || sesion.getSeriesCompletadas() <= 0) {
            return false;
        }
        return fecha.equals(sesion.getFechaFin().toLocalDate());
    }
}
