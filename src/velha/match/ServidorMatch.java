package velha.match;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Metodo unico do servidor de match, como no enunciado.
 * O JSON entra e sai como String.
 */
public interface ServidorMatch extends Remote {
    String getOponent(String jsonIn) throws RemoteException;
}
