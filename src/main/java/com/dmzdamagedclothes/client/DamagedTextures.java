package com.dmzdamagedclothes.client;

import com.dmzdamagedclothes.DamagedConfig;
import com.dmzdamagedclothes.DmzDamagedClothes;
import com.dragonminez.client.systems.FormVisualTransition;
import com.dragonminez.common.stats.FormTransition;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Roupa que rasga conforme a durabilidade, pixel a pixel.
 *
 * <p>Os estagios ficam em {@code dmz_damaged_clothes:textures/damaged/<item>/00.png, 01.png, ...} (00 = roupa inteira,
 * o ultimo = mais destruida), cada um num unico PNG 64x64 com as layers 1 e 2 do DMZ juntas. A layer certa e separada
 * com as mascaras das texturas originais do DMZ ({@code <item>_layer1.png} / {@code _layer2.png}).</p>
 *
 * <p>A armadura do DMZ e desenhada em "cutout": cada pixel aparece inteiro ou some. Entre o estagio k e o k+1 cada pixel
 * tem um limiar fixo (ruido); quando a fracao de desgaste passa do limiar, aquele pixel troca de uma vez para o do
 * estagio k+1. Sem cor intermediaria. As texturas dos passos intermediarios sao geradas na primeira vez que aparecem.</p>
 */
public final class DamagedTextures {

    /** Passos entre dois estagios (alem dos proprios estagios). */
    public static final int STEPS = 8;
    private static final String DMZ_NAMESPACE = "dragonminez";
    private static final int SIZE = 64;

    private static final Map<String, Integer> COUNTS = new HashMap<>();
    private static final Map<ResourceLocation, int[]> PIXELS = new HashMap<>();
    private static final Map<String, boolean[]> MASKS = new HashMap<>();
    private static final Map<String, ResourceLocation> GENERATED = new HashMap<>();
    private static final Set<ResourceLocation> DYNAMIC = new HashSet<>();
    private static final Set<String> FAILED = new HashSet<>();
    private static final Map<String, boolean[]> KEEP = new HashMap<>();
    /** Ultimo estado desenhado de cada peca de cada jogador: {stage, step, cut}. Serve para saber quando soltar particulas. */
    private static final Map<Integer, int[]> LAST = new HashMap<>();

    /** Passos do recorte da camisa/mangas no SSJ4 (0 = inteira, CUT_STEPS = recortada). */
    public static final int CUT_STEPS = 6;

    private DamagedTextures() {
    }

    public static ResourceLocation apply(String modId, String itemId, EquipmentSlot slot, ItemStack stack, ResourceLocation original) {
        if (!DMZ_NAMESPACE.equals(modId) || stack == null || stack.isEmpty()) return original;
        int max = stack.getMaxDamage();
        if (max <= 0) return original;
        int count = stageCount(itemId);
        if (count < 2) return original;
        if (!RenderSystem.isOnRenderThread()) return original;

        float wear = Math.max(0f, Math.min(1f, stack.getDamageValue() / (float) max));
        float position = wear * (count - 1);
        int stage = (int) Math.floor(position);
        int step = Math.round((position - stage) * STEPS);
        if (stage >= count - 1) {
            stage = count - 1;
            step = 0;
        } else if (step >= STEPS) {
            stage++;
            step = 0;
        }

        int layer = slot == EquipmentSlot.LEGS ? 2 : 1;
        Player wearer = findWearer(stack, slot);
        int cut = 0;
        if (wearer != null && layer == 1 && DamagedConfig.SSJ4_CUT.get() && keep(itemId) != null) {
            cut = Math.round(ssj4Progress(wearer) * CUT_STEPS);
        }

        if (wearer != null) track(wearer, slot, itemId, layer, stage, step, cut);

        String key = itemId + "|" + layer + "|" + stage + "|" + step + "|" + cut;
        ResourceLocation cached = GENERATED.get(key);
        if (cached != null) return cached;
        if (FAILED.contains(key)) return original;

        ResourceLocation made = create(itemId, layer, stage, step, cut, key);
        if (made == null) {
            FAILED.add(key);
            return original;
        }
        GENERATED.put(key, made);
        return made;
    }

