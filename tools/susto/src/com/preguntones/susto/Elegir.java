package com.preguntones.susto;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Elige a alguien que no sea el último, si hay más gente. */
final class Elegir {
    private Elegir() {
    }

    static UUID uno(List<UUID> candidatos, UUID ultimo, Random azar) {
        if (candidatos.isEmpty()) {
            return null;
        }
        List<UUID> otros = new ArrayList<>();
        for (UUID id : candidatos) {
            if (!id.equals(ultimo)) {
                otros.add(id);
            }
        }
        List<UUID> bolsa = otros.isEmpty() ? candidatos : otros;
        return bolsa.get(azar.nextInt(bolsa.size()));
    }
}
