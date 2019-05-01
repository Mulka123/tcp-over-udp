import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class TCPPacket implements Serializable {
    boolean SYN = false; // 1 byte
    boolean SYN_ACK = false; // 1 byte
    boolean ACK = false; // 1 byte

    //specifies the number assigned to the first byte of data in the current message
    int seq_number; // 4 bytes

    //contains the value of the next sequence number that the sender of
    // the segment is expecting to receive, if the ACK control bit is set.
    int ack_number; // 4 bytes

    // header contains 11 bytes

    byte[] payload; // variable length

    public TCPPacket(byte[] payload) {
        this.payload = payload;
    }

    public TCPPacket(boolean SYN, boolean SYN_ACK, boolean ACK) {
        this.SYN = SYN;
        this.SYN_ACK = SYN_ACK;
        this.ACK = ACK;
//        this.payload = "".getBytes();
    }

    public String getData() { return (payload == null) ? null : new String(payload); }

    public boolean isACK() { return ACK; }
    public boolean isSYN() { return SYN; }
    public boolean isSYN_ACK() { return SYN_ACK; }

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
}
