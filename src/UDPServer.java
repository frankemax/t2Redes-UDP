import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.zip.CRC32;

public class UDPServer {
    private static DatagramPacket receivePacket;
    private static DatagramSocket serverSocket;
    private static miniDataPackage[] splittedData;
    private static int lastACK;

    public static void main(String args[]) throws Exception {
        serverSocket = new DatagramSocket(9800);

        byte[] receiveData = new byte[512];
        lastACK = 1;
        boolean primeiro = true;

        //fica "escutando" até receber todos os pacotes necessarios
        while (true) {
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            //quando o primeiro pacote chegar, instancia as estruturas necessárias
            if (primeiro) {
                byte[] aux = Arrays.copyOfRange(receivePacket.getData(), 10, 14);
                splittedData = new miniDataPackage[Integer.parseInt(new String(aux))];
                primeiro = false;
            }
            System.out.println("Recebi o pacote: " + getIndice(receivePacket.getData()));

            //adiciona o package, se o crc for correto
            popula(receivePacket);

            //se o pacote que eu recebi é o ACK que enviei, entao atualizo o ACK a ser enviado.
            if (lastACK == getIndice(receivePacket.getData())) {
                refreshLastACK();
            }

            //mando o ACK
            sendACK();

            //se o ACK que eu enviei foi o ultimo pacote +1, encerra a conexão
            if (lastACK == splittedData.length + 1) {
                break;
            }
            System.out.println("===============");

        }


        //se o ACK que eu enviei o de encerrar conexão (00): encerra a conexão
        closeConnection();

        //escreve um arquivo com os dados recebidos
        escreveArquivo();

        //verifica se o arquivo recebido é igual ao arquivo que foi enviado
        checaArquivo();

    }

    //verifica se os arquivos sao iguais ou diferentes
    public static void checaArquivo() throws Exception {
        File fileIn = new File("file.txt");
        File fileOut = new File("fileUDPOut.txt");

        MessageDigest md5 = MessageDigest.getInstance("MD5");

        String in = getFileChecksum(md5, fileIn);
        String out = getFileChecksum(md5, fileOut);

        if (in.equals(out)) {
            System.out.println("Os arquivos são iguais");
        } else System.out.println("Os arquivos são diferentes");
    }

    //atualizo o LastACK com o primeiro pacote que eu não possuo
    public static void refreshLastACK() {
        for (int i = 0; i < splittedData.length; i++) {
            if (splittedData[i] == null) {
                lastACK = i + 1;
                break;
            }
            if (i == splittedData.length - 1) {
                lastACK = splittedData.length + 1;
            }
        }
    }

    //bombardeia o cliente com mensagens de encerrar conexão
    public static void closeConnection() throws Exception {
        InetAddress IPAddress = receivePacket.getAddress();
        int port = receivePacket.getPort();
        String s = "00";
        byte[] ACKByte = s.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(ACKByte, ACKByte.length, IPAddress, port);
        System.out.println("mandei o ack: " + s);
        serverSocket.send(sendPacket);
        serverSocket.send(sendPacket);
        serverSocket.send(sendPacket);
        serverSocket.send(sendPacket);
        serverSocket.send(sendPacket);
        serverSocket.send(sendPacket);
        serverSocket.send(sendPacket);
        serverSocket.send(sendPacket);
        serverSocket.send(sendPacket);
        serverSocket.send(sendPacket);

    }

    //manda o ack atual
    public static void sendACK() throws Exception {
        InetAddress IPAddress = receivePacket.getAddress();
        int port = receivePacket.getPort();
        String s = lastACK + "";
        byte[] ACKByte = s.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(ACKByte, ACKByte.length, IPAddress, port);
        System.out.println("mandei o ack: " + s);
        serverSocket.send(sendPacket);

    }

    //percorre o vetor dos pacotes que eu recebi e escreve no arquivo
    public static void escreveArquivo() throws Exception {
        FileOutputStream f1 = new FileOutputStream(new File("fileUDPOut.txt"), false /* append = true */);
        PrintWriter printWriter = new PrintWriter(f1);
        String s = "";
        String a = "";

        for (int i = 0; i < splittedData.length - 1; i++) {
            a = new String(splittedData[i].getData());
            System.out.println("escrevi: " + a);
            s = s + a;

        }

        byte[] array = splittedData[splittedData.length - 1].getData();

        for (int y = 0; y < array.length; y++) {
            if (array[y] == 0) {
                array = Arrays.copyOfRange(array, 0, y);

                a = new String(array);
                System.out.println("ultimo: " + a);
                s = s + a;
                break;

            }
        }

        printWriter.print(s);
        printWriter.close();

    }

    //adiciona o pacote no vetor de dados
    public static void popula(DatagramPacket dp) {
        int posicao = getIndice(dp.getData());
        //se o crc foi correto:
        if (checkCRC(dp)) {
            splittedData[posicao - 1] = new miniDataPackage(getData(dp.getData()));
            System.out.println("CheckCRC true, adicionando pacote no array");

        }
    }

    //verifica o crc de cada pacote
    public static boolean checkCRC(DatagramPacket receivePacket) {
        byte[] packet = receivePacket.getData();
        byte[] packetOld = Arrays.copyOfRange(packet, 2, 10);

        for (int i = 2; i < 10; i++) {
            packet[i] = (byte) 48;
        }

        CRC32 crc32 = new CRC32();
        crc32.update(packet, 0, packet.length);
        long old = crc32.getValue();
        long old2 = bytesToLong(packetOld, 0);
        if (old == old2) {
            return true;
        }
        return false;
    }

    public static int getIndice(byte[] data) {
        short index = bytesToShort(new byte[]{data[0], data[1]});

        return index;
    }

    //retorna a parte do pacote que contem a data
    public static byte[] getData(byte[] sentence) {
        byte[] array = Arrays.copyOfRange(sentence, 14, sentence.length);

        return array;
    }

    public static long bytesToLong(final byte[] bytes, final int offset) {
        long result = 0;
        for (int i = offset; i < Long.BYTES + offset; i++) {
            result <<= Long.BYTES;
            result |= (bytes[i] & 0xFF);
        }
        return result;
    }

    public static String getFileChecksum(MessageDigest digest, File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);

        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }

        fis.close();

        byte[] bytes = digest.digest();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    public static short bytesToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }
}
