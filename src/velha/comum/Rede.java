package velha.comum;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Map;

/**
 * Enderecos e portas da rede. Sem parametro, valem as regras do NN:
 * RMI em 98NN, grupo 225.1.1.NN e porta multicast 88NN.
 * Cada valor pode ser trocado na linha de comando.
 */
public final class Rede {
    public static final int TTL_PADRAO = 1;

    private Rede() {
    }

    public static int portaRmi(Map<String, String> args, int nn) {
        return Args.inteiro(args, "porta-rmi", 9800 + nn);
    }

    public static String grupo(Map<String, String> args, int nn) {
        return Args.texto(args, "grupo", "225.1.1." + nn);
    }

    public static int portaMulticast(Map<String, String> args, int nn) {
        return Args.inteiro(args, "porta-multicast", 8800 + nn);
    }

    public static int ttl(Map<String, String> args) {
        int ttl = Args.inteiro(args, "ttl", TTL_PADRAO);
        if (ttl < 0 || ttl > 255) {
            throw new IllegalArgumentException("--ttl tem que ser de 0 a 255");
        }
        return ttl;
    }

    /** Interface pedida com --interface (nome como "en0" ou "eth0"). Sem o parametro, devolve null. */
    public static NetworkInterface interfaceEscolhida(Map<String, String> args) throws SocketException {
        String nome = args.get("interface");
        if (nome == null || nome.isEmpty()) {
            return null;
        }
        NetworkInterface iface = NetworkInterface.getByName(nome);
        if (iface == null) {
            throw new IllegalArgumentException("Nao existe a interface " + nome);
        }
        return iface;
    }
}
