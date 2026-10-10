package velha.comum;

/**
 * Leitor e escritor do JSON usado neste trabalho.
 * So cobre objeto plano, texto, inteiro, booleano e vetor de inteiros.
 */
public final class Json {
    private Json() {
    }

    public static String pedido(String ip, int porta, String protocolo) {
        return "{\"ip\":\"" + escapar(ip) + "\",\"porta\":" + porta
                + ",\"protocolo\":\"" + escapar(protocolo) + "\"}";
    }

    public static String resposta(String ip, int porta, boolean inicia) {
        return "{\"ip\":\"" + escapar(ip) + "\",\"porta\":" + porta
                + ",\"inicia\":" + inicia + "}";
    }

    public static String erro(String codigo) {
        return "{\"erro\":\"" + escapar(codigo) + "\"}";
    }

    public static String jogada(int numero, int casa, int[] tabuleiro, String resultado) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"Numero\":").append(numero);
        sb.append(",\"Jogada\":").append(casa);
        sb.append(",\"Tabuleiro\":").append(vetor(tabuleiro));
        sb.append(",\"Resultado\":\"").append(escapar(resultado)).append("\"}");
        return sb.toString();
    }

    public static boolean tem(String json, String campo) {
        return posicaoDoValor(json, campo) >= 0;
    }

    public static String texto(String json, String campo) {
        int i = exigir(json, campo);
        if (json.charAt(i) != '"') {
            throw new IllegalArgumentException("Campo " + campo + " nao e texto");
        }
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\' && i + 1 < json.length()) {
                sb.append(json.charAt(i + 1));
                i += 2;
                continue;
            }
            sb.append(c);
            i++;
        }
        throw new IllegalArgumentException("Texto sem fechamento: " + campo);
    }

    public static int inteiro(String json, String campo) {
        int i = exigir(json, campo);
        int fim = i;
        if (fim < json.length() && json.charAt(fim) == '-') {
            fim++;
        }
        int inicioDigitos = fim;
        while (fim < json.length() && Character.isDigit(json.charAt(fim))) {
            fim++;
        }
        if (fim == inicioDigitos) {
            throw new IllegalArgumentException("Campo " + campo + " nao e inteiro");
        }
        return Integer.parseInt(json.substring(i, fim));
    }

    public static boolean bool(String json, String campo) {
        int i = exigir(json, campo);
        if (json.startsWith("true", i)) {
            return true;
        }
        if (json.startsWith("false", i)) {
            return false;
        }
        throw new IllegalArgumentException("Campo " + campo + " nao e booleano");
    }

    public static int[] inteiros(String json, String campo) {
        int i = exigir(json, campo);
        if (json.charAt(i) != '[') {
            throw new IllegalArgumentException("Campo " + campo + " nao e vetor");
        }
        i++;
        int[] casas = new int[9];
        int n = 0;
        while (i < json.length()) {
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                i++;
            }
            if (i < json.length() && json.charAt(i) == ']') {
                if (n != 9) {
                    throw new IllegalArgumentException("Tabuleiro precisa ter 9 casas");
                }
                return casas;
            }
            int fim = i;
            if (fim < json.length() && json.charAt(fim) == '-') {
                fim++;
            }
            int inicio = fim;
            while (fim < json.length() && Character.isDigit(json.charAt(fim))) {
                fim++;
            }
            if (fim == inicio) {
                throw new IllegalArgumentException("Valor invalido no tabuleiro");
            }
            if (n >= 9) {
                throw new IllegalArgumentException("Tabuleiro tem casas demais");
            }
            casas[n++] = Integer.parseInt(json.substring(i, fim));
            i = fim;
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                i++;
            }
            if (i < json.length() && json.charAt(i) == ',') {
                i++;
            }
        }
        throw new IllegalArgumentException("Vetor sem fechamento");
    }

    private static String vetor(int[] valores) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(valores[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static int exigir(String json, String campo) {
        int i = posicaoDoValor(json, campo);
        if (i < 0) {
            throw new IllegalArgumentException("Campo ausente: " + campo);
        }
        return i;
    }

    private static int posicaoDoValor(String json, String campo) {
        String chave = "\"" + campo + "\"";
        int de = 0;
        while (de < json.length()) {
            int pos = json.indexOf(chave, de);
            if (pos < 0) {
                return -1;
            }
            int j = pos + chave.length();
            while (j < json.length() && Character.isWhitespace(json.charAt(j))) {
                j++;
            }
            if (j < json.length() && json.charAt(j) == ':') {
                j++;
                while (j < json.length() && Character.isWhitespace(json.charAt(j))) {
                    j++;
                }
                return j;
            }
            de = pos + 1;
        }
        return -1;
    }
}
