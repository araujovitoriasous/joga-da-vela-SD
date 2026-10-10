package velha.match;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import velha.comum.Json;

/**
 * Pareia dois jogadores do mesmo protocolo.
 *
 * Cada getOponent chega numa thread do RMI. O primeiro jogador fica preso em
 * wait(), dentro da propria chamada. Quando o segundo chega, os dois recebem
 * o return: um com inicia=true e o outro com inicia=false. Os dois saem da fila.
 */
public class ServidorMatchImpl extends UnicastRemoteObject implements ServidorMatch {
    private static final long serialVersionUID = 1L;

    private final long esperaMs;
    private final Map<String, ArrayDeque<Espera>> filas = new HashMap<String, ArrayDeque<Espera>>();

    public ServidorMatchImpl(long esperaMs) throws RemoteException {
        super();
        this.esperaMs = esperaMs;
    }

    @Override
    public synchronized String getOponent(String jsonIn) {
        String ip;
        int porta;
        String protocolo;
        try {
            ip = Json.texto(jsonIn, "ip");
            porta = Json.inteiro(jsonIn, "porta");
            protocolo = Json.texto(jsonIn, "protocolo").toLowerCase();
        } catch (RuntimeException e) {
            return Json.erro("json invalido");
        }
        if (ip.isEmpty() || porta < 1 || porta > 65535) {
            return Json.erro("endereco invalido");
        }
        if (!protocolo.equals("tcp") && !protocolo.equals("udp")) {
            return Json.erro("protocolo invalido");
        }

        ArrayDeque<Espera> fila = filas.get(protocolo);
        if (fila == null) {
            fila = new ArrayDeque<Espera>();
            filas.put(protocolo, fila);
        }

        if (!fila.isEmpty()) {
            Espera outro = fila.removeFirst();
            outro.resposta = Json.resposta(ip, porta, false);
            outro.pronto = true;
            notifyAll();
            System.out.println("[match] par " + protocolo + " "
                    + outro.ip + ":" + outro.porta + " (espera) x "
                    + ip + ":" + porta + " (comeca)");
            return Json.resposta(outro.ip, outro.porta, true);
        }

        Espera eu = new Espera(ip, porta);
        fila.addLast(eu);
        System.out.println("[match] na fila " + protocolo + " " + ip + ":" + porta);
        long limite = System.currentTimeMillis() + esperaMs;
        while (!eu.pronto) {
            long resta = limite - System.currentTimeMillis();
            if (resta <= 0) {
                fila.remove(eu);
                System.out.println("[match] timeout " + ip + ":" + porta);
                return Json.erro("timeout");
            }
            try {
                wait(resta);
            } catch (InterruptedException e) {
                if (eu.pronto) {
                    Thread.currentThread().interrupt();
                    return eu.resposta;
                }
                fila.remove(eu);
                Thread.currentThread().interrupt();
                return Json.erro("interrompido");
            }
        }
        return eu.resposta;
    }

    private static final class Espera {
        final String ip;
        final int porta;
        String resposta;
        boolean pronto;

        Espera(String ip, int porta) {
            this.ip = ip;
            this.porta = porta;
        }
    }
}
