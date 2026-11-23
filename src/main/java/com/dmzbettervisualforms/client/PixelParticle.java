package com.dmzbettervisualforms.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

/**
 * Pixel da roupa: um quadradinho do tamanho de 1 pixel de textura, com a cor exata do pixel, e com a fisica das particulas
 * de bloco (gravidade, colide e desliza no chao). Usa a textura lisa de um bloco branco, pintada com a cor do pixel.
 */
final class PixelParticle extends TextureSheetParticle {

    PixelParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, int rgb, float size, RandomSource random) {
        super(level, x, y, z);
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper()
                .getParticleIcon(Blocks.WHITE_CONCRETE.defaultBlockState());
        this.setSprite(sprite);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = 1.0f;
        this.hasPhysics = true;
        this.quadSize = size * 0.5f;   // quadSize e a metade do lado; size = 1 pixel de armadura (1/16 de bloco, na escala do jogador)
        this.lifetime = 40 + random.nextInt(40);
        this.rCol = ((rgb >> 16) & 0xFF) / 255.0f;
        this.gCol = ((rgb >> 8) & 0xFF) / 255.0f;
        this.bCol = (rgb & 0xFF) / 255.0f;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float life = (this.age + partialTick) / this.lifetime;
        return life > 0.8f ? this.quadSize * Math.max(0.0f, (1.0f - life) / 0.2f) : this.quadSize;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }
}
