import java.io.*;
import java.util.ArrayList;

class TCPPacket implements Serializable {
    private boolean SYN = false; // 1 byte
    private boolean ACK = false; // 1 byte
    private boolean PSH = false; // indicates if this is the last packet of file
    private boolean WP = false;  // window probe

    //specifies the number assigned to the first byte of data in the current message
    private int seqNumber; // 4 bytes

    //contains the value of the next sequence number that the sender of
    // the segment is expecting to receive, if the ACK control bit is set.
    private int ackNumber; // 4 bytes

    private int rwnd; // receive window size

    private byte[] payload; // variable length

    TCPPacket(int ackNumber, int rwnd) {
        this.ackNumber = ackNumber;
        this.rwnd = rwnd;
    }

    int getRwndSize() {
        return rwnd;
    }

    static void saveToFile(ArrayList<TCPPacket> packets, String pathToFile) throws IOException {
        new File(pathToFile).delete();
        try (FileOutputStream fos = new FileOutputStream(pathToFile)) {
            for (TCPPacket packet : packets)
                fos.write(packet.getData());
        }
    }

    TCPPacket(byte[] payload) {
        this.payload = payload;
    }

    void setAckNumber(int ackNumber) {
        this.ackNumber = ackNumber;
    }

    int getAckNumber() {
        return ackNumber;
    }

    void setSeqNumber(int seqNumber) {
        this.seqNumber = seqNumber;
    }

    TCPPacket(boolean SYN, boolean ACK) {
        this.SYN = SYN;
        this.ACK = ACK;
        payload = new byte[0];
    }

    static int overHeadSize() {
        return 138;
    }

    boolean isLastPacket() {
        return PSH;
    }

    int getSeqNum() {
        return seqNumber;
    }

    private byte[] getData() {
        return payload;
    }

    boolean isACK() {
        return ACK;
    }

    boolean isSYN() {
        return SYN;
    }

    boolean isSYN_ACK() {
        return SYN && ACK;
    }

    byte[] toStream() {
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

    static TCPPacket toTCPPacket(byte[] stream) {
        TCPPacket stu = null;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(stream);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            stu = (TCPPacket) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return stu;
    }

    void setAsLastSegment() {
        PSH = true;
    }

    @Override
    public String toString() {
        return Integer.toString(seqNumber);
    }
}