    /** Jogador (cliente) que esta vestindo exatamente esse stack nesse slot; {@code null} se nao achar (GUI, armor stand...). */
    private static Player findWearer(ItemStack stack, EquipmentSlot slot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        for (Player p : mc.level.players()) {
            if (p.getItemBySlot(slot) == stack) return p;
        }
        return null;
    }

    /** 0 = forma normal, 1 = SSJ4 completo; acompanha a barra de transformacao do DMZ (carga e volta ao normal). */
    private static float ssj4Progress(Player player) {
        StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null || stats.getCharacter() == null) return 0.0F;
        boolean current = isSsj4(FormTransition.Look.current(stats.getCharacter()));

        FormTransition.Charge charge = FormVisualTransition.charge(stats);
        if (charge != null && isSsj4(charge.look()) != current) {
            float f = clamp01(charge.factor());
            return current ? 1.0F - f : f;
        }
        FormTransition.Revert revert = FormVisualTransition.revert(stats);
        if (revert != null && isSsj4(revert.look()) != current) {
            // factor: 1 = visual de antes, 0 = visual atual
            float f = clamp01(revert.factor());
            return current ? 1.0F - f : f;
        }
        return current ? 1.0F : 0.0F;
    }

    private static boolean isSsj4(FormTransition.Look look) {
        return look != null && look.customModel().contains("ssj4");
    }

    private static float clamp01(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    /** Se o estado desta peca avancou (mais rasgada / mais recortada), solta particulas dos pixels que mudaram. */
    private static void track(Player player, EquipmentSlot slot, String itemId, int layer, int stage, int step, int cut) {
        int id = player.getId() * 16 + slot.ordinal();
        int[] last = LAST.get(id);
        if (last == null) {
            LAST.put(id, new int[]{stage, step, cut});
            return;
        }
        if (last[0] == stage && last[1] == step && last[2] == cut) return;
        boolean worse = stage * STEPS + step > last[0] * STEPS + last[1] || cut > last[2];
        int[] before = null;
        int[] prev = last.clone();
        last[0] = stage;
        last[1] = step;
        last[2] = cut;
        if (!worse || !DamagedConfig.PARTICLES.get()) return;
        try {
            before = compose(itemId, layer, prev[0], prev[1], prev[2]);
            int[] after = compose(itemId, layer, stage, step, cut);
            PixelParticles.burst(player, before, after);
        } catch (Exception e) {
            DmzDamagedClothes.LOGGER.debug("Sem particulas para {}: {}", itemId, e.toString());
        }
    }

    /** Chamado quando os resource packs recarregam. */
    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation location : DYNAMIC) {
            mc.getTextureManager().release(location);
        }
        DYNAMIC.clear();
        GENERATED.clear();
        COUNTS.clear();
        PIXELS.clear();
        MASKS.clear();
        FAILED.clear();
        KEEP.clear();
        LAST.clear();
    }

    private static int stageCount(String itemId) {
        Integer cached = COUNTS.get(itemId);
        if (cached != null) return cached;
        int n = 0;
        while (n < 99 && Minecraft.getInstance().getResourceManager().getResource(stageLocation(itemId, n)).isPresent()) n++;
        COUNTS.put(itemId, n);
        return n;
    }

    private static ResourceLocation stageLocation(String itemId, int stage) {
        return new ResourceLocation(DmzDamagedClothes.MOD_ID, "textures/damaged/" + itemId + "/" + (stage < 10 ? "0" : "") + stage + ".png");
    }

    private static ResourceLocation create(String itemId, int layer, int stage, int step, int cut, String key) {
        NativeImage output = null;
        try {
            int[] composed = compose(itemId, layer, stage, step, cut);
            output = new NativeImage(NativeImage.Format.RGBA, SIZE, SIZE, false);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    output.setPixelRGBA(x, y, composed[y * SIZE + x]);
                }
            }
            ResourceLocation location = new ResourceLocation(DmzDamagedClothes.MOD_ID,
                    "dynamic/" + itemId + "_l" + layer + "_" + stage + "_" + step + "_c" + cut);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(output));
            output = null;
            DYNAMIC.add(location);
            return location;
        } catch (Exception e) {
            DmzDamagedClothes.LOGGER.warn("Nao foi possivel gerar a textura {} da roupa danificada; usando a do DMZ.", key, e);
            return null;
        } finally {
            if (output != null) output.close();
        }
    }

    /** Pixels finais (ABGR, 64x64) de uma peca: mistura entre dois estagios + recorte do SSJ4. */
    private static int[] compose(String itemId, int layer, int stage, int step, int cut) throws Exception {
        int[] a = pixels(stageLocation(itemId, stage));
        int[] b = step == 0 ? a : pixels(stageLocation(itemId, stage + 1));
        boolean[] mask = mask(itemId, layer);
        boolean[] keep = cut > 0 && layer == 1 ? keep(itemId) : null;
        float t = step / (float) STEPS;
        float c = cut / (float) CUT_STEPS;
        int[] out = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int i = y * SIZE + x;
                if (!mask[i]) continue;
                if (keep != null && !keep[i] && noise(x + 97, y + 31) < c) continue;   // camisa e mangas do SSJ4 sumindo
                out[i] = noise(x, y) < t ? b[i] : a[i];
            }
        }
        return out;
    }

    /**
     * Pixels da layer 1 que o SSJ4 MANTEM (calca/cinto etc.), da textura {@code ssj4_keep.png} do conjunto. Os que nao estao
     * nela (camisa e mangas) sao recortados. {@code null} se o conjunto nao tem essa mascara.
     */
    private static boolean[] keep(String itemId) {
        if (KEEP.containsKey(itemId)) return KEEP.get(itemId);
        boolean[] out = null;
        ResourceLocation location = new ResourceLocation(DmzDamagedClothes.MOD_ID, "textures/damaged/" + itemId + "/ssj4_keep.png");
        if (Minecraft.getInstance().getResourceManager().getResource(location).isPresent()) {
            try (InputStream in = Minecraft.getInstance().getResourceManager().open(location);
                 NativeImage image = NativeImage.read(in)) {
                out = new boolean[SIZE * SIZE];
                for (int y = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++) {
                        out[y * SIZE + x] = ((image.getPixelRGBA(x * image.getWidth() / SIZE, y * image.getHeight() / SIZE) >>> 24) & 0xFF) != 0;
                    }
                }
            } catch (Exception e) {
                out = null;
            }
        }
        KEEP.put(itemId, out);
        return out;
    }

    /** Pixels ABGR (como o NativeImage guarda) de um estagio, em 64x64. */
    private static int[] pixels(ResourceLocation location) throws Exception {
        int[] cached = PIXELS.get(location);
        if (cached != null) return cached;
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(location);
             NativeImage image = NativeImage.read(in)) {
            int[] out = new int[SIZE * SIZE];
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    out[y * SIZE + x] = image.getPixelRGBA(x * image.getWidth() / SIZE, y * image.getHeight() / SIZE);
                }
            }
            PIXELS.put(location, out);
            return out;
        }
    }

    /** Pixels opacos da layer original (inteira) do DMZ: onde a roupa existe nessa layer. */
    private static boolean[] mask(String itemId, int layer) throws Exception {
        String key = itemId + "|" + layer;
        boolean[] cached = MASKS.get(key);
        if (cached != null) return cached;
        ResourceLocation location = new ResourceLocation(DMZ_NAMESPACE, "textures/armor/" + itemId + "_layer" + layer + ".png");
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(location);
             NativeImage image = NativeImage.read(in)) {
            boolean[] out = new boolean[SIZE * SIZE];
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    out[y * SIZE + x] = ((image.getPixelRGBA(x * image.getWidth() / SIZE, y * image.getHeight() / SIZE) >>> 24) & 0xFF) != 0;
                }
            }
            MASKS.put(key, out);
            return out;
        }
    }

    /** Limiar fixo (0..1) por pixel, espalhado de forma irregular. Mesmo pixel = mesmo limiar, sempre. */
    private static float noise(int x, int y) {
        int h = x * 0x1F123BB5 + y * 0x5F356495;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return (h >>> 8) / (float) (1 << 24);
    }
}
