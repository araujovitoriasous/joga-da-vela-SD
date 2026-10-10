package velha.comum;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Args {
    private Args() {
    }

    public static Map<String, String> ler(String[] args) {
        Map<String, String> mapa = new LinkedHashMap<String, String>();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("--")) {
                continue;
            }
            String chave = args[i].substring(2);
            if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                mapa.put(chave, args[++i]);
            } else {
                mapa.put(chave, "true");
            }
        }
        return mapa;
    }

    public static String obrigatorio(Map<String, String> args, String chave) {
        String valor = args.get(chave);
        if (valor == null || valor.isEmpty()) {
            throw new IllegalArgumentException("Falta o argumento --" + chave);
        }
        return valor;
    }
}
