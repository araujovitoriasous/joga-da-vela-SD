# Jogo da velha distribuído

Trabalho de Sistemas Distribuídos. Três programas Java:

- o **servidor de match** pareia dois jogadores por RMI
- o **jogador** joga uma partida por TCP e publica cada lance em multicast
- o **display** recebe esses lances e imprime o tabuleiro

`NN` é o número da conta, de 0 a 99. O mesmo valor entra nos três programas. A porta do RMI é `98NN` e o grupo multicast é `225.1.1.NN` na porta `88NN`. Conta 7 usa a porta 9807 e o grupo `225.1.1.7:8807`.

Precisa de Java 17 ou mais novo.

## Compilar

```sh
sh compilar.sh
```

Os `.class` vão para `bin/`.

## Executar

Um terminal para cada programa. Troque os IPs pelos das máquinas.

```sh
# servidor de match
java -cp bin velha.match.Principal --nn 7 --ip 152.92.236.14

# display (pode abrir mais de um)
java -cp bin velha.display.Principal --nn 7

# dois jogadores
java -cp bin velha.jogador.Principal --nn 7 --rmi-host 152.92.236.14 --ip 152.92.236.16
java -cp bin velha.jogador.Principal --nn 7 --rmi-host 152.92.236.14 --ip 152.92.236.17
```

Na mesma máquina, use `--rmi-host 127.0.0.1` e `--ip 127.0.0.1`. `--porta` fixa a porta TCP que o jogador anuncia ao match.

Quem recebe `inicia: true` é o X, conecta e joga primeiro. O outro é o O e espera. As casas são digitadas de 0 a 8:

```text
0 | 1 | 2
3 | 4 | 5
6 | 7 | 8
```

Se ninguém entrar na fila em 120 segundos, o match responde `{"erro":"timeout"}`. Uma jogada sem resposta em 60 segundos encerra a partida.

Conferência automática, sem teclado:

```sh
java -cp bin velha.teste.TestePartida
```

## Mensagens

O método remoto é `String getOponent(String jsonIn)`.

```json
{"ip":"152.92.236.16","porta":50231,"protocolo":"tcp"}
{"ip":"152.92.236.17","porta":49811,"inicia":true}
```

Cada jogada é uma linha JSON no TCP. O mesmo texto segue no datagrama multicast.

```json
{"Numero":0,"Jogada":4,"Tabuleiro":[0,0,0,0,1,0,0,0,0],"Resultado":"jog"}
```

| Campo | Significado |
| --- | --- |
| `Numero` | índice da jogada, começa em 0 |
| `Jogada` | casa de 0 a 8 |
| `Tabuleiro` | 0 vazio, 1 = X, 2 = O |
| `Resultado` | `jog`, `vit`, `der` ou `emp` |

Jogada inválida volta a mesma linha, sem avançar o número. Vitória do oponente é aceita com `der`. Empate, com `emp`.

## Código

| Caminho | Função |
| --- | --- |
| `src/velha/match/` | fila de pareamento (`wait` / `notifyAll`) |
| `src/velha/jogador/` | partida TCP e envio multicast |
| `src/velha/display/` | recepção do grupo multicast |
| `src/velha/jogo/` | tabuleiro e mensagem |
| `web/index.html` | página do trabalho |

O pareamento está em `src/velha/match/ServidorMatchImpl.java`. O primeiro jogador de um protocolo fica bloqueado dentro de `getOponent`. O segundo completa o par e as duas chamadas retornam juntas.
