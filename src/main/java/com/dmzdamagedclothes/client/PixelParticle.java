package com.dmzdamagedclothes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.level.block.Blocks;

/**
 * Pedacinho de roupa de 1 pixel: igual as particulas de quebrar bloco (usa o atlas de blocos, cai com gravidade e fica no chao),
 * mas do tamanho de 1 pixel da textura da armadura e na cor exata do pixel que soltou.
 */
final class PixelParticle extends TextureSheetParticle {

    /** @param size lado do quadrado em blocos (1 pixel da armadura = 1/16 de bloco, escalado pelo tamanho do jogador). */
    PixelParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, int r, int g, int b, float size) {
        super(level, x, y, z);
        this.setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper()
                .getParticleIcon(Blocks.WHITE_CONCRETE.defaultBlockState()));
        this.gravity = 1.0F;
        this.hasPhysics = true;
        this.friction = 0.98F;
        this.rCol = r / 255.0F;
        this.gCol = g / 255.0F;
        this.bCol = b / 255.0F;
        this.quadSize = size * 0.5F;          // quadSize e a metade do lado
        this.lifetime = 50 + this.random.nextInt(30);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }
}
