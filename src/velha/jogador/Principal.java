package velha.jogador;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.util.Map;

import velha.comum.Args;
import velha.comum.Json;
import velha.comum.Rede;
import velha.match.ServidorMatch;

public final class Principal {
    private Principal() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> parametros = Args.ler(args);
        if (parametros.containsKey("ajuda")) {
            uso();
            return;
        }
        int nn = velha.match.Principal.nn(parametros);
        String protocolo = parametros.getOrDefault("protocolo", "tcp").toLowerCase();
        if (!protocolo.equals("tcp")) {
            System.out.println("Este jogador fala TCP. O pareamento usa o campo protocolo=tcp.");
            System.exit(1);
        }
        String rmiHost = parametros.getOrDefault("rmi-host", "127.0.0.1");
        String ip = parametros.containsKey("ip") ? parametros.get("ip") : velha.match.Principal.descobrirIp();
        int portaRmi = Rede.portaRmi(parametros, nn);
        String grupo = Rede.grupo(parametros, nn);
        int portaMulticast = Rede.portaMulticast(parametros, nn);
        int ttl = Rede.ttl(parametros);
        NetworkInterface interfaceSaida = Rede.interfaceEscolhida(parametros);
        // Em segundos. 0 = sem limite.
        int timeoutConexao = Args.inteiro(parametros, "timeout-conexao", 30);
        int timeoutJogada = Args.inteiro(parametros, "timeout-jogada", Partida.TIMEOUT_JOGADA_MS / 1000);

        // O socket ja escuta antes do RMI, para o oponente conseguir conectar
        // mesmo que a gente ainda esteja saindo do getOponent.
        int portaPedido = parametros.containsKey("porta") ? Integer.parseInt(parametros.get("porta")) : 0;
        ServerSocket escuta = new ServerSocket(portaPedido);
        Socket jogo = null;
        CanalMulticast canal = null;
        try {
            int portaLocal = escuta.getLocalPort();
            System.out.println("Anunciando " + ip + ":" + portaLocal + " protocolo tcp");
            System.out.println("Match em rmi://" + rmiHost + ":" + portaRmi + "/" + velha.match.Principal.NOME);

            ServidorMatch servidor;
            String resposta;
            try {
                servidor = (ServidorMatch) Naming.lookup(
                        "rmi://" + rmiHost + ":" + portaRmi + "/" + velha.match.Principal.NOME);
                System.out.println("Aguardando oponente...");
                resposta = servidor.getOponent(Json.pedido(ip, portaLocal, protocolo));
            } catch (Exception e) {
                System.out.println("Nao consegui falar com o servidor de match em "
                        + rmiHost + ":" + portaRmi + " (" + e.getMessage() + ").");
                return;
            }
            if (Json.tem(resposta, "erro")) {
                System.out.println("Sem partida: " + Json.texto(resposta, "erro"));
                return;
            }

            String ipOponente = Json.texto(resposta, "ip");
            int portaOponente = Json.inteiro(resposta, "porta");
            boolean inicia = Json.bool(resposta, "inicia");
            System.out.println("Oponente " + ipOponente + ":" + portaOponente
                    + (inicia ? " | voce conecta e comeca" : " | voce aceita e espera"));

            jogo = inicia ? conectar(ipOponente, portaOponente, escuta) : aceitar(escuta, timeoutConexao);
            canal = new CanalMulticast(grupo, portaMulticast, ttl, interfaceSaida);
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String fim = Partida.jogar(jogo, inicia, Partida.escolhaHumana(console, System.out), canal,
                    System.out, timeoutJogada * 1000);
            System.out.println("Partida encerrada: " + fim);
        } finally {
            if (jogo != null) {
                jogo.close();
            }
            if (canal != null) {
                canal.fechar();
            }
            escuta.close();
        }
    }

    private static Socket conectar(String ip, int porta, ServerSocket escuta) throws IOException, InterruptedException {
        escuta.close();
        IOException ultima = null;
        for (int tentativa = 1; tentativa <= 10; tentativa++) {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ip, porta), 3000);
                return socket;
            } catch (IOException e) {
                ultima = e;
                Thread.sleep(300L);
            }
        }
        throw ultima;
    }

    private static Socket aceitar(ServerSocket escuta, int timeoutSegundos) throws IOException {
        escuta.setSoTimeout(timeoutSegundos * 1000);
        return escuta.accept();
    }

    private static void uso() {
        System.out.println("Uso: java velha.jogador.Principal --nn NN [--rmi-host HOST] [--ip IP] [--porta PORTA]");
        System.out.println("       [--porta-rmi PORTA] [--grupo IP] [--porta-multicast PORTA] [--ttl N] [--interface NOME]");
        System.out.println("       [--timeout-conexao SEG] [--timeout-jogada SEG]   (0 = sem limite)");
        System.out.println("Sem parametros, consulta o match na porta 98NN e publica jogadas em 225.1.1.NN:88NN.");
    }
}
