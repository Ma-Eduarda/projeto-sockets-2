import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

public class Cliente {
    static final String IP_SERVIDOR = "127.0.0.1";
    static final String NOME_CLIENTE = "Cliente-Java";   

    public static void main(String[] args) throws IOException, InterruptedException {
        try (Socket clienteSocket = new Socket(IP_SERVIDOR, 12345)) {

            Scanner entrada = new Scanner(clienteSocket.getInputStream(), StandardCharsets.UTF_8);
            PrintStream saida = new PrintStream(clienteSocket.getOutputStream(), true, StandardCharsets.UTF_8);

            String status = entrada.nextLine();
            if (status.startsWith("ERRO")) {
                System.out.println(status);
                return;
            }
            Scanner teclado = new Scanner(System.in);

            saida.println("NOME " + NOME_CLIENTE);
            String respostaNome = entrada.nextLine();
            String meuNome = respostaNome.substring("NOME_OK ".length());
            System.out.println(" Conectado ao servidor como: " + meuNome);

            ArrayBlockingQueue<String> respostas = new ArrayBlockingQueue<>(50);

            Thread leitora = new Thread(() -> {
                try {
                    while (entrada.hasNextLine()) {
                        String linha = entrada.nextLine();
                        if (linha.startsWith("MSG ")) {
                            System.out.println();
                            System.out.println("Mensagem de " + linha.substring(4));
                        } else {
                            respostas.put(linha);
                        }
                    }
                    respostas.put("CONEXAO ENCERRADA!");
                } catch (Exception e) {
                    respostas.offer("CONEXAO ENCERRADA!");
                }
            });
            leitora.setDaemon(true);
            leitora.start();

            while (true) {
                System.out.println("");
                System.out.println("Menu:");
                System.out.println("1 - Divisao");
                System.out.println("2 - Adicao");
                System.out.println("3 - Subtracao");
                System.out.println("4 - Pedir imagem ao servidor");
                System.out.println("5 - Mandar mensagem para outro cliente");
                System.out.println("0 - Encerrar");
                System.out.println("");
                System.out.print("Opcao: ");
                String opcao = teclado.nextLine();

                String mensagem;
                if (opcao.equals("1") || opcao.equals("2") || opcao.equals("3")) {
                    System.out.print("Digite o primeiro numero: ");
                    String numero1 = teclado.nextLine().trim().replace(",", ".");
                    System.out.print("Digite o segundo numero: ");
                    String numero2 = teclado.nextLine().trim().replace(",", ".");
                    mensagem = opcao + " " + numero1 + " " + numero2;

                } else if (opcao.equals("4")) {
                    mensagem = "4";

                } else if (opcao.equals("5")) {
                    saida.println("5");
                    String lista = respostas.take();  
                    if (!lista.startsWith("OK:")) {
                        System.out.println("Servidor encerrou a conexao.");
                        break;
                    }
                    String texto = lista.substring(3).trim();
                    if (texto.isEmpty()) {
                        System.out.println("Nenhum outro cliente conectado.");
                        System.out.println();
                        continue;
                    }

                    String[] nomes = texto.split(", ");
                    System.out.println("Clientes conectados:");
                    for (int i = 0; i < nomes.length; i++) {
                        System.out.println((i + 1) + " - " + nomes[i]);
                    }
                    System.out.println("0 - Voltar");
                    System.out.print("Destinatario: ");
                    String escolha = teclado.nextLine().trim();
                    if (escolha.equals("0")) {
                        System.out.println();
                        continue;
                    }
                    int numero = 0;
                    try {
                        numero = Integer.parseInt(escolha);
                    } catch (NumberFormatException e) {
                    }
                    if (numero < 1 || numero > nomes.length) {
                        System.out.println("Opcao invalida.");
                        System.out.println();
                        continue;
                    }
                    String destino = nomes[numero - 1];

                    System.out.print("Mensagem para " + destino + ": ");
                    String conteudo = teclado.nextLine().trim();
                    if (conteudo.isEmpty()) {
                        System.out.println("Mensagem vazia, nada enviado.");
                        System.out.println();
                        continue;
                    }
                    mensagem = "5 " + destino + " " + conteudo;

                } else if (opcao.equals("0")) {
                    saida.println("0");
                    break;

                } else {
                    System.out.println("Opcao invalida.");
                    System.out.println();
                    continue;
                }

                saida.println(mensagem);
                String resposta = respostas.take();

                if (resposta.equals("CONEXAO ENCERRADA!")) {
                    System.out.println("Servidor encerrou a conexao.");
                    break;
                }

                if (opcao.equals("4") && !resposta.startsWith("ERRO")) {
                    Files.write(Paths.get("carameloPedreiro.jpg"), Base64.getDecoder().decode(resposta));
                    System.out.println("Imagem recebida!");

                } else if (opcao.equals("5")) {
                    if (resposta.startsWith("OK:")) {
                        System.out.println("Mensagem enviada.");
                    } else {
                        System.out.println(resposta);
                    }

                } else {
                    System.out.println("Resultado: " + resposta);
                }
            }

        } catch (IOException e) {
            System.out.println("Servidor desconectado. Conexao encerrada.");
        } catch (java.util.NoSuchElementException e) {
            System.out.println("CONEXAO ENCERRADA!");
        }
    }
}
