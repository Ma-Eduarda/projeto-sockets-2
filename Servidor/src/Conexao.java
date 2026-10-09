import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

public class Conexao {

    Socket socket;
    ArrayBlockingQueue<String> fila = new ArrayBlockingQueue<>(50);

    public Conexao(Socket socket) {
        this.socket = socket;
    }

    public void iniciar() {

        Thread ler_do_socket = new Thread(() -> {
            try {
                Scanner entrada = new Scanner(socket.getInputStream());
                while (entrada.hasNextLine()) {
                    String mensagem = entrada.nextLine();
                    System.out.println("Mensagem recebida: " + mensagem);
                    if (mensagem.equals("0")) {
                        break;
                    }
                    fila.put(processarRequisicao(mensagem));
                }
                fila.put("CONEXAO ENCERRADA!");
            } catch (IOException | InterruptedException e) {
                fila.offer("CONEXAO ENCERRADA!");
            }
        });

        Thread escrever_no_socket = new Thread(() -> {
            try {
                PrintStream saida = new PrintStream(socket.getOutputStream(), true);
                String mensagem;
                do {
                    mensagem = fila.take();
                    if (mensagem.equals("CONEXAO ENCERRADA!")) {
                        Servidor.conexoes.remove(this);
                    }
                    saida.println(mensagem);
                } while (!mensagem.equals("CONEXAO ENCERRADA!"));
            } catch (IOException | InterruptedException e) {
                System.out.println("Erro na escrita: " + e.getMessage());
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

        System.out.println();
        System.out.println(
                "Cliente "
                + socket.getInetAddress().getHostAddress()
                + ":"
                + socket.getPort()
                + " saiu."
        );
        System.out.println(
                "Clientes conectados: "
                + Servidor.conexoes.size()
                + "/"
                + Servidor.maxClientes
        );
    }

    String formatarResultado(double valor) {
        String texto = String.valueOf(valor);
        if (texto.endsWith(".0")) {
            texto = texto.substring(0, texto.length() - 2);
        }
        return texto;
    }

    String processarRequisicao(String mensagem) {
        try {
            String[] partes = mensagem.trim().split("\\s+");
            String opcao = partes[0];

            if (opcao.equals("1")
                    || opcao.equals("2")
                    || opcao.equals("3")) {

                if (partes.length != 3) {
                    return "ERRO: mensagem invalida";
                }

                double numero1 = Double.parseDouble(partes[1]);
                double numero2 = Double.parseDouble(partes[2]);

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

            // Imagem
            if (opcao.equals("4")) {
                return Servidor.imagemBase64;
            }
            return "ERRO: opcao invalida";

        } catch (NumberFormatException e) {
            return "ERRO: mensagem invalida";
        }
    }
}