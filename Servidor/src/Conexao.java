import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

public class Conexao {

    Socket socket;
    ArrayBlockingQueue<String> fila = new ArrayBlockingQueue<>(50);
    volatile String nome = null;

    public Conexao(Socket socket) {
        this.socket = socket;
    }

    public void iniciar() {

        Thread ler_do_socket = new Thread(() -> {
            try {
                Scanner entrada = new Scanner(socket.getInputStream(), StandardCharsets.UTF_8);

                if (entrada.hasNextLine()) {
                    fila.put(registrarNome(entrada.nextLine()));
                }

                while (entrada.hasNextLine()) {
                    String mensagem = entrada.nextLine();
                    if (mensagem.equals("0")) {
                        break;
                    }
                    fila.put(processarRequisicao(mensagem));
                }
            } catch (IOException | InterruptedException e) {
            }

            fila.clear();
            fila.offer("CONEXAO ENCERRADA!");
        });

        Thread escrever_no_socket = new Thread(() -> {
            try {
                PrintStream saida = new PrintStream(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                String mensagem;
                do {
                    mensagem = fila.take();
                    saida.println(mensagem);
                } while (!mensagem.equals("CONEXAO ENCERRADA!"));
            } catch (IOException | InterruptedException e) {
            } finally {
                fecharConexao();
            }
        });

        ler_do_socket.start();
        escrever_no_socket.start();
    }

    void fecharConexao() {
        try {
            socket.close();
        } catch (IOException e) {
        }
        Servidor.conexoes.remove(this);
        System.out.println("");
        System.out.println("Cliente " + nome + " saiu (" + Servidor.conexoes.size() + "/" + Servidor.maxClientes + ")");
    }

    String formatarResultado(double valor) {
        String texto = String.valueOf(valor);
        if (texto.endsWith(".0")) {
            texto = texto.substring(0, texto.length() - 2);
        }
        return texto;
    }

    String registrarNome(String mensagem) {
        String[] partes = mensagem.trim().split("\\s+", 2);
        String addNome = (partes.length == 2) ? partes[1].trim().replaceAll("[\\s,]+", "-") : "Cliente";
        this.nome = addNome;
        System.out.println("Nome registrado: " + nome);
        return "NOME_OK " + nome;
    }

    Conexao encontrarPorNome(String nomeProcurado) {
        for (Conexao c : Servidor.conexoes) {
            if (c.nome != null && c.nome.equalsIgnoreCase(nomeProcurado)) {
                return c;
            }
        }
        return null;
    }

    String processarRequisicao(String mensagem) {
        try {
            String[] partes = mensagem.trim().split("\\s+");
            String opcao = partes[0];

            // 1 = divisao, 2 = adicao, 3 = subtracao
            if (opcao.equals("1") || opcao.equals("2") || opcao.equals("3")) {
                if (partes.length != 3) {
                    return "ERRO: mensagem invalida";
                }
                double numero1 = Double.parseDouble(partes[1]);
                double numero2 = Double.parseDouble(partes[2]);
                System.out.println("");
                System.out.println(nome + " opcao: " + mensagem);

                if (opcao.equals("1")) {
                    if (numero2 == 0) {
                        return "ERRO: divisao por zero";
                    }
                    return formatarResultado(numero1 / numero2);
                }
                if (opcao.equals("2")) {
                    return formatarResultado(numero1 + numero2);
                }
                return formatarResultado(numero1 - numero2);
            }

            // 4 = imagem
            if (opcao.equals("4")) {
                System.out.println("");
                System.out.println(nome + " pediu a imagem");
                return Servidor.imagemBase64;
            }

            // 5 = mensagem para outro cliente: "5 <nomeDestino> <texto...>"
            if (opcao.equals("5")) {
                String[] p = mensagem.trim().split("\\s+", 3);

                if (p.length == 1) {
                    String nomes = "";
                    for (Conexao c : Servidor.conexoes) {
                        if (c != this && c.nome != null) {
                            nomes += (nomes.isEmpty() ? "" : ", ") + c.nome;
                        }
                    }
                    return "OK: " + nomes;
                }
                if (p.length < 3) {
                    return "ERRO: use 5 <nome> <mensagem>";
                }
                Conexao destino = encontrarPorNome(p[1]);
                if (destino == null) {
                    return "ERRO: cliente '" + p[1] + "' nao encontrado";
                }
                if (!destino.fila.offer("MSG " + nome + ": " + p[2])) {
                    return "ERRO: destinatario indisponivel";
                }
                System.out.println("");
                System.out.println(nome + " enviou mensagem para " + destino.nome + ": " + p[2]);
                return "OK: mensagem enviada para " + destino.nome;
            }

            return "ERRO: opcao invalida";

        } catch (NumberFormatException e) {
            return "ERRO: mensagem invalida";
        }
    }
}
