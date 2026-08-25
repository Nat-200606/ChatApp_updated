package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static javax.swing.SwingConstants.CENTER;

public class MyFrame extends JFrame implements ActionListener {
        Font font = Font.createFont(Font.TRUETYPE_FONT, new File("Minecraft.ttf")).deriveFont(25f);

        JButton button;
        JTextArea mensagem;
        JTextArea historico;


        Socket socket;
        BufferedReader entrada;
        PrintWriter saida;

        static final int PORTA_DESCOBERTA = 12346;
        static final String PEDIDO_DESCOBERTA = "DISCOVER_CHAT_SERVER";
        static final String RESPOSTA_DESCOBERTA = "CHAT_SERVER_HERE";

        public MyFrame() throws IOException, FontFormatException {
                this.setLayout(null);
                this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                this.getContentPane().setBackground(Color.black);
                this.setResizable(false);
                this.setTitle("Chat");
                this.setLocation(360, 80);
                this.setSize(614,635);

                historico = new JTextArea();
                historico.setEditable(false);
                historico.setFont(font);
                historico.setLineWrap(true);
                historico.setWrapStyleWord(true);
                historico.setBackground(Color.LIGHT_GRAY);

                JScrollPane scrollHistorico = new JScrollPane(historico);
                scrollHistorico.setBounds(0, 0, 600, 400);
                this.add(scrollHistorico);

                mensagem = new JTextArea();
                mensagem.setFont(font);
                mensagem.setLineWrap(true);
                mensagem.setWrapStyleWord(true);

                JScrollPane scrollMensagem = new JScrollPane(mensagem);
                scrollMensagem.setBounds(0,400,400,200);
                scrollMensagem.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

                button = new JButton();
                ImageIcon buttonIcon = new ImageIcon("send_button.png");
                ImageIcon buttonPressedIcon = new ImageIcon("send_button_pressed.png");
                button.setIcon(buttonIcon);
                button.setPressedIcon(buttonPressedIcon);

                button.setBorderPainted(false);
                button.setHorizontalAlignment(CENTER);
                button.setHorizontalTextPosition(CENTER);
                button.setBackground(null);
                button.setBounds(400,400,200,200);
                button.setFocusable(false);
                button.setContentAreaFilled(false);
                button.addActionListener(this);


                this.add(scrollHistorico);
                this.add(scrollMensagem);
                this.add(button);
                this.setVisible(true);

                try {
                        String host = descobrirServidor();

                        if (host == null) {
                                historico.append("Servidor nao encontrado na rede.\n");
                                return;
                        }

                        int porta = 12345;
                        socket = new Socket(host, porta);

                        entrada = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));

                        saida = new PrintWriter(
                                socket.getOutputStream(), true);

                } catch (IOException e) {
                        e.printStackTrace();
                }



                Thread threadLeitura = new Thread(() -> {
                        String linha;
                        try {
                                while ((linha = entrada.readLine()) != null) {
                                        String mensagemRecebida = linha;
                                        SwingUtilities.invokeLater(() -> {
                                                historico.append("[recebida]" + mensagemRecebida + "\n");
                                        });
                                }
                        } catch (IOException ex) {
                                ex.printStackTrace();
                        }
                });
                threadLeitura.setDaemon(true); // encerra sozinha quando o programa fechar
                threadLeitura.start();
                
                        
                

        }

        // Manda um broadcast UDP na rede local perguntando quem eh o servidor de chat
        // e devolve o IP de quem responder. Retorna null se ninguem responder a tempo.
        private String descobrirServidor() {
                try (DatagramSocket socketDescoberta = new DatagramSocket()) {
                        socketDescoberta.setBroadcast(true);
                        socketDescoberta.setSoTimeout(3000); // espera ate 3s pela resposta

                        byte[] dadosPedido = PEDIDO_DESCOBERTA.getBytes();
                        DatagramPacket pacotePedido = new DatagramPacket(
                                dadosPedido, dadosPedido.length,
                                InetAddress.getByName("255.255.255.255"), PORTA_DESCOBERTA);

                        socketDescoberta.send(pacotePedido);

                        byte[] buffer = new byte[256];
                        DatagramPacket pacoteResposta = new DatagramPacket(buffer, buffer.length);
                        socketDescoberta.receive(pacoteResposta);

                        String resposta = new String(pacoteResposta.getData(), 0, pacoteResposta.getLength());

                        if (resposta.startsWith(RESPOSTA_DESCOBERTA)) {
                                return pacoteResposta.getAddress().getHostAddress();
                        }

                        return null;

                } catch (SocketTimeoutException e) {
                        return null; // ninguem respondeu dentro do tempo
                } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
                if (e.getSource() == button){
                        saida.println(mensagem.getText());
                        historico.append("[enviada]"+ mensagem.getText() + "\n");
                        mensagem.setText("");

                }
        }
}
