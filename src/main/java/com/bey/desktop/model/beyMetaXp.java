package com.bey.desktop.model;

public enum beyMetaXp {
    PROTEINA("Proteína del día", "Alcanza tu meta de proteína registrando tus comidas", 25),
    AGUA("Hidratación completa", "Completa tu meta de agua del día", 20),
    CREATINA("Creatina tomada", "Registra tu dosis diaria de creatina", 10),
    SUENO("Sueño reparador", "Cumple tus horas mínimas de sueño", 20),
    ENTRENAMIENTO("Sesión de entrenamiento", "Finaliza una sesión en el Workout Player", 40),
    DIA_PERFECTO("Día perfecto", "Cumple las cinco metas en el mismo día", 30);

    private final String titulo;
    private final String descripcion;
    private final int xp;

    beyMetaXp(String titulo, String descripcion, int xp) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.xp = xp;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getXp() {
        return xp;
    }
}
