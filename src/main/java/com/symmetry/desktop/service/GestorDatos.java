package com.symmetry.desktop.service;

import com.symmetry.desktop.model.BaseDatos;
import com.symmetry.desktop.model.Comida;
import com.symmetry.desktop.model.DiaSalud;
import com.symmetry.desktop.model.Ejercicio;
import com.symmetry.desktop.model.FotoProgreso;
import com.symmetry.desktop.model.MedicionCorporal;
import com.symmetry.desktop.model.ModalidadEntrenamiento;
import com.symmetry.desktop.model.PistaAudio;
import com.symmetry.desktop.model.Recipiente;
import com.symmetry.desktop.model.RegistroHidratacion;
import com.symmetry.desktop.model.Rutina;
import com.symmetry.desktop.model.SesionEntrenamiento;
import com.symmetry.desktop.model.TemaApp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Fachada unica de acceso a los datos locales. Mantiene el estado en memoria,
 * lo carga al arrancar y lo guarda de forma atomica cuando hay cambios.
 * Los controladores JavaFX trabajan solo contra esta clase.
 */
public class GestorDatos {

    private static GestorDatos instancia;

    private PersistenciaService persistencia;
    private BaseDatos datos;
    private final GamificacionService gamificacion = new GamificacionService(this);

    private GestorDatos(PersistenciaService persistencia) {
        this.persistencia = persistencia;
    }

    /** Instancia compartida con la persistencia JSON por defecto. */
    public static synchronized GestorDatos getInstancia() {
        if (instancia == null) {
            instancia = new GestorDatos(PerfilService.getInstancia().inicializar());
        }
        return instancia;
    }

    /** Permite inyectar otra implementacion de persistencia (util para pruebas). */
    public static synchronized GestorDatos conPersistencia(PersistenciaService persistencia) {
        instancia = new GestorDatos(persistencia);
        instancia.cargar();
        return instancia;
    }

    /**
     * Cambia el archivo de datos activo (por ejemplo, al cambiar de perfil de usuario):
     * persiste el estado saliente y recarga la base de datos del nuevo origen.
     */
    public void cambiarPersistencia(PersistenciaService nuevaPersistencia) {
        if (nuevaPersistencia == null) {
            return;
        }
        if (datos != null) {
            persistencia.guardar(datos);
        }
        this.persistencia = nuevaPersistencia;
        this.datos = null;
        cargar();
    }

    /** Carga los datos existentes o crea la base inicial con las rutinas precargadas. */
    public void cargar() {
        BaseDatos cargada = persistencia.cargar();
        if (cargada == null) {
            datos = DatosInicialesService.crearBaseDatosInicial();
            persistencia.guardar(datos);
        } else {
            datos = cargada;
        }
    }

    public BaseDatos getDatos() {
        if (datos == null) {
            cargar();
        }
        return datos;
    }

    public Path getRutaDatos() {
        return persistencia.getArchivoDatos();
    }

    /** Guarda inmediatamente todo el estado. */
    public void guardar() {
        persistencia.guardar(getDatos());
    }

    /** Guarda solo si el guardado automatico esta activo en la configuracion. */
    public void guardarAutomatico() {
        if (getDatos().getConfiguracion().isGuardadoAutomatico()) {
            guardar();
        }
    }

    // ------------------------------------------------------------------
    // Entrenamiento
    // ------------------------------------------------------------------

    /** Rutina programada para una fecha concreta, si el dia tiene rutina asignada. */
    public Optional<Rutina> rutinaDe(LocalDate fecha) {
        return getDatos().buscarRutinaDe(fecha);
    }

    /**
     * Alternativas equivalentes para un ejercicio: primero las que el usuario definio
     * manualmente y despues otros ejercicios del catalogo con el mismo grupo muscular.
     * Es la base de la funcion "Maquina ocupada" del Workout Player.
     */
    public List<String> alternativasPara(Ejercicio ejercicio) {
        if (ejercicio == null) {
            return List.of();
        }
        Set<String> alternativas = new LinkedHashSet<>(ejercicio.getAlternativasPersonalizadas());
        for (Ejercicio candidato : getDatos().catalogoEjercicios()) {
            boolean mismoGrupo = candidato.getGrupoMuscular() == ejercicio.getGrupoMuscular();
            boolean distinto = !candidato.getNombre().equalsIgnoreCase(ejercicio.getNombre());
            if (mismoGrupo && distinto) {
                alternativas.add(candidato.getNombre());
            }
        }
        return new ArrayList<>(alternativas);
    }

    /** Registra una sesion finalizada y la persiste siempre en el archivo JSON local. */
    public void guardarSesion(SesionEntrenamiento sesion) {
        if (sesion == null) {
            return;
        }
        getDatos().getSesiones().removeIf(s -> s.getId().equals(sesion.getId()));
        getDatos().getSesiones().add(sesion);
        // "Finalizar y guardar" implica un guardado explicito: no depende del guardado automatico.
        guardar();
        gamificacion.evaluar();
    }

