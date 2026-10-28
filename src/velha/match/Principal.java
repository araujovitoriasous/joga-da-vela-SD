package velha.match;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;
import java.util.Map;

import velha.comum.Args;
import velha.comum.Rede;

public final class Principal {
    public static final String NOME = "ServidorMatch";
    public static final long ESPERA_PADRAO_MS = 120_000L;

    private Principal() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> parametros = Args.ler(args);
        if (parametros.containsKey("ajuda")) {
            uso();
            return;
        }
        int nn = nn(parametros);
        int porta = Rede.portaRmi(parametros, nn);
        String ip = parametros.containsKey("ip") ? parametros.get("ip") : descobrirIp();
        long espera = ESPERA_PADRAO_MS;
        if (parametros.containsKey("espera")) {
            espera = Long.parseLong(parametros.get("espera")) * 1000L;
        }

        // O stub RMI grava esse endereco. Tem que ser o IP que os jogadores alcancam.
        System.setProperty("java.rmi.server.hostname", ip);
        ServidorMatchImpl impl = new ServidorMatchImpl(espera);
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(porta);
        } catch (java.io.IOException e) {
            System.out.println("Nao abri o registry na porta " + porta
                    + ". Ela ja esta em uso? " + e.getMessage());
            return;
        }
        registry.rebind(NOME, impl);
        System.out.println("Servidor de match em rmi://" + ip + ":" + porta + "/" + NOME);
        System.out.println("Espera por um par: " + (espera / 1000) + "s. Ctrl+C encerra.");
        Thread.currentThread().join();
    }

    public static int nn(Map<String, String> parametros) {
        int valor = Integer.parseInt(Args.obrigatorio(parametros, "nn"));
        if (valor < 0 || valor > 99) {
            throw new IllegalArgumentException("--nn tem que ser de 0 a 99");
        }
        return valor;
    }

    public static String descobrirIp() throws Exception {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (!iface.isUp() || iface.isLoopback()) {
                continue;
            }
            Enumeration<InetAddress> enderecos = iface.getInetAddresses();
            while (enderecos.hasMoreElements()) {
                InetAddress endereco = enderecos.nextElement();
                if (endereco instanceof Inet4Address && !endereco.isLoopbackAddress()) {
                    return endereco.getHostAddress();
                }
            }
        }
        return "127.0.0.1";
    }

    private static void uso() {
        System.out.println("Uso: java velha.match.Principal --nn NN [--ip IP] [--porta-rmi PORTA] [--espera SEGUNDOS]");
        System.out.println("Sobe o RMIRegistry na porta 98NN (ou --porta-rmi) e publica ServidorMatch.");
    }
}
