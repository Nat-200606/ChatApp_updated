import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    static List<PrintWriter> clientes = new ArrayList<>();
    static final int PORTA_DESCOBERTA = 12346;
    static final String PEDIDO_DESCOBERTA = "DISCOVER_CHAT_SERVER";
    static final String RESPOSTA_DESCOBERTA = "CHAT_SERVER_HERE";

    public static void main(String[] args) {
        int porta = 12345;

        iniciarServicoDescoberta(porta);

        try {
            ServerSocket servidor = new ServerSocket(porta);
            System.out.println("Servidor iniciado na porta " + porta);
            System.out.println("Aguardando clientes...");

            while (true) {
                Socket cliente = servidor.accept();
                System.out.println("Cliente conectado: " + cliente.getInetAddress());
                new Thread(() -> tratarCliente(cliente)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Fica escutando pedidos de descoberta em UDP e responde com o IP do servidor,
    // assim o cliente acha o servidor sozinho sem precisar de IP fixo no código.
    static void iniciarServicoDescoberta(int portaChat) {
        Thread threadDescoberta = new Thread(() -> {
            try (DatagramSocket socketDescoberta = new DatagramSocket(PORTA_DESCOBERTA, InetAddress.getByName("0.0.0.0"))) {
                socketDescoberta.setBroadcast(true);
                byte[] buffer = new byte[256];

                System.out.println("Serviço de descoberta escutando na porta UDP " + PORTA_DESCOBERTA);

                while (true) {
                    DatagramPacket pacoteRecebido = new DatagramPacket(buffer, buffer.length);
                    socketDescoberta.receive(pacoteRecebido);

                    String mensagem = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength()).trim();

                    if (PEDIDO_DESCOBERTA.equals(mensagem)) {
                        String resposta = RESPOSTA_DESCOBERTA + ":" + portaChat;
                        byte[] dadosResposta = resposta.getBytes();

                        DatagramPacket pacoteResposta = new DatagramPacket(
                                dadosResposta, dadosResposta.length,
                                pacoteRecebido.getAddress(), pacoteRecebido.getPort());

                        socketDescoberta.send(pacoteResposta);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        threadDescoberta.setDaemon(true);
        threadDescoberta.start();
    }

    static void tratarCliente(Socket cliente) {
        PrintWriter saida = null;
        try {
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(cliente.getInputStream()));

            saida = new PrintWriter(cliente.getOutputStream(), true);

            synchronized (clientes) {
                clientes.add(saida);
            }

            String linha;
            while ((linha = entrada.readLine()) != null) {
                System.out.println("Recebido: " + linha);

                synchronized (clientes) {
                    for (PrintWriter outro : clientes) {
                        if (outro != saida) {
                            outro.println(linha);
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Cliente desconectou.");
        } finally {
            if (saida != null) {
                synchronized (clientes) {
                    clientes.remove(saida);
                }
            }
            try {
                cliente.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
