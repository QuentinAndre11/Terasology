// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.engine.particles.functions.generators;

import org.terasology.engine.entitySystem.Component;
import org.terasology.gestalt.module.sandbox.API;
import org.terasology.engine.particles.ParticleData;
import org.terasology.engine.particles.ParticleDataMask;
import org.terasology.engine.particles.functions.ParticleSystemFunction;
import org.terasology.engine.utilities.random.Random;

/**
 * A generator function is called on a particle's data when it is created to set its fields.
 */
@API
public abstract class GeneratorFunction<T extends Component> extends ParticleSystemFunction<T> {
    public GeneratorFunction(ParticleDataMask dataMask, ParticleDataMask... dataMasks) {
        super(dataMask, dataMasks);
    }

    public abstract void onEmission(T component, ParticleData particleData, Random random);
}