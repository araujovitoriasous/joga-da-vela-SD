package velha.jogador;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public final class CanalMulticast implements Partida.Publicador {
    private final InetAddress grupo;
    private final int porta;
    private final MulticastSocket socket;

    public CanalMulticast(String endereco, int porta) throws IOException {
        this.grupo = InetAddress.getByName(endereco);
        this.porta = porta;
        this.socket = new MulticastSocket();
        this.socket.setOption(StandardSocketOptions.IP_MULTICAST_TTL, Integer.valueOf(1));
        NetworkInterface saida = interfaceDeEnvio();
        if (saida != null) {
            this.socket.setOption(StandardSocketOptions.IP_MULTICAST_IF, saida);
        }
    }

    private static NetworkInterface interfaceDeEnvio() throws IOException {
        NetworkInterface reserva = null;
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (!iface.isUp() || !iface.supportsMulticast() || iface.isLoopback()) {
                continue;
            }
            String nome = iface.getName();
            if (nome.startsWith("utun") || nome.startsWith("awdl") || nome.startsWith("llw")) {
                continue;
            }
            boolean ipv4 = false;
            Enumeration<InetAddress> enderecos = iface.getInetAddresses();
            while (enderecos.hasMoreElements()) {
                if (enderecos.nextElement() instanceof Inet4Address) {
                    ipv4 = true;
                }
            }
            if (!ipv4) {
                continue;
            }
            if (nome.startsWith("en") || nome.startsWith("eth") || nome.startsWith("ens")) {
                return iface;
            }
            if (reserva == null) {
                reserva = iface;
            }
        }
        return reserva;
    }

    @Override
    public void publicar(String json) throws IOException {
        byte[] dados = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket pacote = new DatagramPacket(dados, dados.length, grupo, porta);
        socket.send(pacote);
    }

    public void fechar() {
        socket.close();
    }
}
