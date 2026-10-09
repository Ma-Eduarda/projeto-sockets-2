import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Scanner;

public class Cliente {

    public static void main(String[] args) throws IOException {
        try (Socket clienteSocket = new Socket("127.0.0.1", 12345)) {

            Scanner entrada = new Scanner(clienteSocket.getInputStream());
            PrintStream saida = new PrintStream(clienteSocket.getOutputStream(), true);

            String status = entrada.nextLine();
            if (status.startsWith("ERRO")) {
                System.out.println(status);
                entrada.close();
                saida.close();
                System.out.println("Cliente encerrado.");
                return;
            }

            System.out.println("Conectado ao servidor.");
            Scanner teclado = new Scanner(System.in);
            System.out.println();

            while (true) {
                System.out.println("Menu:");
                System.out.println("1 - Divisao");
                System.out.println("2 - Adicao");
                System.out.println("3 - Subtracao");
                System.out.println("4 - Pedir imagem ao servidor");
                System.out.println("0 - Encerrar");
                System.out.println();
                System.out.print("Opcao: ");
                if (!teclado.hasNextLine()) {
                    System.out.println("\nCONEXAO ENCERRADA!");
                    break;
                }

                String opcao = teclado.nextLine();

                String mensagem;
                if (opcao.equals("1")
                        || opcao.equals("2")
                        || opcao.equals("3")) {

                    System.out.println();
                    System.out.print("Digite o primeiro numero: ");
                    String numero1 = teclado.nextLine().trim().replace(",", ".");

                    System.out.print("Digite o segundo numero: ");
                    String numero2 = teclado.nextLine().trim().replace(",", ".");

                    mensagem = opcao + " " + numero1 + " " + numero2;

                } else if (opcao.equals("4")) {
                    mensagem = "4";

                } else if (opcao.equals("0")) {
                    mensagem = "0";

                } else {
                    System.out.println("Opcao invalida.");
                    System.out.println();
                    continue;
                }

                saida.println(mensagem);

                if (!entrada.hasNextLine()) {
                    System.out.println("Servidor encerrou a conexao.");
                    break;
                }

                String resposta = entrada.nextLine();
                if (opcao.equals("4") && !resposta.startsWith("ERRO")) {
                    Files.write(Paths.get("carameloPedreiro.jpg"), Base64.getDecoder().decode(resposta));

                    System.out.println("Imagem recebida!");
                    System.out.println();

                } else {
                    System.out.println("Resultado: " + resposta);
                    System.out.println();
                }

                if (opcao.equals("0")) {
                    break;
                }

                if (resposta.equals("ERRO: servidor cheio!")) {
                    break;
                }
            }

            entrada.close();
            saida.close();
            teclado.close();
            clienteSocket.close();
        } catch (IOException e) {
            System.out.println("Servidor desconectado!");
        } catch (java.util.NoSuchElementException e) {
            System.out.println("CONEXAO ENCERRADA!");
        }
    }
}
