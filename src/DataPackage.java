import java.util.ArrayList;
import java.util.zip.CRC32;

public class DataPackage {
    private static DataPackage instance;
    private byte[] data;
    private ArrayList<miniDataPackage> splittedData;


    private DataPackage() {

    }

    public static DataPackage getInstance() {
        if (instance == null) {
            instance = new DataPackage();
        }
        return instance;
    }

    public byte[] getData() {
        return data;
    }

    public ArrayList<miniDataPackage> getSplittedData() {
        return splittedData;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getTotalPackages(){
        int totalPackages = (int)Math.ceil((data.length)/(498.0));

        return totalPackages;
    }

    public void splitData() {
        ArrayList<miniDataPackage> splittedData = new ArrayList<>();
        byte[] pedaco = new byte[512];
        int aux = 0;
        short aux2= 1;
        byte[] aux2Byte;
        String s = "";
        String a = "";
        int ultimo = 0;

        byte[] totalPackages = (String.format("%04d",getTotalPackages())).getBytes();


        int result = data.length +((int)Math.ceil(data.length/512.0)*14);
        for (int i = 0; i < result ; i++) {
            if(aux==0 || aux==1){
                a= aux2+"";
                s= ("00"+aux2).substring(a.length());
                aux2Byte = (String.format("%02d",aux2)).getBytes();
                pedaco[aux] = aux2Byte[aux];
                aux++;

            }
            else if(aux > 1 && aux <10){
                pedaco[aux] =(byte)48;
                aux++;
            }
            else if(aux >9 && aux<14){
                pedaco[aux] = totalPackages[aux-10];
                aux++;
            }
            else {
                pedaco[aux] = this.data[ultimo];
                ultimo++;

                if (aux==511) {

                    // adiciona o crc no package
                    CRC32 crc32 = new CRC32();
                    crc32.update(pedaco,0,pedaco.length);
                    long crc32Long = crc32.getValue();
                    byte[] longByte;
                    longByte= longToBytes(crc32Long);
                    for (int j =2; j<10; j++){
                        pedaco[j]= longByte[j-2];
                    }

                    System.out.println("pedaco: " + new String(pedaco));
                    splittedData.add(new miniDataPackage(pedaco));

                    pedaco = new byte[512];
                    aux = 0;
                    aux2 ++;
                }
                else{
                    aux++;
                }

            }

        }

        // adiciona o crc no package
        CRC32 crc32 = new CRC32();
        crc32.update(pedaco,0,pedaco.length);
        long crc32Long = crc32.getValue();
        byte[] longByte;
        longByte= longToBytes(crc32Long);
        for (int i =2; i<10; i++){
            pedaco[i]= longByte[i-2];
        }

        // adiciona o numero de pacotes no package
        for (int i = 10; i <14 ; i++) {
            pedaco[i] = totalPackages[i-10];
        }




        splittedData.add(new miniDataPackage(pedaco));

        System.out.println("pedaco: " + new String(pedaco));


        this.splittedData = splittedData;
    }

    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    public static long bytesToLong(final byte[] bytes, final int offset) {
        long result = 0;
        for (int i = offset; i < Long.BYTES + offset; i++) {
            result <<= Long.BYTES;
            result |= (bytes[i] & 0xFF);
        }
        return result;
    }

    public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }




}
