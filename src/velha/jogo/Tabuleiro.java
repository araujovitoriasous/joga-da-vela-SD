package velha.jogo;

import java.util.Arrays;

public final class Tabuleiro {
    public static final int VAZIO = 0;
    public static final int X = 1;
    public static final int O = 2;

    private static final int[][] LINHAS = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };

    private final int[] casas = new int[9];

    public boolean livre(int casa) {
        return casa >= 0 && casa <= 8 && casas[casa] == VAZIO;
    }

    public void marcar(int casa, int simbolo) {
        if (!livre(casa)) {
            throw new IllegalArgumentException("Casa indisponivel: " + casa);
        }
        if (simbolo != X && simbolo != O) {
            throw new IllegalArgumentException("Simbolo invalido: " + simbolo);
        }
        casas[casa] = simbolo;
    }

    public void desmarcar(int casa) {
        if (casa >= 0 && casa <= 8) {
            casas[casa] = VAZIO;
        }
    }

    public boolean venceu(int simbolo) {
        return venceu(casas, simbolo);
    }

    public boolean cheio() {
        return cheio(casas);
    }

    public int[] copiar() {
        return Arrays.copyOf(casas, 9);
    }

    public static boolean venceu(int[] casas, int simbolo) {
        for (int[] linha : LINHAS) {
            if (casas[linha[0]] == simbolo
                    && casas[linha[1]] == simbolo
                    && casas[linha[2]] == simbolo) {
                return true;
            }
        }
        return false;
    }

    public static boolean cheio(int[] casas) {
        for (int casa : casas) {
            if (casa == VAZIO) {
                return false;
            }
        }
        return true;
    }

    public static char simbolo(int valor) {
        if (valor == X) {
            return 'X';
        }
        if (valor == O) {
            return 'O';
        }
        return ' ';
    }

    public static String desenho(int[] casas) {
        StringBuilder sb = new StringBuilder();
        for (int linha = 0; linha < 3; linha++) {
            for (int coluna = 0; coluna < 3; coluna++) {
                int casa = linha * 3 + coluna;
                if (coluna > 0) {
                    sb.append('|');
                }
                sb.append(' ');
                sb.append(simbolo(casas[casa]));
                sb.append(' ');
            }
            sb.append('\n');
            if (linha < 2) {
                sb.append("---+---+---\n");
            }
        }
        return sb.toString();
    }

    public String desenho() {
        return desenho(casas);
    }
}
