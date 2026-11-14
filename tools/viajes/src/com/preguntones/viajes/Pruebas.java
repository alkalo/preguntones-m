package com.preguntones.viajes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class Pruebas {
    public static void main(String[] args) {
        nadarConAireNoEsAhogarse();
        perderAireBajoElAguaEsAhogarse();
        sinAireDentroDelAguaEsAhogarse();
        salirARespirarNoEsAhogarse();
        laLavaGanaAlResto();
        laPeleaCuentaSiNoHayLavaNiAhogo();
        elRadioCabeEnElAnillo();
        elRadioNoSeAmasaEnElBordeInterior();
        elRelojDuraQuinceSegundos();
        elNombreDeLaBandera();
        laVisitaSoloSaleSiLaAceptan();
        laVisitaCaducaYNoSeCuela();
        repetirLaVisitaNoLaSustituye();
        otraVisitaSustituyeALaAnterior();
        nadieSeVisitaASiMismo();
        laSalidaNoSeLlevaAQuienNoAcepta();
        laSalidaEsperaYLuegoSaleConQuienDijoQueSi();
        siNadieRespondeLaSalidaVaSola();
        quienSeVaCancelaSuSalidaYSusVisitas();
        unaVisitaApartaDeLaSalidaSinLlevarselo();
        laMuerteCuestaOchenta();
        sinMuerteNoSePide();
        conSetentaYNueveNoAlcanza();
        conOchentaSeEsperaYAlLlegarSeCobra();
        elDueloNoBorraLaMuerte();
        laXoNoBorraLaMuerte();
        morirOtraVezCambiaElSitio();
        llegarAOtraMuerteNoCobra();
        juntoNoEsLejos();
        elTextoDeLaMuerteVuelveALeerse();
        laCasaEsLaBandera();
        elPanelCabeDeUnaVez();
        System.out.println("pruebas de viajes: bien");
    }

    private static void nadarConAireNoEsAhogarse() {
        igual(null, Peligro.evaluar(false, true, true, 300, 300, false));
    }

    private static void perderAireBajoElAguaEsAhogarse() {
        igual(Peligro.Motivo.AHOGO, Peligro.evaluar(false, true, true, 299, 300, false));
    }

    private static void sinAireDentroDelAguaEsAhogarse() {
        igual(Peligro.Motivo.AHOGO, Peligro.evaluar(false, false, true, 0, 300, false));
    }

    private static void salirARespirarNoEsAhogarse() {
        igual(null, Peligro.evaluar(false, false, true, 200, 300, false));
    }

    private static void laLavaGanaAlResto() {
        igual(Peligro.Motivo.LAVA, Peligro.evaluar(true, true, true, 0, 300, true));
    }

    private static void laPeleaCuentaSiNoHayLavaNiAhogo() {
        igual(Peligro.Motivo.PELEA, Peligro.evaluar(false, false, false, 300, 300, true));
        igual(null, Peligro.evaluar(false, false, false, 300, 300, false));
    }

    private static void elRadioCabeEnElAnillo() {
        Random azar = new Random(26);
        for (int i = 0; i < 10000; i++) {
            double distancia = Radio.radio(azar, 1200, 49600);
            if (distancia < 1200 || distancia > 49600) {
                throw new IllegalStateException("radio fuera de rango: " + distancia);
            }
            double[] punto = Radio.punto(azar, 1200, 49600);
            double lejos = Math.hypot(punto[0], punto[1]);
            if (lejos < 1199.9 || lejos > 49600.1) {
                throw new IllegalStateException("punto fuera de rango: " + lejos);
            }
        }
    }

    private static void elRadioNoSeAmasaEnElBordeInterior() {
        Random azar = new Random(11);
        int cerca = 0;
        for (int i = 0; i < 10000; i++) {
            if (Radio.radio(azar, 1200, 49600) < 5000) {
                cerca++;
            }
        }
        if (cerca > 1500) {
            throw new IllegalStateException("demasiados puntos cerca del barrio: " + cerca);
        }
    }

    private static void elRelojDuraQuinceSegundos() {
        igualInt(15, Reloj.quedan(30, 0));
        igualInt(15, Reloj.quedan(30, 1));
        igualInt(14, Reloj.quedan(30, 2));
        igualInt(1, Reloj.quedan(30, 28));
        igualInt(1, Reloj.quedan(30, 29));
        igualInt(0, Reloj.quedan(30, 30));
    }

    private static void elNombreDeLaBandera() {
        igualTexto("Bandera", NombreBandera.elegir(null, List.of()));
        igualTexto("Bandera", NombreBandera.elegir("  ", List.of()));
        igualTexto("Bandera", NombreBandera.elegir("Bandera", List.of()));
        igualTexto("Bandera 2", NombreBandera.elegir("Bandera", List.of("Bandera")));
        igualTexto("Bandera 3", NombreBandera.elegir("bandera", List.of("Bandera", "Bandera 2")));
        igualTexto("Casa", NombreBandera.elegir("Casa", List.of("Bandera")));
        igualTexto("Casa", NombreBandera.elegir("§aCasa", List.of()));
        igualTexto("Bandera", NombreBandera.elegir("Bandera · 40 duros", List.of()));
        igualTexto("Bandera norte", NombreBandera.elegir("Bandera norte", List.of("Bandera")));
        igualTexto("abcdefghijklmnop", NombreBandera.elegir("abcdefghijklmnopqrstuvwxyz", List.of()));
        if (!NombreBandera.cabe(4) || NombreBandera.cabe(5)) {
            throw new IllegalStateException("el tope de banderas no cuadra");
        }
    }

    private static void laVisitaSoloSaleSiLaAceptan() {
        Consentir mesa = new Consentir();
        UUID pide = UUID.randomUUID();
        UUID responde = UUID.randomUUID();
        List<Consentir.Hecho> efectos = new ArrayList<>();
        if (mesa.pedirVisita(pide, responde, 0, efectos) == null || !efectos.isEmpty()) {
            throw new IllegalStateException("la visita no quedó pedida");
        }
        Consentir.Hecho aceptada = uno(mesa.aceptar(responde, 1), Consentir.Que.VISITA_ACEPTADA);
        if (!pide.equals(aceptada.pide) || !responde.equals(aceptada.responde)) {
            throw new IllegalStateException("la visita aceptada no une a quien pidió con quien acepta");
        }
        if (!mesa.aceptar(responde, 2).isEmpty()) {
            throw new IllegalStateException("aceptar otra vez no deja la petición cerrada");
        }
        mesa.pedirVisita(pide, responde, 3, efectos);
        Consentir.Hecho rechazada = uno(mesa.rechazar(responde, 4), Consentir.Que.VISITA_RECHAZADA);
        if (!pide.equals(rechazada.pide)) {
            throw new IllegalStateException("el rechazo no avisa a quien pidió");
        }
    }

    private static void laVisitaCaducaYNoSeCuela() {
        Consentir mesa = new Consentir();
        UUID pide = UUID.randomUUID();
        UUID responde = UUID.randomUUID();
        UUID otra = UUID.randomUUID();
        mesa.pedirVisita(pide, responde, 0, new ArrayList<>());
        mesa.pedirVisita(pide, otra, 0, new ArrayList<>());
        if (!mesa.caducar(Consentir.ESPERA - 1).isEmpty()) {
            throw new IllegalStateException("la visita caducó antes de tiempo");
        }
        List<Consentir.Hecho> tarde = mesa.aceptar(responde, Consentir.ESPERA);
        if (tarde.size() != 2) {
            throw new IllegalStateException("al caducar tenía que cerrar las dos visitas, no aceptar");
        }
        for (Consentir.Hecho hecho : tarde) {
            if (hecho.que != Consentir.Que.VISITA_CADUCADA) {
                throw new IllegalStateException("una visita caducada se aceptó: " + hecho.que);
            }
        }
        if (!mesa.aceptar(responde, Consentir.ESPERA + 1).isEmpty()) {
            throw new IllegalStateException("la visita caducada sigue viva");
        }
    }

    private static void repetirLaVisitaNoLaSustituye() {
        Consentir mesa = new Consentir();
        UUID pide = UUID.randomUUID();
        UUID responde = UUID.randomUUID();
        List<Consentir.Hecho> efectos = new ArrayList<>();
        mesa.pedirVisita(pide, responde, 0, efectos);
        mesa.pedirVisita(pide, responde, 1_000, efectos);
        if (!efectos.isEmpty()) {
            throw new IllegalStateException("repetir la visita la sustituyó");
        }
        if (!mesa.caducar(Consentir.ESPERA).isEmpty()) {
            throw new IllegalStateException("repetir la visita no alargó el plazo");
        }
        uno(mesa.caducar(1_000 + Consentir.ESPERA), Consentir.Que.VISITA_CADUCADA);
    }

    private static void otraVisitaSustituyeALaAnterior() {
        Consentir mesa = new Consentir();
        UUID primero = UUID.randomUUID();
        UUID segundo = UUID.randomUUID();
        UUID responde = UUID.randomUUID();
        List<Consentir.Hecho> efectos = new ArrayList<>();
        mesa.pedirVisita(primero, responde, 0, efectos);
        mesa.pedirVisita(segundo, responde, 10, efectos);
        Consentir.Hecho sustituida = uno(efectos, Consentir.Que.VISITA_SUSTITUIDA);
        if (!primero.equals(sustituida.pide) || !responde.equals(sustituida.responde)) {
            throw new IllegalStateException("no avisó a quien pidió antes");
        }
        Consentir.Hecho aceptada = uno(mesa.aceptar(responde, 11), Consentir.Que.VISITA_ACEPTADA);
        if (!segundo.equals(aceptada.pide)) {
            throw new IllegalStateException("aceptó a quien ya no estaba pidiendo");
        }
    }

    private static void nadieSeVisitaASiMismo() {
        Consentir mesa = new Consentir();
        UUID alguien = UUID.randomUUID();
        if (mesa.pedirVisita(alguien, alguien, 0, new ArrayList<>()) != null) {
            throw new IllegalStateException("dejó pedir una visita a uno mismo");
        }
        if (!mesa.aceptar(alguien, 1).isEmpty()) {
            throw new IllegalStateException("quedó una petición al visitarse");
        }
    }

    private static void laSalidaNoSeLlevaAQuienNoAcepta() {
        Consentir mesa = new Consentir();
        UUID lider = UUID.randomUUID();
        UUID si = UUID.randomUUID();
        UUID no = UUID.randomUUID();
        List<Consentir.Hecho> alta = mesa.pedirSalida(lider, List.of(si, no, lider, si), 0);
        if (!alta.isEmpty() || !mesa.tieneSalida(lider) || mesa.esperandoDe(lider).size() != 2) {
            throw new IllegalStateException("la salida no esperó a las dos personas");
        }
        uno(mesa.aceptar(si, 1), Consentir.Que.SALIDA_ACEPTADA);
        if (!mesa.tieneSalida(lider)) {
            throw new IllegalStateException("salió antes de que respondiera todo el mundo");
        }
        Consentir.Hecho listo = uno(mesa.rechazar(no, 2), Consentir.Que.GRUPO_LISTO);
        mismos(List.of(si), listo.aceptados);
        if (!listo.callados.isEmpty() || listo.aceptados.contains(no)) {
            throw new IllegalStateException("se llevó a quien dijo que no");
        }
        if (mesa.tieneSalida(lider)) {
            throw new IllegalStateException("la salida quedó abierta después de salir");
        }
    }

    private static void laSalidaEsperaYLuegoSaleConQuienDijoQueSi() {
        Consentir mesa = new Consentir();
        UUID lider = UUID.randomUUID();
        UUID si = UUID.randomUUID();
        UUID callado = UUID.randomUUID();
        mesa.pedirSalida(lider, List.of(si, callado), 0);
        uno(mesa.aceptar(si, 1), Consentir.Que.SALIDA_ACEPTADA);
        Consentir.Hecho listo = uno(mesa.caducar(Consentir.ESPERA), Consentir.Que.GRUPO_LISTO);
        mismos(List.of(si), listo.aceptados);
        mismos(List.of(callado), listo.callados);
        if (listo.responde != null) {
            throw new IllegalStateException("el silencio no es un rechazo pulsado");
        }
    }

    private static void siNadieRespondeLaSalidaVaSola() {
        Consentir mesa = new Consentir();
        UUID lider = UUID.randomUUID();
        UUID una = UUID.randomUUID();
        UUID otra = UUID.randomUUID();
        mesa.pedirSalida(lider, List.of(una, otra), 0);
        Consentir.Hecho listo = uno(mesa.caducar(Consentir.ESPERA), Consentir.Que.GRUPO_LISTO);
        if (!listo.aceptados.isEmpty()) {
            throw new IllegalStateException("se llevó a quien no respondió");
        }
        mismos(List.of(una, otra), listo.callados);
        Consentir mesaNueva = new Consentir();
        mesaNueva.pedirSalida(lider, List.of(una, otra), 0);
        int canceladas = 0;
        for (Consentir.Hecho hecho : mesaNueva.pedirSalida(lider, List.of(una), 5)) {
            if (hecho.que == Consentir.Que.SALIDA_CANCELADA) {
                canceladas++;
            }
        }
        if (canceladas != 2 || !mesaNueva.esperandoDe(lider).equals(List.of(una))) {
            throw new IllegalStateException("repetir la salida no sustituyó a la anterior");
        }
        Consentir.Hecho sola = uno(mesaNueva.rechazar(una, 6), Consentir.Que.GRUPO_LISTO);
        if (!sola.aceptados.isEmpty() || !una.equals(sola.responde)) {
            throw new IllegalStateException("un rechazo no dejó la salida vacía");
        }
    }

    private static void quienSeVaCancelaSuSalidaYSusVisitas() {
        Consentir mesa = new Consentir();
        UUID lider = UUID.randomUUID();
        UUID acepta = UUID.randomUUID();
        UUID espera = UUID.randomUUID();
        UUID visita = UUID.randomUUID();
        mesa.pedirSalida(lider, List.of(acepta, espera), 0);
        uno(mesa.aceptar(acepta, 1), Consentir.Que.SALIDA_ACEPTADA);
        mesa.pedirVisita(lider, visita, 2, new ArrayList<>());
        List<Consentir.Hecho> ida = mesa.salir(lider);
        if (mesa.tieneSalida(lider)) {
            throw new IllegalStateException("al irse la salida siguió viva");
        }
        int canceladas = 0;
        boolean retirada = false;
        for (Consentir.Hecho hecho : ida) {
            if (hecho.que == Consentir.Que.SALIDA_CANCELADA) {
                canceladas++;
            }
            if (hecho.que == Consentir.Que.VISITA_RETIRADA && visita.equals(hecho.responde)) {
                retirada = true;
            }
            if (hecho.que == Consentir.Que.GRUPO_LISTO) {
                throw new IllegalStateException("al irse el grupo salió solo");
            }
        }
        if (canceladas != 2 || !retirada) {
            throw new IllegalStateException("no canceló a quien venía ni la visita");
        }
        if (!mesa.aceptar(espera, 3).isEmpty() || !mesa.aceptar(visita, 3).isEmpty()) {
            throw new IllegalStateException("quedó una petición después de irse");
        }
        mesa.pedirSalida(lider, List.of(acepta, espera), 4);
        uno(mesa.aceptar(acepta, 5), Consentir.Que.SALIDA_ACEPTADA);
        Consentir.Hecho ausente = uno(mesa.salir(acepta), Consentir.Que.SALIDA_AUSENTE);
        if (!lider.equals(ausente.pide) || !mesa.tieneSalida(lider)) {
            throw new IllegalStateException("quien ya había aceptado no se apartó de la salida");
        }
        Consentir.Hecho listo = uno(mesa.rechazar(espera, 6), Consentir.Que.GRUPO_LISTO);
        if (!listo.aceptados.isEmpty()) {
            throw new IllegalStateException("se llevó a quien ya se había ido");
        }
    }

    private static void unaVisitaApartaDeLaSalidaSinLlevarselo() {
        Consentir mesa = new Consentir();
        UUID lider = UUID.randomUUID();
        UUID espera = UUID.randomUUID();
        UUID visita = UUID.randomUUID();
        mesa.pedirSalida(lider, List.of(espera), 0);
        List<Consentir.Hecho> efectos = new ArrayList<>();
        mesa.pedirVisita(visita, espera, 1, efectos);
        Consentir.Hecho listo = uno(efectos, Consentir.Que.GRUPO_LISTO);
        uno(efectos, Consentir.Que.SALIDA_APARTADA);
        if (!listo.aceptados.isEmpty() || mesa.tieneSalida(lider)) {
            throw new IllegalStateException("la visita se llevó a quien no había aceptado la salida");
        }
        Consentir.Hecho aceptada = uno(mesa.aceptar(espera, 2), Consentir.Que.VISITA_ACEPTADA);
        if (!visita.equals(aceptada.pide)) {
            throw new IllegalStateException("no aceptó la visita nueva");
        }
    }

    private static Consentir.Hecho uno(List<Consentir.Hecho> hechos, Consentir.Que que) {
        Consentir.Hecho hallado = null;
        int veces = 0;
        for (Consentir.Hecho hecho : hechos) {
            if (hecho.que == que) {
                hallado = hecho;
                veces++;
            }
        }
        if (veces != 1) {
            throw new IllegalStateException(que + " salió " + veces + " veces");
        }
        return hallado;
    }

    private static void mismos(List<UUID> esperado, List<UUID> real) {
        if (esperado.size() != real.size() || !new HashSet<>(esperado).equals(new HashSet<>(real))) {
            throw new IllegalStateException("esperaba " + esperado + " y fue " + real);
        }
    }

    private static void igualTexto(String esperado, String real) {
        if (!esperado.equals(real)) {
            throw new IllegalStateException("esperaba " + esperado + " y fue " + real);
        }
    }

    private static void igual(Peligro.Motivo esperado, Peligro.Motivo real) {
        if (esperado != real) {
            throw new IllegalStateException("esperaba " + esperado + " y fue " + real);
        }
    }

    private static void laMuerteCuestaOchenta() {
        igualInt(80, Huella.PRECIO);
        igualTexto("barrio", Huella.sitio("world"));
        igualTexto("nether", Huella.sitio("survival_nether"));
        igualTexto("end", Huella.sitio("fin"));
        igualTexto("survival", Huella.sitio("survival"));
    }

    private static void sinMuerteNoSePide() {
        if (Huella.pedir(null, 2000) != Huella.Paso.SIN_SITIO) {
            throw new IllegalStateException("sin muerte no se puede pedir");
        }
        if (Huella.llegar(null, null, 2000) != Huella.Paso.SIN_SITIO) {
            throw new IllegalStateException("sin muerte no se cobra al llegar");
        }
    }

    private static void conSetentaYNueveNoAlcanza() {
        Huella huella = new Huella("survival", 10, 64, -20);
        if (Huella.pedir(huella, 79) != Huella.Paso.SIN_DUROS) {
            throw new IllegalStateException("79 duros no tienen que alcanzar");
        }
        if (Huella.llegar(huella, huella, 79) != Huella.Paso.SIN_DUROS) {
            throw new IllegalStateException("al llegar, 79 duros no tienen que alcanzar");
        }
        if (Huella.pedir(huella, 25) != Huella.Paso.SIN_DUROS) {
            throw new IllegalStateException("la entrada no paga la muerte");
        }
        if (Huella.pedir(huella, 50) != Huella.Paso.SIN_DUROS) {
            throw new IllegalStateException("el préstamo sin aval no paga la muerte");
        }
        if (Huella.pedir(huella, 55) != Huella.Paso.SIN_DUROS) {
            throw new IllegalStateException("la segunda parcela no es más cara que la muerte");
        }
    }

    private static void conOchentaSeEsperaYAlLlegarSeCobra() {
        Huella huella = new Huella("survival", 10, 64, -20);
        if (Huella.pedir(huella, 80) != Huella.Paso.ESPERAR) {
            throw new IllegalStateException("80 duros tienen que dejar esperar");
        }
        if (Huella.llegar(huella, huella, 80) != Huella.Paso.COBRAR) {
            throw new IllegalStateException("al llegar, 80 duros tienen que cobrar");
        }
        if (Huella.pedir(huella, 2000) != Huella.Paso.ESPERAR) {
            throw new IllegalStateException("el techo de saldo tiene que poder pagar");
        }
    }

    private static void elDueloNoBorraLaMuerte() {
        Huella anterior = new Huella("survival", 4, 70, 8);
        Huella sigue = Huella.alMorir(anterior, "duelo", 0, 65, 0);
        if (sigue != anterior) {
            throw new IllegalStateException("el duelo no puede borrar la muerte");
        }
        if (Huella.alMorir(null, "duelo", 0, 65, 0) != null) {
            throw new IllegalStateException("morir en el duelo no guarda sitio");
        }
    }

    private static void laXoNoBorraLaMuerte() {
        Huella anterior = new Huella("fin", 0, 80, 0);
        if (Huella.alMorir(anterior, "xo", 1, 2, 3) != anterior) {
            throw new IllegalStateException("la XO no puede borrar la muerte");
        }
    }

    private static void morirOtraVezCambiaElSitio() {
        Huella primera = new Huella("survival", 1, 2, 3);
        Huella segunda = Huella.alMorir(primera, "survival_nether", 9, 40, -4);
        if (segunda == null || segunda.misma(primera) || !segunda.mundo.equals("survival_nether") || segunda.y != 40) {
            throw new IllegalStateException("la última muerte tiene que ser la nueva");
        }
        Huella barrio = Huella.alMorir(null, "world", 0, 66, 0);
        if (barrio == null || !barrio.mundo.equals("world")) {
            throw new IllegalStateException("morir en el barrio también cuenta");
        }
    }

    private static void llegarAOtraMuerteNoCobra() {
        Huella pedida = new Huella("survival", 1, 2, 3);
        Huella ahora = new Huella("survival", 8, 9, 10);
        if (Huella.llegar(pedida, ahora, 2000) != Huella.Paso.SIN_SITIO) {
            throw new IllegalStateException("una muerte nueva no se cobra como la anterior");
        }
        if (Huella.llegar(pedida, null, 2000) != Huella.Paso.SIN_SITIO) {
            throw new IllegalStateException("si la muerte se olvidó, no se cobra");
        }
    }

    private static void juntoNoEsLejos() {
        Huella huella = new Huella("survival", 10, 64, -20);
        if (!Huella.junto(huella, "survival", 10, 64, -20)) {
            throw new IllegalStateException("el mismo bloque está junto");
        }
        if (!Huella.junto(huella, "survival", 14, 64, -20)) {
            throw new IllegalStateException("cuatro bloques siguen junto");
        }
        if (Huella.junto(huella, "survival", 15, 64, -20)) {
            throw new IllegalStateException("cinco bloques ya no están junto");
        }
        if (Huella.junto(huella, "fin", 10, 64, -20)) {
            throw new IllegalStateException("otro mundo no está junto");
        }
        if (Huella.junto(null, "survival", 10, 64, -20)) {
            throw new IllegalStateException("sin muerte no se está junto");
        }
    }

    private static void elTextoDeLaMuerteVuelveALeerse() {
        Huella huella = new Huella("survival", 120, -20, -4000);
        Huella leida = Huella.leer(huella.texto());
        if (leida == null || !leida.misma(huella)) {
            throw new IllegalStateException("la muerte guardada no vuelve a leerse");
        }
        if (Huella.leer(null) != null || Huella.leer("") != null || Huella.leer("duelo,0,65,0") != null || Huella.leer("survival,1,2") != null) {
            throw new IllegalStateException("un texto roto no es una muerte");
        }
    }

    private static void laCasaEsLaBandera() {
        if (!Casa.es("/sethome") || !Casa.es("/home casa") || !Casa.es("/Essentials:DelHome") || !Casa.es("  /homes")) {
            throw new IllegalStateException("un comando de casa ha pasado");
        }
        if (Casa.es("/back") || Casa.es("/spawn") || Casa.es("/volver") || Casa.es("/bandera") || Casa.es(null) || Casa.es("sethome")) {
            throw new IllegalStateException("un viaje normal se ha leído como casa");
        }
    }

    private static void elPanelCabeDeUnaVez() {
        if (filas(11, 4) != 3 || filas(11, 2) < 6) {
            throw new IllegalStateException("los once botones tienen que caber en tres filas");
        }
        if (altoDe(11, 4) > 90) {
            throw new IllegalStateException("tres filas no caben en la pantalla");
        }
        String json = leerPanel();
        if (!json.contains("\"columns\": 4") || json.contains("\"body\"")) {
            throw new IllegalStateException("el panel de Escape lleva texto de más o no va a cuatro columnas");
        }
        if (json.contains("\"width\": 150") || json.contains("show_dialog")) {
            throw new IllegalStateException("un botón del panel es demasiado ancho o abre otro diálogo");
        }
        int desde = json.indexOf("\"actions\"");
        int hasta = json.indexOf("\"exit_action\"");
        String acciones = desde < 0 || hasta < desde ? "" : json.substring(desde, hasta);
        String[] etiquetas = {
            "Spawn", "Jugador", "Survival", "Muerte", "Eventos", "PVP",
            "Parcelas", "Tienda", "Bandera", "Banco", "Grupo"
        };
        int cursor = 0;
        for (String etiqueta : etiquetas) {
            int sitio = acciones.indexOf("\"text\": \"" + etiqueta + "\"", cursor);
            if (sitio < 0) {
                throw new IllegalStateException("falta el botón " + etiqueta);
            }
            cursor = sitio + etiqueta.length();
        }
        if (acciones.contains("\"text\": \"XO\"")) {
            throw new IllegalStateException("la XO tiene que estar dentro de Eventos");
        }
    }

    private static int filas(int botones, int columnas) {
        return (botones + columnas - 1) / columnas;
    }

    private static int altoDe(int botones, int columnas) {
        int filas = (botones + columnas - 1) / columnas;
        return filas * 22 + 14;
    }

    private static String leerPanel() {
        Path[] sitios = {
            Path.of("tools", "viajes", "panel.json"),
            Path.of("panel.json")
        };
        for (Path sitio : sitios) {
            if (Files.isRegularFile(sitio)) {
                try {
                    return Files.readString(sitio);
                } catch (Exception e) {
                    throw new IllegalStateException("no pude leer el panel");
                }
            }
        }
        throw new IllegalStateException("no está panel.json");
    }

    private static void igualInt(int esperado, int real) {
        if (esperado != real) {
            throw new IllegalStateException("esperaba " + esperado + " y fue " + real);
        }
    }
}
