package com.zszq.neonarsenal.mixin;

import net.minecraft.client.particle.Particle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read/write access to the world and previous positions of a particle. */
@Mixin(Particle.class)
public interface ParticleAccessor {

    @Accessor("x")
    double neonarsenal$x();

    @Accessor("x")
    void neonarsenal$setX(double value);

    @Accessor("y")
    double neonarsenal$y();

    @Accessor("y")
    void neonarsenal$setY(double value);

    @Accessor("z")
    double neonarsenal$z();

    @Accessor("z")
    void neonarsenal$setZ(double value);

    @Accessor("xo")
    double neonarsenal$previousX();

    @Accessor("xo")
    void neonarsenal$setPreviousX(double value);

    @Accessor("yo")
    double neonarsenal$previousY();

    @Accessor("yo")
    void neonarsenal$setPreviousY(double value);

    @Accessor("zo")
    double neonarsenal$previousZ();

    @Accessor("zo")
    void neonarsenal$setPreviousZ(double value);
}