    public void agregarRutina(Rutina rutina) {
        if (rutina == null) {
            return;
        }
        getDatos().getRutinas().add(rutina);
        guardarAutomatico();
    }

    public void eliminarRutina(Rutina rutina) {
        if (rutina == null) {
            return;
        }
        getDatos().getRutinas().removeIf(r -> r.getId().equals(rutina.getId()));
        guardarAutomatico();
    }

    /** Persiste cambios realizados directamente sobre rutinas o ejercicios. */
    public void guardarCambiosEntrenamiento() {
        guardarAutomatico();
    }

    /** Modalidad de entrenamiento activa (con equipo o sin equipo / casa). */
    public ModalidadEntrenamiento getModalidad() {
        return getDatos().getConfiguracion().getModalidad();
    }

    /** Cambia y persiste la modalidad de entrenamiento. */
    public void setModalidad(ModalidadEntrenamiento modalidad) {
        getDatos().getConfiguracion().setModalidad(modalidad);
        guardarAutomatico();
    }

    // ------------------------------------------------------------------
    // Gamificacion (XP, niveles y retos)
    // ------------------------------------------------------------------

    public GamificacionService getGamificacion() {
        return gamificacion;
    }

    // ------------------------------------------------------------------
    // Nutricion, hidratacion, suplementacion y descanso
    // ------------------------------------------------------------------

    public DiaSalud dia(LocalDate fecha) {
        return getDatos().obtenerDia(fecha);
    }

    public DiaSalud hoy() {
        return dia(LocalDate.now());
    }

    public void registrarComida(Comida comida) {
        if (comida == null) {
            return;
        }
        LocalDate fecha = comida.getFechaHora() == null
                ? LocalDate.now() : comida.getFechaHora().toLocalDate();
        dia(fecha).getComidas().add(comida);
        guardarAutomatico();
        gamificacion.evaluar();
    }

    public void eliminarComida(LocalDate fecha, Comida comida) {
        dia(fecha).getComidas().removeIf(c -> c.getId().equals(comida.getId()));
        guardarAutomatico();
    }

    public void registrarHidratacion(LocalDate fecha, int mililitros, Recipiente recipiente) {
        if (mililitros <= 0) {
            return;
        }
        RegistroHidratacion registro = new RegistroHidratacion(
                mililitros,
                recipiente == null ? Recipiente.PERSONALIZADO : recipiente,
                LocalDateTime.now());
        dia(fecha).getHidratacion().add(registro);
        guardarAutomatico();
        gamificacion.evaluar();
    }

    public void eliminarHidratacion(LocalDate fecha, RegistroHidratacion registro) {
        dia(fecha).getHidratacion().removeIf(r -> r.getId().equals(registro.getId()));
        guardarAutomatico();
    }

    public void setCreatinaTomada(LocalDate fecha, boolean tomada) {
        dia(fecha).setCreatinaTomada(tomada);
        guardarAutomatico();
        gamificacion.evaluar();
    }

    public void registrarSueno(LocalDate fecha, double horas) {
        dia(fecha).setHorasSueno(horas);
        guardarAutomatico();
        gamificacion.evaluar();
    }

    // ------------------------------------------------------------------
    // Mediciones corporales
    // ------------------------------------------------------------------

    /** Registra la medicion del dia; si ya existe una para esa fecha, la actualiza.
     *  Los campos no informados (nulos) conservan el valor previamente registrado. */
    public void registrarMedicion(MedicionCorporal medicion) {
        if (medicion == null || medicion.getFecha() == null) {
            return;
        }
        List<MedicionCorporal> mediciones = getDatos().getMediciones();
        MedicionCorporal existente = mediciones.stream()
                .filter(m -> medicion.getFecha().equals(m.getFecha()))
                .findFirst()
                .orElse(null);
        if (existente == null) {
            mediciones.add(medicion);
        } else {
            if (medicion.getPesoKg() != null) {
                existente.setPesoKg(medicion.getPesoKg());
            }
            if (medicion.getCinturaCm() != null) {
                existente.setCinturaCm(medicion.getCinturaCm());
            }
            if (medicion.getGrasaPorcentaje() != null) {
                existente.setGrasaPorcentaje(medicion.getGrasaPorcentaje());
            }
            if (medicion.getMasaMuscularKg() != null) {
                existente.setMasaMuscularKg(medicion.getMasaMuscularKg());
            }
        }
        guardarAutomatico();
    }

