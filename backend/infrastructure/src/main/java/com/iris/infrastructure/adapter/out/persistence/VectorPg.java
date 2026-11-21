package com.iris.infrastructure.adapter.out.persistence;

/** Utilidad compartida: convierte un float[] al formato de texto que entiende pgvector. */
public final class VectorPg {

    private VectorPg() {
    }

    /** {0.1f, -0.2f} -> "[0.1,-0.2]". En SQL se convierte con CAST(? AS vector). */
    public static String aTexto(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 10).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
