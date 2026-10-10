package velha.jogo;

import java.util.Arrays;

import velha.comum.Json;

public final class Mensagem {
    public static final String JOG = "jog";
    public static final String VIT = "vit";
    public static final String DER = "der";
    public static final String EMP = "emp";

    public final int numero;
    public final int jogada;
    public final int[] tabuleiro;
    public final String resultado;

    public Mensagem(int numero, int jogada, int[] tabuleiro, String resultado) {
        this.numero = numero;
        this.jogada = jogada;
        this.tabuleiro = Arrays.copyOf(tabuleiro, tabuleiro.length);
        this.resultado = resultado;
    }

    public String toJson() {
        return Json.jogada(numero, jogada, tabuleiro, resultado);
    }

    public static Mensagem parse(String json) {
        return new Mensagem(
                Json.inteiro(json, "Numero"),
                Json.inteiro(json, "Jogada"),
                Json.inteiros(json, "Tabuleiro"),
                Json.texto(json, "Resultado"));
    }

    public boolean igual(Mensagem outra) {
        return numero == outra.numero
                && jogada == outra.jogada
                && resultado.equals(outra.resultado)
                && Arrays.equals(tabuleiro, outra.tabuleiro);
    }
}
