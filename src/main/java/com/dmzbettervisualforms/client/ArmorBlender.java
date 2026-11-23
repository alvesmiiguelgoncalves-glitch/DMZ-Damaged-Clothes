package com.dmzbettervisualforms.client;

import com.dmzbettervisualforms.DmzBetterVisualForms;
import com.dmzbettervisualforms.SsjArmorConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Escolhe a textura da armadura para o jogador.
 *
 * <p>A roupa padrao fica neste addon em {@code dmz_ssj4_armors:textures/armor/<mesmo nome>.png}; outros
 * conjuntos ("perfis") ficam em {@code dmz_ssj4_armors:textures/armor/<perfil>/<mesmo nome>.png}, no
 * addon ou em qualquer resource pack.
 * Para a transicao, em vez de desenhar duas camadas, geramos texturas intermediarias (mistura
 * das duas, em {@link #STEPS} passos) e devolvemos a do passo atual. Assim o DMZ continua
 * desenhando a armadura do jeito dele e a transicao funciona em todos os caminhos de render
 * (corpo, camada customizada, mao em primeira pessoa, capa).</p>
 */
public final class ArmorBlender {

    public static final int STEPS = 12;
    private static final int MAX_BLENDS = 1024;
    private static final String DMZ_NAMESPACE = "dragonminez";
    private static final String ARMOR_DIR = "textures/armor/";

    private static final Map<ResourceLocation, Boolean> VARIANT_EXISTS = new HashMap<>();
    private static final Map<String, ResourceLocation> BLENDS = new HashMap<>();
    private static final Set<ResourceLocation> DYNAMIC_TEXTURES = new HashSet<>();
    private static final Set<String> FAILED = new HashSet<>();
    private static final Map<ResourceLocation, ResourceLocation> ARMLESS = new HashMap<>();

    /** Retangulos dos bracos num layout de skin 64x64: x, y, largura, altura (braco direito/esquerdo, camada de fora). */
    private static final int[][] ARM_RECTS = {{40, 16, 16, 16}, {32, 48, 16, 16}, {40, 32, 16, 16}, {48, 48, 16, 16}};

    private ArmorBlender() {
    }

    /**
     * @param original textura que o DMZ escolheu (ja considera roupa danificada etc.)
     * @return a mesma textura, a roupa do perfil da forma, ou uma mistura de duas
     */
    public static ResourceLocation apply(AbstractClientPlayer player, ResourceLocation original) {
        if (!SsjArmorConfig.ENABLED.get()) return original;
        if (!DMZ_NAMESPACE.equals(original.getNamespace())) return original;

        String path = original.getPath();
        if (!path.startsWith(ARMOR_DIR)) return original;

        FadeTracker.Frame frame = FadeTracker.frame(player);
        ResourceLocation from = resolve(original, path, frame.from);
        ResourceLocation to = resolve(original, path, frame.to);
        if (from.equals(to)) return from;

        float progress = frame.progress;
        // A dissolucao sempre vai da roupa "cheia" (com camisa) para a "vazia" (sem camisa), de cima para baixo, seja a
        // transformacao lenta ou a instantanea (nesta o DMZ entrega o par ao contrario). Na volta, o mesmo caminho ao contrario.
        if (density(to) > density(from)) {
            ResourceLocation swap = from;
            from = to;
            to = swap;
            progress = 1.0f - progress;
        }

        int step = Math.round(progress * STEPS);
        // particulas so nas formas SSJ4 GT e Daima (as outras trocas de roupa se desfazem sem particulas)
        if (isSsj4(frame.from) || isSsj4(frame.to)) DissolveParticles.note(player, from, to, progress);
        if (step <= 0) return from;
        if (step >= STEPS) return to;
        return blended(from, to, step);
    }

    private static boolean isSsj4(@Nullable String profile) {
        return FormMatcher.SSJ4_GT.equals(profile) || FormMatcher.SSJ4_DAIMA.equals(profile);
    }

    /** Textura do perfil para esta peca; se o perfil nao tem a peca, vale a original do DMZ. */
    private static ResourceLocation resolve(ResourceLocation original, String path, @Nullable String profile) {
        if (profile == null) return original;
        if (FormMatcher.NO_ARMS.equals(profile)) return armless(original);
        String rest = path.substring(ARMOR_DIR.length());
        String variantPath = profile.isEmpty() ? path : ARMOR_DIR + profile + "/" + rest;
        ResourceLocation variant = new ResourceLocation(DmzBetterVisualForms.TEXTURE_NAMESPACE, variantPath);
        if (variantExists(variant)) return variant;
        // SSJ4 (GT e Daima): o DMZ tambem nao desenha a manga; peca que a pasta nao tem perde a manga na transicao em vez de no fim
        boolean ssj4 = FormMatcher.SSJ4_GT.equals(profile) || FormMatcher.SSJ4_DAIMA.equals(profile);
        return ssj4 && SsjArmorConfig.FADE_SLEEVES.get() ? armless(original) : original;
    }

    private static final Map<ResourceLocation, Integer> DENSITY = new HashMap<>();

    /** Quantos pixels opacos a textura tem (a mais "cheia" e a que tem a camisa). */
    private static int density(ResourceLocation location) {
        Integer cached = DENSITY.get(location);
        if (cached != null) return cached;
        if (!RenderSystem.isOnRenderThread()) return 0;
        int count = 0;
        try (NativeImage image = read(location)) {
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if (((image.getPixelRGBA(x, y) >>> 24) & 0xFF) != 0) count++;
                }
            }
        } catch (Exception e) {
            // nao conseguiu ler: conta como 0 e nao tenta de novo
        }
        DENSITY.put(location, count);
        return count;
    }

    /** Chamado quando os resource packs recarregam: descarta caches e texturas geradas. */
    public static void clear() {
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation location : DYNAMIC_TEXTURES) {
            textures.release(location);
        }
        DYNAMIC_TEXTURES.clear();
        BLENDS.clear();
        ARMLESS.clear();
        DENSITY.clear();
        VARIANT_EXISTS.clear();
        FAILED.clear();
        FadeTracker.clear();
        DissolveParticles.clear();
        ArmorContext.reset();
    }

    /** A textura original com os bracos apagados (gerada uma vez); fora da thread de render devolve a original. */
    private static ResourceLocation armless(ResourceLocation original) {
        ResourceLocation cached = ARMLESS.get(original);
        if (cached != null) return cached;
        if (!RenderSystem.isOnRenderThread()) return original;

        NativeImage output = null;
        try (NativeImage source = read(original)) {
            int width = source.getWidth();
            int height = source.getHeight();
            output = new NativeImage(NativeImage.Format.RGBA, width, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    output.setPixelRGBA(x, y, insideArm(x * 64 / width, y * 64 / height) ? 0 : source.getPixelRGBA(x, y));
                }
            }
            ResourceLocation location = new ResourceLocation(DmzBetterVisualForms.TEXTURE_NAMESPACE,
                    "dynamic/armless_" + sanitize(original.toString()) + "_" + Integer.toHexString(original.hashCode()));
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(output));
            output = null;
            DYNAMIC_TEXTURES.add(location);
            ARMLESS.put(original, location);
            return location;
        } catch (Exception e) {
            ARMLESS.put(original, original); // nao tenta de novo ate recarregar
            DmzBetterVisualForms.LOGGER.warn("[dmz_better_visual_forms] nao foi possivel gerar a roupa sem manga de {}.", original, e);
            return original;
        } finally {
            if (output != null) output.close();
        }
    }

    private static boolean insideArm(int x, int y) {
        for (int[] r : ARM_RECTS) {
            if (x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3]) return true;
        }
        return false;
    }

    private static boolean variantExists(ResourceLocation variant) {
        return VARIANT_EXISTS.computeIfAbsent(variant,
                location -> Minecraft.getInstance().getResourceManager().getResource(location).isPresent());
    }

    private static ResourceLocation blended(ResourceLocation original, ResourceLocation variant, int step) {
        String key = original + "|" + variant + "#" + step;
        ResourceLocation cached = BLENDS.get(key);
        if (cached != null) return cached;

        ResourceLocation result;
        if (BLENDS.size() >= MAX_BLENDS || FAILED.contains(key) || !RenderSystem.isOnRenderThread()) {
            result = hardSwitch(original, variant, step);
            // Nao guarda: se foi so falta de thread/limite, tenta de novo depois.
            return result;
        }

        result = createBlend(original, variant, step, key);
        BLENDS.put(key, result);
        return result;
    }

    private static ResourceLocation hardSwitch(ResourceLocation original, ResourceLocation variant, int step) {
        return step * 2 >= STEPS ? variant : original;
    }

    private static ResourceLocation createBlend(ResourceLocation original, ResourceLocation variant, int step, String key) {
        float t = step / (float) STEPS;
        NativeImage output = null;
        try (NativeImage from = read(original); NativeImage to = read(variant)) {
            int width = to.getWidth();
            int height = to.getHeight();
            int fromWidth = from.getWidth();
            int fromHeight = from.getHeight();

            output = new NativeImage(NativeImage.Format.RGBA, width, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int a = from.getPixelRGBA(x * fromWidth / width, y * fromHeight / height);
                    int b = to.getPixelRGBA(x, y);
                    output.setPixelRGBA(x, y, mix(a, b, t, threshold(x, y * 64 / height), hem(x * 64 / width, y * 64 / height)));
                }
            }

            ResourceLocation location = new ResourceLocation(DmzBetterVisualForms.TEXTURE_NAMESPACE,
                    "dynamic/" + sanitize(key) + "_" + Integer.toHexString(key.hashCode()));
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(output));
            output = null; // agora e responsabilidade do DynamicTexture
            DYNAMIC_TEXTURES.add(location);
            return location;
        } catch (Exception e) {
            if (FAILED.add(key)) {
                DmzBetterVisualForms.LOGGER.warn("Nao foi possivel gerar a transicao de {} (passo {}); usando troca direta.",
                        original, step, e);
            }
            return hardSwitch(original, variant, step);
        } finally {
            if (output != null) output.close();
        }
    }

    static NativeImage read(ResourceLocation location) throws IOException {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(location)) {
            return NativeImage.read(in);
        }
    }

    /**
     * Mistura dois pixels ABGR.
     *
     * <p>A armadura do DMZ e desenhada com um render type "cutout": o pixel ou aparece inteiro ou
     * some, nao existe meia transparencia. Por isso um pixel que so existe numa das roupas
     * (a camisa que some no SSJ4, a munhequeira que aparece...) nao pode simplesmente ficar mais
     * ou menos transparente: ele "dissolve", aparecendo/sumindo em pontos espalhados conforme o
     * progresso. O mesmo ponto usa sempre o mesmo limiar, entao ida e volta sao a mesma animacao
     * ao contrario. Pixels que existem nas duas roupas misturam a cor normalmente.</p>
     */
    private static int mix(int a, int b, float t, float threshold, boolean hem) {
        int alphaA = (a >>> 24) & 0xFF;
        int alphaB = (b >>> 24) & 0xFF;

        if (alphaA == 0 && alphaB == 0) return 0;
        // Punho do braco (munhequeira): como a posicao dos pixels pode mudar de uma roupa para a outra, ela nao some e
        // reaparece: a antiga fica ate o fim e a nova ja aparece no comeco.
        if (alphaB == 0) return (hem || threshold >= t) ? a : 0;   // so na roupa antiga: some quando t passa do limiar
        if (alphaA == 0) return (hem || threshold < t) ? b : 0;    // so na roupa da forma: aparece quando t passa do limiar

        float weightA = alphaA * (1.0f - t);
        float weightB = alphaB * t;
        float total = weightA + weightB;
        int red = channel(a & 0xFF, b & 0xFF, weightA, weightB, total);
        int green = channel((a >>> 8) & 0xFF, (b >>> 8) & 0xFF, weightA, weightB, total);
        int blue = channel((a >>> 16) & 0xFF, (b >>> 16) & 0xFF, weightA, weightB, total);
        int alpha = Math.round(alphaA * (1.0f - t) + alphaB * t);
        return (alpha << 24) | (blue << 16) | (green << 8) | red;
    }

    /**
     * Limiar fixo (0..1) por pixel. Mistura a altura na peca (topo some primeiro, base por ultimo, nos layouts
     * de skin de 64 px onde cada peca tem linhas de 4 a 15 dentro do bloco de 16) com um pouco de ruido,
     * para nao ficar uma linha reta. Linhas fora das pecas (cabeca etc.) usam so o ruido.
     */
    static float threshold(int x, int y) {
        float noise = noise(x, y);
        if (y < 16) return noise;
        float s = Math.max(0.0f, Math.min(1.0f, ((y & 15) - 4) / 11.0f));
        return Math.min(1.0f, 0.78f * s + 0.22f * noise);
    }

    /** Punho do braco (4 linhas de baixo dos bracos): so ali, a munhequeira nao some e reaparece. A camisa nao entra. */
    static boolean hem(int x, int y) {
        return y >= 16 && (y & 15) >= 12 && insideArm(x, y);
    }

    /** Ruido fixo (0..1) por pixel, espalhado de forma irregular. */
    private static float noise(int x, int y) {
        int h = x * 0x1F123BB5 + y * 0x5F356495;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return (h >>> 8) / (float) (1 << 24);
    }

    private static int channel(int a, int b, float weightA, float weightB, float total) {
        int value = Math.round((a * weightA + b * weightB) / total);
        return Math.max(0, Math.min(255, value));
    }

    private static String sanitize(String key) {
        String name = key.toLowerCase(Locale.ROOT)
                .replace("textures/armor/", "")
                .replace(".png", "")
                .replace('#', '_');
        StringBuilder out = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/';
            out.append(ok ? c : '_');
        }
        return out.toString();
    }
}
