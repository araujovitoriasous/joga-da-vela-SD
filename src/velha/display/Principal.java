package velha.display;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;

import velha.comum.Args;
import velha.jogo.Mensagem;
import velha.jogo.Tabuleiro;

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
        String endereco = "225.1.1." + nn;
        int porta = 8800 + nn;
        InetAddress grupo = InetAddress.getByName(endereco);

        MulticastSocket socket = new MulticastSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(porta));
        int interfaces = entrarNoGrupo(socket, grupo, porta);
        System.out.println("Display em " + endereco + ":" + porta
                + " (" + interfaces + " interfaces). Ctrl+C encerra.");

        byte[] buffer = new byte[2048];
        int ultimoNumero = -1;
        while (true) {
            DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
            socket.receive(pacote);
            String json = new String(pacote.getData(), pacote.getOffset(), pacote.getLength(), StandardCharsets.UTF_8).trim();
            // Dois jogadores publicam ao mesmo tempo. Um datagrama antigo
            // pode chegar depois; o tabuleiro do numero maior ja tem essa casa.
            ultimoNumero = exibir(json, ultimoNumero);
        }
    }

    public static int entrarNoGrupo(MulticastSocket socket, InetAddress grupo, int porta) throws Exception {
        int entrou = 0;
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (!iface.isUp() || !iface.supportsMulticast()) {
                continue;
            }
            try {
                socket.joinGroup(new InetSocketAddress(grupo, porta), iface);
                System.out.println("Escutando pela interface " + iface.getName());
                entrou++;
            } catch (Exception ignorada) {
                // essa interface nao entra no grupo; as outras seguem
            }
        }
        if (entrou == 0) {
            throw new java.io.IOException("Nenhuma interface entrou no grupo " + grupo.getHostAddress());
        }
        return entrou;
    }

    static int exibir(String json, int ultimoNumero) {
        try {
            Mensagem mensagem = Mensagem.parse(json);
            if (mensagem.numero < ultimoNumero) {
                return ultimoNumero;
            }
            System.out.println();
            System.out.println("Jogada " + mensagem.numero + " na casa " + mensagem.jogada
                    + " - " + texto(mensagem.resultado));
            System.out.print(Tabuleiro.desenho(mensagem.tabuleiro));
            return mensagem.numero;
        } catch (RuntimeException e) {
            System.out.println("Pacote ignorado: " + json);
            return ultimoNumero;
        }
    }

    private static String texto(String resultado) {
        if (Mensagem.VIT.equals(resultado)) {
            return "vitoria de quem jogou";
        }
        if (Mensagem.DER.equals(resultado)) {
            return "derrota aceita";
        }
        if (Mensagem.EMP.equals(resultado)) {
            return "empate";
        }
        return "em andamento";
    }

    private static void uso() {
        System.out.println("Uso: java velha.display.Principal --nn NN");
        System.out.println("Recebe jogadas em 225.1.1.NN na porta 88NN.");
    }
}
