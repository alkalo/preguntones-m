package com.preguntones.bingo;

final class Premio {
    private Premio() {
    }

    static boolean toca(int hechas, int meta, boolean cobrado) {
        return !cobrado && meta > 0 && hechas >= meta;
    }
}
