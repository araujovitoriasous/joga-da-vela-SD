package velha.jogador;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import velha.jogo.Mensagem;
import velha.jogo.Tabuleiro;

/**
 * Uma partida, depois que o socket com o oponente ja esta aberto.
 * Quem tem inicia=true joga primeiro e, neste projeto, tambem e quem conectou.
 */
public final class Partida {
    public static final int TIMEOUT_JOGADA_MS = 60_000;

    public interface Escolha {
        int casa(Tabuleiro tabuleiro) throws IOException;
    }

    public interface Publicador {
        void publicar(String json) throws IOException;
    }

    private Partida() {
    }

    public static String jogar(Socket socket, boolean inicia, Escolha escolha, Publicador publicador)
            throws IOException {
        return jogar(socket, inicia, escolha, publicador, System.out, TIMEOUT_JOGADA_MS);
    }

    public static String jogar(Socket socket, boolean inicia, Escolha escolha, Publicador publicador,
            PrintStream saida, int timeoutMs) throws IOException {
        socket.setTcpNoDelay(true);
        int meu = inicia ? Tabuleiro.X : Tabuleiro.O;
        int outro = inicia ? Tabuleiro.O : Tabuleiro.X;
        Tabuleiro tabuleiro = new Tabuleiro();
        int esperado = 0;
        boolean minhaVez = inicia;
        boolean esperandoAceite = false;

        BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter saidaSocket = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        Mensagem ultima = null;
        saida.println(inicia
                ? "Voce e X e comeca."
                : "Voce e O. Espere o oponente.");
        saida.println("Casas: 0 1 2 / 3 4 5 / 6 7 8");

        while (true) {
            if (minhaVez) {
                saida.print(tabuleiro.desenho());
                int casa = escolha.casa(tabuleiro);
                if (!tabuleiro.livre(casa)) {
                    saida.println("Casa invalida. Escolha outra.");
                    continue;
                }
                tabuleiro.marcar(casa, meu);
                String resultado = resultadoDaPosicao(tabuleiro, meu);
                ultima = new Mensagem(esperado, casa, tabuleiro.copiar(), resultado);
                enviar(saidaSocket, ultima.toJson());
                publicador.publicar(ultima.toJson());
                esperado++;
                minhaVez = false;
                esperandoAceite = !Mensagem.JOG.equals(resultado);
                if (esperandoAceite) {
                    saida.println(Mensagem.VIT.equals(resultado)
                            ? "Vitoria enviada. Esperando o aceite do oponente."
                            : "Empate enviado. Esperando o aceite do oponente.");
                }
                continue;
            }

            socket.setSoTimeout(timeoutMs);
            String linha;
            try {
                linha = entrada.readLine();
            } catch (SocketTimeoutException e) {
                saida.println("Tempo esgotado esperando o oponente.");
                return "timeout";
            }
            if (linha == null) {
                saida.println("Oponente desconectou.");
                return "desconexao";
            }
            linha = linha.trim();
            if (linha.isEmpty()) {
                continue;
            }

            Mensagem recebida;
            try {
                recebida = Mensagem.parse(linha);
            } catch (RuntimeException e) {
                saida.println("Mensagem ilegivel. Pedi para o oponente reenviar.");
                enviar(saidaSocket, linha);
                continue;
            }

            if (esperandoAceite) {
                if (Mensagem.VIT.equals(ultima.resultado) && Mensagem.DER.equals(recebida.resultado)) {
                    saida.println("Oponente aceitou. Voce venceu.");
                    return "vitoria";
                }
                // No empate o aceite repete a ultima mensagem. Nao e recusa.
                if (Mensagem.EMP.equals(ultima.resultado) && recebida.igual(ultima)) {
                    saida.println("Empate confirmado.");
                    return "empate";
                }
                if (recebida.igual(ultima)) {
                    tabuleiro.desmarcar(ultima.jogada);
                    esperado--;
                    esperandoAceite = false;
                    minhaVez = true;
                    saida.println("O oponente recusou o resultado. Jogue de novo.");
                    continue;
                }
                saida.println("Aceite inesperado. Encerrando.");
                return "desconexao";
            }

            if (ultima != null && recebida.igual(ultima)) {
                tabuleiro.desmarcar(ultima.jogada);
                esperado--;
                minhaVez = true;
                saida.println("O oponente recusou a jogada " + ultima.jogada
                        + ". Jogue de novo (mesmo numero " + esperado + ").");
                continue;
            }

            String erro = validarJogada(recebida, tabuleiro, outro, esperado);
            if (erro != null) {
                saida.println("Jogada recusada (" + erro + "). Avisei o oponente.");
                enviar(saidaSocket, linha);
                continue;
            }

            tabuleiro.marcar(recebida.jogada, outro);
            esperado++;

            // Se a partida continua, o tabuleiro sai no inicio da minha vez.
            // So desenho aqui quando ela acaba, para nao repetir.
            if (tabuleiro.venceu(outro) || Mensagem.VIT.equals(recebida.resultado)) {
                saida.print(tabuleiro.desenho());
                saida.println("Voce perdeu.");
                Mensagem aceite = new Mensagem(recebida.numero, recebida.jogada, tabuleiro.copiar(), Mensagem.DER);
                enviar(saidaSocket, aceite.toJson());
                return "derrota";
            }
            if (tabuleiro.cheio() || Mensagem.EMP.equals(recebida.resultado)) {
                saida.print(tabuleiro.desenho());
                saida.println("Empate.");
                Mensagem aceite = new Mensagem(recebida.numero, recebida.jogada, tabuleiro.copiar(), Mensagem.EMP);
                enviar(saidaSocket, aceite.toJson());
                return "empate";
            }
            minhaVez = true;
        }
    }

