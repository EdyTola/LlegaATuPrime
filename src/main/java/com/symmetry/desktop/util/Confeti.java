package com.symmetry.desktop.util;

import javafx.scene.layout.Pane;

/**
 * Atajo historico del efecto de confeti: delega en {@link AchievementVFX#confeti(Pane)}, que
 * dibuja las particulas en un Canvas transparente.
 */
public final class Confeti {

    private Confeti() {
    }

    /** Lanza la lluvia de confeti sobre {@code capa} (debe ser transparente a los clics). */
    public static void estallar(Pane capa) {
        AchievementVFX.confeti(capa);
    }
}