    public void eliminarMedicion(MedicionCorporal medicion) {
        if (medicion == null) {
            return;
        }
        getDatos().getMediciones().removeIf(m -> m.getId().equals(medicion.getId()));
        guardarAutomatico();
    }

    // ------------------------------------------------------------------
    // Fotos de progreso (galeria de transformacion)
    // ------------------------------------------------------------------

    /** Anade una foto de progreso a la galeria y la persiste. */
    public void agregarFoto(FotoProgreso foto) {
        if (foto == null) {
            return;
        }
        getDatos().getFotos().add(foto);
        guardarAutomatico();
    }

    public void eliminarFoto(FotoProgreso foto) {
        if (foto == null) {
            return;
        }
        getDatos().getFotos().removeIf(f -> f.getId().equals(foto.getId()));
        guardarAutomatico();
    }

    // ------------------------------------------------------------------
    // Biblioteca musical
    // ------------------------------------------------------------------

    /** Anade pistas evitando duplicados por ruta de archivo (locales) o por enlace (externos). */
    public void agregarPistas(List<PistaAudio> pistas) {
        if (pistas == null) {
            return;
        }
        for (PistaAudio pista : pistas) {
            agregarPista(pista);
        }
    }

    /** Anade una unica pista si no existe ya otra equivalente en la biblioteca. */
    public void agregarPista(PistaAudio pista) {
        if (pista == null) {
            return;
        }
        boolean duplicada = getDatos().getBibliotecaMusical().stream()
                .anyMatch(p -> mismaPista(p, pista));
        if (!duplicada) {
            getDatos().getBibliotecaMusical().add(pista);
        }
        guardarAutomatico();
    }

    /** Dos pistas son iguales si comparten el mismo enlace o la misma ruta de archivo. */
    private static boolean mismaPista(PistaAudio a, PistaAudio b) {
        if (a.esEnlace() || b.esEnlace()) {
            return a.getEnlace() != null && a.getEnlace().equalsIgnoreCase(b.getEnlace());
        }
        return a.getRutaArchivo() != null && a.getRutaArchivo().equalsIgnoreCase(b.getRutaArchivo());
    }

    public void eliminarPista(PistaAudio pista) {
        if (pista == null) {
            return;
        }
        getDatos().getBibliotecaMusical().removeIf(p -> p.getId().equals(pista.getId()));
        guardarAutomatico();
    }

    public void limpiarBiblioteca() {
        getDatos().getBibliotecaMusical().clear();
        guardarAutomatico();
    }

    /** Persiste un nuevo orden de la biblioteca (arrastrar y soltar en el reproductor). */
    public void reordenarBiblioteca(List<PistaAudio> pistas) {
        if (pistas == null) {
            return;
        }
        List<PistaAudio> copia = new ArrayList<>(pistas);
        List<PistaAudio> biblioteca = getDatos().getBibliotecaMusical();
        biblioteca.clear();
        biblioteca.addAll(copia);
        guardarAutomatico();
    }

    /**
     * Carpeta gestionada por la aplicacion donde se guarda el audio descargado y convertido
     * a MP3 ({@code userData/music}). Se crea de forma perezosa si todavia no existe.
     */
    public Path getCarpetaMusica() {
        Path base = JsonPersistenciaService.rutaPorDefecto().getParent();
        Path carpeta = (base == null ? Path.of(".") : base).resolve("music");
        try {
            Files.createDirectories(carpeta);
        } catch (IOException ignored) {
            // Si no se puede crear, se devuelve la ruta igualmente; el error se informara al descargar.
        }
        return carpeta;
    }

    /** Guarda la ultima carpeta utilizada en el reproductor de musica. */
    public void recordarCarpetaMusica(String rutaCarpeta) {
        getDatos().getConfiguracion().setUltimaCarpetaMusica(rutaCarpeta);
        guardarAutomatico();
    }

    /** Guarda la ultima carpeta utilizada para adjuntar multimedia de ejercicios. */
    public void recordarCarpetaMultimedia(String rutaCarpeta) {
        getDatos().getConfiguracion().setUltimaCarpetaMultimedia(rutaCarpeta);
        guardarAutomatico();
    }

    // ------------------------------------------------------------------
    // Preferencias de interfaz (tema claro / oscuro)
    // ------------------------------------------------------------------

    /** Tema visual activo, tomado de la configuracion guardada. */
    public TemaApp getTema() {
        return getDatos().getConfiguracion().getTema();
    }

    /** Cambia y persiste el tema visual. */
    public void setTema(TemaApp tema) {
        getDatos().getConfiguracion().setTema(tema);
        guardarAutomatico();
    }

    /** Alterna entre modo claro y oscuro, devolviendo el tema resultante. */
    public TemaApp alternarTema() {
        TemaApp nuevo = getTema().alternado();
        setTema(nuevo);
        return nuevo;
    }
}