    public static Escolha escolhaHumana(final BufferedReader console, final PrintStream saida) {
        return new Escolha() {
            @Override
            public int casa(Tabuleiro tabuleiro) throws IOException {
                while (true) {
                    saida.print("Sua vez. Casa (0-8): ");
                    saida.flush();
                    String linha = console.readLine();
                    if (linha == null) {
                        throw new EOFException("entrada encerrada");
                    }
                    linha = linha.trim();
                    int casa;
                    try {
                        casa = Integer.parseInt(linha);
                    } catch (NumberFormatException e) {
                        saida.println("Digite um numero de 0 a 8.");
                        continue;
                    }
                    if (!tabuleiro.livre(casa)) {
                        saida.println("Essa casa nao pode ser jogada.");
                        continue;
                    }
                    return casa;
                }
            }
        };
    }

    private static String resultadoDaPosicao(Tabuleiro tabuleiro, int simbolo) {
        if (tabuleiro.venceu(simbolo)) {
            return Mensagem.VIT;
        }
        if (tabuleiro.cheio()) {
            return Mensagem.EMP;
        }
        return Mensagem.JOG;
    }

    private static String validarJogada(Mensagem msg, Tabuleiro tabuleiro, int simbolo, int esperado) {
        if (msg.numero != esperado) {
            return "numero " + msg.numero + ", esperava " + esperado;
        }
        if (!tabuleiro.livre(msg.jogada)) {
            return "casa " + msg.jogada + " indisponivel";
        }
        int[] previsto = tabuleiro.copiar();
        previsto[msg.jogada] = simbolo;
        if (!Arrays.equals(previsto, msg.tabuleiro)) {
            return "tabuleiro nao confere";
        }
        boolean vitoria = Tabuleiro.venceu(previsto, simbolo);
        boolean empate = !vitoria && Tabuleiro.cheio(previsto);
        if (Mensagem.VIT.equals(msg.resultado) && !vitoria) {
            return "anunciou vitoria sem ter vencido";
        }
        if (Mensagem.EMP.equals(msg.resultado) && !empate) {
            return "anunciou empate fora de hora";
        }
        if (Mensagem.DER.equals(msg.resultado)) {
            return "derrota no meio da partida";
        }
        if (!Mensagem.JOG.equals(msg.resultado)
                && !Mensagem.VIT.equals(msg.resultado)
                && !Mensagem.EMP.equals(msg.resultado)) {
            return "resultado desconhecido";
        }
        return null;
    }

    private static void enviar(BufferedWriter saida, String json) throws IOException {
        saida.write(json);
        saida.write("\n");
        saida.flush();
    }
}
