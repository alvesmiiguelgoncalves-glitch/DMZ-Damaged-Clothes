package com.dmzbettervisualforms.client;

import com.dmzbettervisualforms.DmzBetterVisualForms;
import com.dmzbettervisualforms.SsjArmorConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Particulas de pixels da roupa que se desfaz. Cada pixel que some da textura solta uma particula de 1 pixel, na posicao do
 * proprio pixel no corpo (via {@link PixelMapper}), com a cor dele, que sai para fora do corpo e cai no chao.
 *
 * <p>O {@link ArmorBlender} avisa a cada quadro (thread de render) qual par de texturas esta se misturando e em que ponto
 * ({@link #note}). Como cada pixel some em um passo conhecido (o mesmo limiar da mistura), basta soltar, a cada passo novo,
 * os pixels que sumiram nele. Isso vale para a transformacao lenta e para a instantanea, que pode pular varios passos de
 * um quadro para o outro.</p>
 */
@Mod.EventBusSubscriber(modid = DmzBetterVisualForms.MOD_ID, value = Dist.CLIENT)
public final class DissolveParticles {

    private record Key(int player, ResourceLocation from) {
    }

    private static final class Note {
        ResourceLocation from;
        ResourceLocation to;
        int lastStep = -1;
        boolean rising;
        long tick;
    }

    /** Pixels que somem na mistura {@code from -> to}: passo em que somem, posicao no corpo (lateral, altura, frente) e cor. */
    private record Burst(int[] step, float[] lateral, float[] height, float[] front, int[] rgb) {
        int size() {
            return step.length;
        }
    }

    private static final Map<Key, Note> NOTES = new HashMap<>();
    private static final Map<String, Burst> BURSTS = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Burst EMPTY = new Burst(new int[0], new float[0], new float[0], new float[0], new int[0]);

    private DissolveParticles() {
    }

    /**
     * Chamado pelo render a cada peca de roupa que esta se desfazendo.
     *
     * @param progress 0 = roupa inteira (com camisa), 1 = toda desfeita
     */
    static void note(AbstractClientPlayer player, ResourceLocation from, ResourceLocation to, float progress) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        if (!SsjArmorConfig.ENABLED.get() || !SsjArmorConfig.DISSOLVE_PARTICLES.get()) return;

        Note note = NOTES.computeIfAbsent(new Key(player.getId(), from), key -> new Note());
        note.from = from;
        note.to = to;
        note.tick = level.getGameTime();

        int step = Math.round(progress * ArmorBlender.STEPS);
        // a dissolucao comeca em 0; se o primeiro quadro ja vem perto do fim, a roupa esta VOLTANDO: nada a soltar
        if (note.lastStep < 0) note.lastStep = progress < 0.5f ? 0 : step;
        if (step > note.lastStep) {
            emit(level, player, note, note.lastStep, step);
            note.lastStep = step;
            note.rising = true;
        } else if (step < note.lastStep) {
            note.lastStep = step;
            note.rising = false;
        }
    }

    static void clear() {
        NOTES.clear();
        BURSTS.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            NOTES.clear();
            return;
        }
        if (mc.isPaused()) return;

        long now = level.getGameTime();
        Iterator<Map.Entry<Key, Note>> it = NOTES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Key, Note> entry = it.next();
            Note note = entry.getValue();
            if (now - note.tick <= 2) continue;
            // A transicao acabou (a instantanea pode terminar de um quadro para o outro): solta o que faltou
            if (note.rising && note.lastStep >= ArmorBlender.STEPS / 2 && note.lastStep < ArmorBlender.STEPS
                    && level.getEntity(entry.getKey().player()) instanceof Player player) {
                emit(level, player, note, note.lastStep, ArmorBlender.STEPS);
            }
            it.remove();
        }
    }

    /** Solta as particulas dos pixels que somem nos passos {@code (after, upTo]}. */
    private static void emit(ClientLevel level, Player player, Note note, int after, int upTo) {
        Burst burst = burst(note.from, note.to);
        if (burst.size() == 0) return;

        float keep = Math.min(1.0f, SsjArmorConfig.DISSOLVE_PARTICLE_COUNT.get() / (float) burst.size());
        float scale = player.getBbHeight() / 1.8f;
        float pixel = scale / 16.0f;
        double yaw = Math.toRadians(player.yBodyRot);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        // frente = (-sin, cos); direita do jogador = (-cos, -sin)
        for (int i = 0; i < burst.size(); i++) {
            int step = burst.step()[i];
            if (step <= after || step > upTo) continue;
            if (keep < 1.0f && RANDOM.nextFloat() >= keep) continue;

            double lat = burst.lateral()[i] * pixel;
            double front = burst.front()[i] * pixel;
            double x = player.getX() + (-cos) * lat + (-sin) * front;
            double z = player.getZ() + (-sin) * lat + cos * front;
            double y = player.getY() + burst.height()[i] * pixel;
            // um empurrao para fora do corpo e um pouco para cima; depois cai
            double out = 0.03 + RANDOM.nextDouble() * 0.04;
            double vx = (x - player.getX()) * out * 8.0 + (RANDOM.nextDouble() - 0.5) * 0.03;
            double vz = (z - player.getZ()) * out * 8.0 + (RANDOM.nextDouble() - 0.5) * 0.03;
            double vy = 0.02 + RANDOM.nextDouble() * 0.06;
            Minecraft.getInstance().particleEngine.add(new PixelParticle(level, x, y, z, vx, vy, vz, burst.rgb()[i], pixel, RANDOM));
        }
    }

    private static Burst burst(ResourceLocation from, ResourceLocation to) {
        String key = from + "|" + to;
        Burst cached = BURSTS.get(key);
        if (cached != null) return cached;

        Burst result = EMPTY;
        try (NativeImage a = ArmorBlender.read(from)) {
            NativeImage b = null;
            try {
                b = ArmorBlender.read(to);   // texturas geradas (sem manga) nao estao nos resource packs: contam como vazias
            } catch (Exception ignored) {
                // sem a textura de destino
            }
            try {
                result = compute(a, b);
            } finally {
                if (b != null) b.close();
            }
        } catch (Exception e) {
            DmzBetterVisualForms.LOGGER.warn("Nao foi possivel preparar as particulas de {}.", from, e);
        }
        BURSTS.put(key, result);
        return result;
    }

    private static Burst compute(NativeImage a, NativeImage b) {
        int w = a.getWidth();
        int h = a.getHeight();
        List<float[]> found = new ArrayList<>();
        List<Integer> steps = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = a.getPixelRGBA(x, y);
                if (((pixel >>> 24) & 0xFF) == 0) continue;
                if (b != null && ((b.getPixelRGBA(x * b.getWidth() / w, y * b.getHeight() / h) >>> 24) & 0xFF) != 0) continue; // fica nas duas: so troca de cor
                int px = x * 64 / w;
                int py = y * 64 / h;
                float[] position = PixelMapper.map(px, py);
                if (position == null) continue;   // fora das pecas conhecidas (camada de fora etc.)

                // mesmo criterio da mistura: pixel so da roupa antiga some no primeiro passo k com k/STEPS > limiar
                float threshold = ArmorBlender.threshold(x, py);
                int step = (ArmorBlender.hem(px, py) || threshold >= 1.0f)
                        ? ArmorBlender.STEPS
                        : Math.min(ArmorBlender.STEPS, (int) Math.floor(threshold * ArmorBlender.STEPS) + 1);
                found.add(position);
                steps.add(step);
                colors.add(pixel);
            }
        }
        int n = found.size();
        if (n == 0) return EMPTY;
        int[] stepArray = new int[n];
        float[] lateral = new float[n];
        float[] height = new float[n];
        float[] front = new float[n];
        int[] rgb = new int[n];
        for (int i = 0; i < n; i++) {
            stepArray[i] = steps.get(i);
            lateral[i] = found.get(i)[0];
            height[i] = found.get(i)[1];
            front[i] = found.get(i)[2];
            int c = colors.get(i);
            // ABGR -> 0xRRGGBB
            rgb[i] = ((c & 0xFF) << 16) | (c & 0xFF00) | ((c >>> 16) & 0xFF);
        }
        return new Burst(stepArray, lateral, height, front, rgb);
    }
}
