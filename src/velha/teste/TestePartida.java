package velha.teste;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import velha.comum.Json;
import velha.jogador.CanalMulticast;
import velha.jogador.Partida;
import velha.jogo.Mensagem;
import velha.jogo.Tabuleiro;
import velha.match.ServidorMatch;
import velha.match.ServidorMatchImpl;

public final class TestePartida {
    private TestePartida() {
    }

    public static void main(String[] args) throws Exception {
        try {
            testarJson();
            testarTabuleiro();
            testarPareamento();
            testarVitoria();
            testarEmpate();
            testarJogadaRecusada();
            testarMulticast();
            testarRmi();
            System.out.println("OK");
            System.exit(0);
        } catch (Throwable erro) {
            erro.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static void testarJson() {
        String pedido = "{ \"ip\" : \"127.0.0.1\" , \"porta\" : 4000 , \"protocolo\" : \"tcp\" }";
        verificar("127.0.0.1".equals(Json.texto(pedido, "ip")), "ip");
        verificar(Json.inteiro(pedido, "porta") == 4000, "porta");
        verificar("tcp".equals(Json.texto(pedido, "protocolo")), "protocolo");
        String jogada = Json.jogada(0, 4, new int[] {0, 0, 0, 0, 1, 0, 0, 0, 0}, "jog");
        Mensagem mensagem = Mensagem.parse(jogada);
        verificar(mensagem.numero == 0 && mensagem.jogada == 4, "jogada");
        verificar(mensagem.tabuleiro[4] == 1, "casa central");
        verificar(Json.tem(Json.erro("timeout"), "erro"), "erro");
        verificar(Json.bool(Json.resposta("127.0.0.1", 9, true), "inicia"), "inicia");
    }

    private static void testarTabuleiro() {
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.marcar(0, Tabuleiro.X);
        tabuleiro.marcar(1, Tabuleiro.X);
        tabuleiro.marcar(2, Tabuleiro.X);
        verificar(tabuleiro.venceu(Tabuleiro.X), "linha de X");
        verificar(!tabuleiro.venceu(Tabuleiro.O), "O nao venceu");
        verificar(Tabuleiro.desenho(tabuleiro.copiar()).contains("X"), "desenho");
    }

    private static void testarPareamento() throws Exception {
        ServidorMatchImpl servidor = new ServidorMatchImpl(1000L);
        try {
            AtomicReference<String> primeiro = new AtomicReference<String>();
            Thread espera = new Thread(new Runnable() {
                @Override
                public void run() {
                    primeiro.set(servidor.getOponent(Json.pedido("127.0.0.1", 4100, "tcp")));
                }
            });
            espera.start();
            esperarBloqueio(espera);

            AtomicReference<String> udp = new AtomicReference<String>();
            Thread outroProtocolo = new Thread(new Runnable() {
                @Override
                public void run() {
                    udp.set(servidor.getOponent(Json.pedido("127.0.0.1", 4200, "udp")));
                }
            });
            outroProtocolo.start();
            esperarBloqueio(outroProtocolo);

            String segundo = servidor.getOponent(Json.pedido("127.0.0.2", 4101, "tcp"));
            espera.join(2000L);
            verificar(Json.bool(segundo, "inicia"), "segundo comeca");
            verificar("127.0.0.1".equals(Json.texto(segundo, "ip")), "ip do primeiro");
            verificar(Json.inteiro(segundo, "porta") == 4100, "porta do primeiro");
            verificar(!Json.bool(primeiro.get(), "inicia"), "primeiro espera");
            verificar("127.0.0.2".equals(Json.texto(primeiro.get(), "ip")), "ip do segundo");
            verificar(outroProtocolo.isAlive(), "udp nao entra no par tcp");

            outroProtocolo.join(2000L);
            verificar("timeout".equals(Json.texto(udp.get(), "erro")), "timeout do udp");
        } finally {
            UnicastRemoteObject.unexportObject(servidor, true);
        }
    }

    private static void testarVitoria() throws Exception {
        String[] fim = jogar(new int[] {0, 1, 2}, new int[] {3, 4});
        verificar("vitoria".equals(fim[0]), "X vence: " + fim[0]);
        verificar("derrota".equals(fim[1]), "O perde: " + fim[1]);
    }

    private static void testarEmpate() throws Exception {
        String[] fim = jogar(new int[] {0, 2, 3, 7, 8}, new int[] {1, 4, 5, 6});
        verificar("empate".equals(fim[0]), "X empata: " + fim[0]);
        verificar("empate".equals(fim[1]), "O empata: " + fim[1]);
    }

    private static void testarJogadaRecusada() throws Exception {
        List<String> publicados = Collections.synchronizedList(new ArrayList<String>());
        ServerSocket escuta = new ServerSocket(0);
        AtomicReference<String> fimO = new AtomicReference<String>();
        Thread oponente = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = escuta.accept();
                    fimO.set(Partida.jogar(socket, false, fila(4), publicarEm(publicados), silencio(), 3000));
                    socket.close();
                } catch (Exception e) {
                    fimO.set(e.toString());
                }
            }
        });
        oponente.start();
        Socket socket = new Socket("127.0.0.1", escuta.getLocalPort());
        BufferedWriter saida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        String ruim = Json.jogada(9, 0, new int[] {1, 0, 0, 0, 0, 0, 0, 0, 0}, Mensagem.JOG);
        saida.write(ruim);
        saida.write("\n");
        saida.flush();
        verificar(ruim.equals(entrada.readLine()), "eco da jogada invalida");

        String boa = Json.jogada(0, 0, new int[] {1, 0, 0, 0, 0, 0, 0, 0, 0}, Mensagem.JOG);
        saida.write(boa);
        saida.write("\n");
        saida.flush();
        Mensagem resposta = Mensagem.parse(entrada.readLine());
        verificar(resposta.numero == 1, "numero da resposta");
        verificar(resposta.jogada == 4, "O joga no centro");
        verificar(resposta.tabuleiro[0] == Tabuleiro.X, "X ficou na casa 0");
        verificar(resposta.tabuleiro[4] == Tabuleiro.O, "O ficou na casa 4");
        socket.close();
        oponente.join(4000L);
        verificar("desconexao".equals(fimO.get()) || "timeout".equals(fimO.get()), "fim apos queda: " + fimO.get());
        escuta.close();
    }

    private static void testarMulticast() throws Exception {
        String grupo = "225.1.1.90";
        int porta = 8890;
        MulticastSocket recepcao = new MulticastSocket(null);
        recepcao.setReuseAddress(true);
        recepcao.bind(new InetSocketAddress(porta));
        velha.display.Principal.entrarNoGrupo(recepcao, InetAddress.getByName(grupo), porta);
        recepcao.setSoTimeout(3000);

        String json = Json.jogada(0, 4, new int[] {0, 0, 0, 0, 1, 0, 0, 0, 0}, Mensagem.JOG);
        CanalMulticast canal = new CanalMulticast(grupo, porta);
        try {
            String recebido = null;
            for (int tentativa = 0; tentativa < 3 && recebido == null; tentativa++) {
                canal.publicar(json);
                try {
                    byte[] buffer = new byte[2048];
                    DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                    recepcao.receive(pacote);
                    recebido = new String(pacote.getData(), pacote.getOffset(), pacote.getLength(), StandardCharsets.UTF_8);
                } catch (java.net.SocketTimeoutException e) {
                    // o join no grupo as vezes nao vale para o primeiro datagrama
                }
            }
            verificar(json.equals(recebido), "multicast devolveu outra mensagem: " + recebido);
        } finally {
            canal.fechar();
            recepcao.close();
        }
    }

    private static void testarRmi() throws Exception {
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        int porta = portaLivre();
        ServidorMatchImpl impl = new ServidorMatchImpl(2000L);
        Registry registry = LocateRegistry.createRegistry(porta);
        registry.rebind("ServidorMatch", impl);
        ServidorMatch stub = (ServidorMatch) Naming.lookup("rmi://127.0.0.1:" + porta + "/ServidorMatch");

        AtomicReference<String> primeiro = new AtomicReference<String>();
        Thread espera = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    primeiro.set(stub.getOponent(Json.pedido("127.0.0.1", 4300, "tcp")));
                } catch (Exception e) {
                    primeiro.set(Json.erro(e.toString()));
                }
            }
        });
        espera.start();
        Thread.sleep(200L);
        String segundo = stub.getOponent(Json.pedido("127.0.0.2", 4301, "tcp"));
        espera.join(3000L);
        verificar(Json.bool(segundo, "inicia"), "rmi: segundo inicia");
        verificar(!Json.bool(primeiro.get(), "inicia"), "rmi: primeiro espera");
        registry.unbind("ServidorMatch");
        UnicastRemoteObject.unexportObject(impl, true);
    }

    private static String[] jogar(int[] casasX, int[] casasO) throws Exception {
        List<String> publicados = Collections.synchronizedList(new ArrayList<String>());
        ServerSocket escuta = new ServerSocket(0);
        AtomicReference<String> fimO = new AtomicReference<String>();
        Thread oponente = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = escuta.accept();
                    fimO.set(Partida.jogar(socket, false, fila(casasO), publicarEm(publicados), silencio(), 3000));
                    socket.close();
                } catch (Exception e) {
                    fimO.set(e.toString());
                }
            }
        });
        oponente.start();
        Socket socket = new Socket("127.0.0.1", escuta.getLocalPort());
        String fimX = Partida.jogar(socket, true, fila(casasX), publicarEm(publicados), silencio(), 3000);
        socket.close();
        oponente.join(4000L);
        escuta.close();
        verificar(!publicados.isEmpty(), "ninguem publicou a partida");
        return new String[] {fimX, fimO.get()};
    }

    private static Partida.Escolha fila(final int... casas) {
        return new Partida.Escolha() {
            private int indice;

            @Override
            public int casa(Tabuleiro tabuleiro) {
                if (indice >= casas.length) {
                    throw new IllegalStateException("acabaram as jogadas de teste");
                }
                return casas[indice++];
            }
        };
    }

    private static Partida.Publicador publicarEm(final List<String> destino) {
        return new Partida.Publicador() {
            @Override
            public void publicar(String json) {
                destino.add(json);
            }
        };
    }

    private static PrintStream silencio() {
        return new PrintStream(new ByteArrayOutputStream(), true);
    }

    private static void esperarBloqueio(Thread thread) throws InterruptedException {
        for (int i = 0; i < 50 && thread.getState() != Thread.State.TIMED_WAITING; i++) {
            Thread.sleep(20L);
        }
        verificar(thread.getState() == Thread.State.TIMED_WAITING, "thread nao ficou esperando oponente");
    }

    private static int portaLivre() throws Exception {
        ServerSocket socket = new ServerSocket(0);
        int porta = socket.getLocalPort();
        socket.close();
        return porta;
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }
}
