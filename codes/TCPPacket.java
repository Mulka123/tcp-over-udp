import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class TCPPacket implements Serializable {
    boolean SYN = false; // 1 byte
    boolean ACK = false; // 1 byte
    boolean PSH = false; // indicates if this is the last packet of file

    //specifies the number assigned to the first byte of data in the current message
    int seqNumber; // 4 bytes

    //contains the value of the next sequence number that the sender of
    // the segment is expecting to receive, if the ACK control bit is set.
    int ackNumber; // 4 bytes

    // header contains 11 bytes

    byte[] payload; // variable length

    public TCPPacket(int ackNumber) {
        this.ackNumber = ackNumber;
    }

    public static void saveToFile(ArrayList<TCPPacket> packets, String pathToFile) throws IOException {

        for (TCPPacket packet : packets) {
            Files.write(Paths.get(pathToFile),
                    packet.getData(), //encode, decode?
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    public TCPPacket(byte[] payload) {
        this.payload = payload;
    }

    public void setAckNumber(int ackNumber) { this.ackNumber = ackNumber; }

    public int getAckNumber() { return ackNumber; }

    public void setSeqNumber(int seqNumber) { this.seqNumber = seqNumber; }

    public TCPPacket(boolean SYN, boolean ACK) {
        this.SYN = SYN;
        this.ACK = ACK;
    }

    public boolean isLastPacket() { return PSH; }

    public int getSeqNum() { return seqNumber; }

    public byte[] getData() { return payload; }

    public boolean isACK() { return ACK; }
    public boolean isSYN() { return SYN; }
    public boolean isSYN_ACK() { return SYN && ACK; }

    public byte[] toStream(){
        byte[] stream = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            stream = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream;
    }

    public static TCPPacket toTCPPacket(byte[] stream) {
        TCPPacket stu = null;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(stream);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            stu = (TCPPacket) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // Error in de-serialization
            e.printStackTrace();
        } // You are converting an invalid stream to TCPPacket

        return stu;
    }

//    public static TCPPacket toACKPacket(int ackNumber) {
//        return new TCPPacket(ackNumber);
//    }
}
