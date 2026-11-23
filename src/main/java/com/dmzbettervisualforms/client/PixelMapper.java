package com.dmzbettervisualforms.client;

/**
 * Converte um pixel da textura de armadura 64x64 (layout padrao de jogador) na posicao dele no corpo, em pixels de modelo:
 * {@code out = {lateral (+ = direita do jogador), altura acima dos pes, frente (+ = para onde ele olha)}}.
 * E aproximado (serve para as particulas sairem da regiao certa), nao precisa ser exato.
 */
final class PixelMapper {

    // u0, v0, w, h, d, x0, y0(topo, a partir do pivo do corpo), z0, alturaDoTopoDoPivo
    private static final int[][] BOXES = {
            {0, 0, 8, 8, 8, -4, -8, -4},     // cabeca
            {16, 16, 8, 12, 4, -4, 0, -2},   // corpo
            {40, 16, 4, 12, 4, -8, 0, -2},   // braco direito
            {32, 48, 4, 12, 4, 4, 0, -2},    // braco esquerdo
            {0, 16, 4, 12, 4, -4, 12, -2},   // perna direita
            {16, 48, 4, 12, 4, 0, 12, -2},   // perna esquerda
    };

    private PixelMapper() {
    }

    /** @return {lateral, altura, frente} em pixels, ou {@code null} se o pixel nao esta em nenhuma caixa. */
    static float[] map(int px, int py) {
        for (int[] b : BOXES) {
            int u0 = b[0], v0 = b[1], w = b[2], h = b[3], d = b[4], x0 = b[5], y0 = b[6], z0 = b[7];
            int du, dv;
            float mx, my, mz;
            // frente
            du = px - (u0 + d);
            dv = py - (v0 + d);
            if (du >= 0 && du < w && dv >= 0 && dv < h) {
                mx = x0 + du; my = y0 + dv; mz = z0;
                return out(mx, my, mz);
            }
            // costas
            du = px - (u0 + 2 * d + w);
            if (du >= 0 && du < w && dv >= 0 && dv < h) {
                mx = x0 + w - 1 - du; my = y0 + dv; mz = z0 + d;
                return out(mx, my, mz);
            }
            // lado direito do jogador (-x do modelo)
            du = px - u0;
            if (du >= 0 && du < d && dv >= 0 && dv < h) {
                mx = x0; my = y0 + dv; mz = z0 + d - 1 - du;
                return out(mx, my, mz);
            }
            // lado esquerdo
            du = px - (u0 + d + w);
            if (du >= 0 && du < d && dv >= 0 && dv < h) {
                mx = x0 + w; my = y0 + dv; mz = z0 + du;
                return out(mx, my, mz);
            }
            // topo
            du = px - (u0 + d);
            dv = py - v0;
            if (du >= 0 && du < w && dv >= 0 && dv < d) {
                return out(x0 + du, y0, z0 + d - 1 - dv);
            }
            // base
            du = px - (u0 + d + w);
            if (du >= 0 && du < w && dv >= 0 && dv < d) {
                return out(x0 + du, y0 + h, z0 + dv);
            }
        }
        return null;
    }

    /** Modelo: x+ = esquerda do jogador, y para baixo a partir do pivo (24 px acima dos pes), z- = frente. */
    private static float[] out(float mx, float my, float mz) {
        float lateral = -(mx + 0.5F);
        float height = 24.0F - (my + 0.5F);
        float front = -(mz + 0.5F);
        return new float[]{lateral, height, front};
    }
}
