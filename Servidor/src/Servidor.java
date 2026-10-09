import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.CopyOnWriteArrayList;

public class Servidor {
    static int maxClientes = 3;
    static CopyOnWriteArrayList<Conexao> conexoes = new CopyOnWriteArrayList<>();
    static String imagemBase64;

    public static void main(String[] args) throws IOException {
        byte[] imagem = Files.readAllBytes(Paths.get("imagem_servidor.jpg"));
        imagemBase64 = Base64.getEncoder().encodeToString(imagem);
        System.out.println("Imagem carregada.");

        ServerSocket servidor = new ServerSocket(12345);
        System.out.println("Servidor iniciado.");
        System.out.println("Aguardando clientes...");

        while (true) {
            try {
                Socket socket = servidor.accept();
                PrintStream saida = new PrintStream(socket.getOutputStream(), true);

                if (conexoes.size() >= maxClientes) {
                    saida.println("ERRO: servidor cheio");
                    socket.close();
                    continue;
                }

                saida.println("OK: conectado");

                Conexao conexao = new Conexao(socket);
                conexoes.add(conexao);

                System.out.println(
                        "Cliente "
                        + socket.getInetAddress().getHostAddress()
                        + ":"
                        + socket.getPort()
                        + " conectado."
                );
                System.out.println(
                        "Clientes conectados: "
                        + Servidor.conexoes.size()
                        + "/"
                        + Servidor.maxClientes
                );

                System.out.println();
                conexao.iniciar();

            } catch (IOException e) {
                System.out.println("Erro no servidor: " + e.getMessage());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                }
            }
        }
    }
}