import java.io.*; // classes para input e output streams e
import java.net.*;// DatagramaSocket,InetAddress,DatagramaPacket
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class UDPClient {
    private static DatagramSocket clientSocket;
    private static int[] ACKArray;
    private static int lastACKReceived;



    public static void main(String args[]) throws Exception {
        clientSocket = new DatagramSocket();
        Scanner sc= new Scanner(System.in);

        //le o arquivo e armazena na estrutura criada
        readFile(sc.nextLine());

        ACKArray = new int[DataPackage.getInstance().getTotalPackages()];

        int dobra = 1;
        int inicia;
        lastACKReceived = 1;

        //fica "escutando" até enviar todos os pacotes requisitados
        while (true) {
            System.out.println("last ack received: " + lastACKReceived);

            if ((lastACKReceived) - 1 == ACKArray.length || lastACKReceived == 00) {
                System.out.println("Recebi confirmacao que todos os pacotes foram enviados, encerrando programa");
                break;
            }

            //se algum pacote recebeu mais de 3 acks, troca o inicio para a posicao do ACK com +3 pedidos e o multiplicador para 1
            if (has3ACKS() != -1) {
                System.out.println("recebi 3 ACKS ");

                System.out.println("pacArray: ");
                int count = 2;
                for (int i : ACKArray) {
                    System.out.print(count + " | ");
                    count++;
                }

                System.out.println("\n ACKArray: ");
                for (int i : ACKArray) {
                    System.out.print(i + " | ");
                }
                inicia = has3ACKS() + 1;
                dobra = 1;
                ACKArray[has3ACKS()] = 0;
            } else {
                System.out.println("Nenhum ACK tem mais que 3 pedidos");
                inicia = lastACKReceived - 1;
            }
            System.out.println("\n inicia: " + inicia);
            System.out.println("dobra: " + dobra);

            //manda o pacote
            normalSendDataInit(inicia, dobra);



            System.out.println("===================== ACK ZONE =====================");
            try {
                clientSocket.setSoTimeout(1000);
                while (true) {
                    handleACK();
                }
            } catch (Exception e) {
                System.out.println("Excecao no ack, nao consegui capturar o ACK");
            }

            System.out.println("===================== FIM ACK ZONE =====================");

            //modifica o multiplicador
            dobra *= 2;
        }
    }

    public static void readFile(String namefile) throws IOException {
        Path fileLocation = Paths.get(namefile);
        DataPackage.getInstance().setData(Files.readAllBytes(fileLocation));
        DataPackage.getInstance().splitData();

    }

    //metodo utilizado pelo slow start
    public static void sendData(byte[] data) throws Exception {
        // obtem endere�o IP do servidor com o DNS
        InetAddress IPAddress = InetAddress.getByName("localhost");

        // cria pacote com o dado, o endere�o do server e porta do servidor
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, 9800);
        System.out.println("Ta enviando o pacote: " + bytesToShort(new byte[]{data[0],data[1]}));

        //envia o pacote
        clientSocket.send(sendPacket);
    }

    //slowStart
    public static void normalSendDataInit(int comeca, int tamanho) throws Exception {
        System.out.println("=============== SEND PACKET ZONE =============== ");
        for (int i = comeca; i < comeca + tamanho; i++) {
            if (DataPackage.getInstance().getSplittedData().size() > i) {
                sendData(DataPackage.getInstance().getSplittedData().get(i).getData());
            }
        }
        System.out.println("=============== FIM SEND PACKET ZONE =============== ");
    }

    //recebe o ACK e armazena no array somando +1
    public static void handleACK() throws Exception {
        byte[] receiveData = new byte[512];

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        byte[] array = receivePacket.getData();

        for (int i = 0; i < array.length; i++) {
            if (array[i] == 0) {
                array = Arrays.copyOfRange(array, 0, i);
            }
        }
        int pos = Integer.parseInt(new String(array));
        System.out.println("recebendo ack: " + pos);
        if (lastACKReceived < pos) {
            lastACKReceived = pos;
        }
        if (pos == 0) {
            lastACKReceived = 0;
        }
        ACKArray[pos - 2] = (ACKArray[pos - 2]) + 1;
    }

    //retorna a posicao com +3 ACKS
    public static int has3ACKS() {
        for (int i = 0; i < ACKArray.length; i++) {
            if (ACKArray[i] >= 3) {
                return i;
            }
        }
        return -1;
    }

    public static short bytesToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }
}
