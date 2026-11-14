package com.preguntones.reinicio;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/** Se queda con TERM e INT para que el panel no apague el proceso sin avisar. */
final class Senal {
    @FunctionalInterface
    interface Aviso {
        void alApagar();
    }

    private Senal() {
    }

    static List<String> instalar(Aviso aviso) {
        List<String> puestas = new ArrayList<>();
        for (String nombre : new String[] {"TERM", "INT"}) {
            if (enganchar(nombre, aviso)) {
                puestas.add(nombre);
            }
        }
        return puestas;
    }

    private static boolean enganchar(String nombre, Aviso aviso) {
        try {
            Class<?> signal = Class.forName("sun.misc.Signal");
            Class<?> handler = Class.forName("sun.misc.SignalHandler");
            Object sig = signal.getConstructor(String.class).newInstance(nombre);
            Object proxy = Proxy.newProxyInstance(handler.getClassLoader(), new Class<?>[] {handler}, (nada, method, args) -> {
                if ("handle".equals(method.getName())) {
                    aviso.alApagar();
                }
                return null;
            });
            Method handle = signal.getMethod("handle", signal, handler);
            handle.invoke(null, sig, proxy);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
