package com.dmzdamagedclothes.client;

import com.dmzdamagedclothes.DamagedConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Solta as particulas de 1 pixel dos pixels que acabaram de trocar/sumir. */
final class PixelParticles {

    private static final RandomSource RANDOM = RandomSource.create();

    private PixelParticles() {
    }

    /**
     * @param before pixels (ABGR) antes da mudanca; {@code after} depois. So pixels que existiam e mudaram soltam particula,
     *               com a cor que tinham antes.
     */
    static void burst(Player player, int[] before, int[] after) {
        if (!DamagedConfig.PARTICLES.get()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        List<Integer> changed = new ArrayList<>();
        for (int i = 0; i < before.length; i++) {
            if (((before[i] >>> 24) & 0xFF) != 0 && before[i] != after[i]) changed.add(i);
        }
        if (changed.isEmpty()) return;
        int limit = DamagedConfig.MAX_PARTICLES.get();
        while (changed.size() > limit) changed.remove(RANDOM.nextInt(changed.size()));

        float scale = player.getBbHeight() / 1.8F;
        float pixel = scale / 16.0F;
        double yaw = Math.toRadians(player.yBodyRot);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        // frente = (-sin, cos); direita do jogador = (-cos, -sin)
        for (int i : changed) {
            float[] p = PixelMapper.map(i % 64, i / 64);
            if (p == null) continue;
            double lat = p[0] * pixel;
            double front = p[2] * pixel;
            double x = player.getX() + (-cos) * lat + (-sin) * front;
            double z = player.getZ() + (-sin) * lat + cos * front;
            double y = player.getY() + p[1] * pixel;
            int c = before[i];
            // um empurraozinho para fora do corpo e para cima, depois cai
            double out = 0.03 + RANDOM.nextDouble() * 0.04;
            double vx = (x - player.getX()) * out * 8.0 + (RANDOM.nextDouble() - 0.5) * 0.03;
            double vz = (z - player.getZ()) * out * 8.0 + (RANDOM.nextDouble() - 0.5) * 0.03;
            double vy = 0.02 + RANDOM.nextDouble() * 0.06;
            Minecraft.getInstance().particleEngine.add(new PixelParticle(level, x, y, z, vx, vy, vz,
                    c & 0xFF, (c >>> 8) & 0xFF, (c >>> 16) & 0xFF, pixel));
        }
    }
}
