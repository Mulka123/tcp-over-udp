import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.*;
import java.io.*;

public class TCPSocketImpl extends TCPSocket {
    private static final int MAX_PAYLOAD_SIZE = EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES;
    private static final int receiveBufferSize = 1024; // num of available packets at first
    private EnhancedDatagramSocket udpSocket;
    private TCPState state;
    private CongestionControlState congestionState;
    private double windowSize;
    private long SSThreshold;
    private int myNextSequenceNumber;
    private InetAddress address;
    private List<TCPPacket> sentPackets = new ArrayList<>();

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        address = InetAddress.getByName(ip);
        this.peerPort = port;
        this.port = 8000;
        this.state = TCPState.CLOSED; //TODO: check
    }

    void setSeqNum(int seqNum) {
        myNextSequenceNumber = seqNum;
    }

    void setUdpSocket(EnhancedDatagramSocket sock) {
        this.udpSocket = sock;
    }

    @Override
    public void connect() throws Exception {
        if (state != TCPState.CLOSED) return;

        udpSocket = new EnhancedDatagramSocket(port);
        udpSocket.setSoTimeout(50);

        myNextSequenceNumber = -2;

        int synackSeqNum = 0;
        int synackAckNum;
        // send ACK and expect SYN-ACK
        TCPPacket packet = new TCPPacket(true, false);
        packet.setSeqNumber(myNextSequenceNumber++);
        boolean synAckReceived = false;
        while (!synAckReceived) {
            sendPacket(packet);
            state = TCPState.SYN_SENT;
            try {
                TCPPacket recvPkt = receivePacket();

                synackSeqNum = recvPkt.getSeqNum();
                synackAckNum = recvPkt.getAckNumber();
                if (synackAckNum != myNextSequenceNumber) {
                    System.out.println("BAD PACKET :|");
                    //what now?
                }
                if (recvPkt.isSYN_ACK())
                    synAckReceived = true;
            } catch (SocketTimeoutException e) {
                //TODO: limite maximum try? ye counter bezarim vase datagramSocketemon ke tedade try hasho beshmare
                // age ziad shod kolan timeout bede nasaze socketo?
            }
        }

        // send ACK
        packet = new TCPPacket(false, true);
        packet.setSeqNumber(myNextSequenceNumber++);
        packet.setAckNumber(++synackSeqNum);
        while (true) { //TODO: chejori az send shodane ACK mitonim motmaen shim?
            sendPacket(packet);
            try {
                receivePacket();
            } catch (SocketTimeoutException e) {
                break;
            }
        }

        System.out.println("Client Established.");
        state = TCPState.ESTABLISHED;
    }

    private TCPPacket receivePacket() throws Exception {
        byte[] buf = new byte[MAX_PAYLOAD_SIZE];
        DatagramPacket recvDp = new DatagramPacket(buf, buf.length);
        udpSocket.receive(recvDp);
        return TCPPacket.toTCPPacket(recvDp.getData());
    }

    private void sendPacket(TCPPacket packet) throws Exception {
        byte[] buf = packet.toStream();
        DatagramPacket dp = new DatagramPacket(buf, buf.length, address, peerPort);
        udpSocket.send(dp);
    }

    private void sendpacketchunkbychunk(String pathToFile) throws Exception {
        File file = new File(pathToFile);
        FileInputStream f = new FileInputStream(file);

        final int chunkSize = EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES - TCPPacket.overHeadSize();
        int EFS = 0;
        int highWater = 0;
        int lastAck = -1;
        int lastPacketSeqNum = -2;
        int maxTryForLastSegment = 5;
        int lastSentChunkNum = -1;
        int dupCount = 0;

        boolean isFileRead = false;
        while (!isFileRead || (lastPacketSeqNum != lastAck && maxTryForLastSegment > 0)) {

            // SENDING PART
            System.out.println("-------- SENDING SEGMENTS ---------");

            while (true) {

                if (congestionState == CongestionControlState.FASTRECOVERY) {
                    if (EFS >= windowSize) break;
                } else {
                    if (lastSentChunkNum - lastAck >= windowSize) break;
                }

                byte[] chunk = new byte[chunkSize];
                int bytesRead;
                if (!isFileRead && (bytesRead = f.read(chunk)) > 0) {

                    if (bytesRead < chunkSize)
                        chunk = Arrays.copyOfRange(chunk, 0, bytesRead);

                    TCPPacket tcpp = new TCPPacket(chunk);
                    tcpp.setSeqNumber(++lastSentChunkNum);

                    if (f.available() == 0) {
                        tcpp.setAsLastSegment();
                        lastPacketSeqNum = lastSentChunkNum;
                        isFileRead = true;
                    }

                    sentPackets.add(tcpp);

                    sendPacket(tcpp);
                    System.out.println("SENT PACKET " + tcpp.getSeqNum());
                    EFS++;
                } else {
                    isFileRead = true;
                    break;
                }
            }
            System.out.println("-------- RECEIVING ACK ---------");

            // RECEIVING PART
            boolean shouldRetransmit = false;

            try {
                TCPPacket recvpkt = receivePacket();

                System.out.println("RECEIVED ACK " + recvpkt.getAckNumber());

                CongestionControlState nextState = congestionState;
                boolean isDup = false;
                int rwnd = recvpkt.getRwndSize();
                int ackNumber = recvpkt.getAckNumber();

                // CHECK FOR DUPLICATE ACK
                if (ackNumber == lastAck) {
                    isDup = true;
                    dupCount++;
                    if (dupCount == 3 && congestionState != CongestionControlState.FASTRECOVERY) {
                        EFS--;
                        System.out.println("TRIPLE DUPACKS RECEIVED");
                        dupCount = 0;
                        highWater = lastSentChunkNum;
                        SSThreshold = Math.max(Utils.toInt(windowSize / 2), 2);
                        windowSize = SSThreshold + 3;
                        onWindowChange();
                        nextState = CongestionControlState.FASTRECOVERY;
                        shouldRetransmit = true;
                    }
                } else dupCount = 0;


                // CHANGE STATE, WINDOW SIZE AND SSTHRESHOLD
                switch (congestionState) {
                    case SLOWSTART:
                        if (!isDup) {
                            windowSize++;
                            onWindowChange();
                            if (windowSize > SSThreshold)
                                nextState = CongestionControlState.CONGESTIONAVOIDANCE;
                        }
                        break;
                    case CONGESTIONAVOIDANCE:
                        if (!isDup) {
                            windowSize += (1 / windowSize);
                            onWindowChange();
                        }
                        break;
                    case FASTRECOVERY:
                        if (!isDup) {
                            if (ackNumber >= highWater) {
                                windowSize = SSThreshold;
                                nextState = CongestionControlState.CONGESTIONAVOIDANCE;
                            } else {
                                System.out.println("PARTIAL ACK RECEIVED");
                                windowSize -= ackNumber - lastAck + 1; // num of data acked by this ack
                                shouldRetransmit = true;
                            }
                        } else {
                            windowSize++;
                        }
                        onWindowChange();
                        break;
                }

                // UPDATE LAST ACKNOWLEDGED PACKET
                if (ackNumber > lastAck)
                    lastAck = ackNumber;


                EFS -= ackNumber - lastAck + 1;

                congestionState = nextState;

                windowSize = Math.min(windowSize, rwnd);

            } catch (SocketTimeoutException e) {
                System.out.println("PACKET TIMED OUT");
                SSThreshold = Math.max(Utils.toInt(windowSize / 2), 2);
                windowSize = 1;
                onWindowChange();
                congestionState = CongestionControlState.SLOWSTART;
                shouldRetransmit = true;
            }

            // RETRANSMIT LOST SEGMENT
            if (shouldRetransmit) {
                TCPPacket pkt = sentPackets.get(lastAck + 1);
                System.out.println("RETRANSMIT PACKET " + pkt.getSeqNum());
                if (pkt.getSeqNum() == lastPacketSeqNum)
                    maxTryForLastSegment--;
                sendPacket(pkt);
                EFS++;
            }

            System.out.println("WINDOW SIZE: " + windowSize);
            System.out.println("SSTHRESHOLD: " + SSThreshold);
            System.out.println("EFS: " + EFS);
            System.out.println("STATE: " + congestionState);
        }
        f.close();
    }

    @Override
    public void send(String pathToFile) throws Exception {
        udpSocket.setSoTimeout(15);
        congestionState = CongestionControlState.SLOWSTART;
        SSThreshold = 20;
        windowSize = 1;
        sendpacketchunkbychunk(pathToFile);
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        ArrayList<TCPPacket> packets = new ArrayList<>();
        int expectedSequenceNumber = 0;
        int myLastAckNumber = -1;
        ArrayList<TCPPacket> receiveBuffer = new ArrayList<>();
        ArrayList<Integer> receivedSeqNums = new ArrayList<>();
        udpSocket.setSoTimeout(Integer.MAX_VALUE);
        while (true) {
            TCPPacket recvPkt = receivePacket();
            int seqNum = recvPkt.getSeqNum();

            if (!receivedSeqNums.contains(seqNum)) {
                Utils.addIfNotExists(receiveBuffer, recvPkt);
                receivedSeqNums.add(seqNum);
            }


            receiveBuffer.sort(Comparator.comparing(TCPPacket::getSeqNum));
            int inOrderBufferedItems = Utils.findAllInOrderItems(receiveBuffer, expectedSequenceNumber);

            System.out.println("RECEIVED PACKET " + recvPkt.getSeqNum());
            System.out.println("EXPECTING " + expectedSequenceNumber);
            System.out.println("FIRST BUFFERED ITEM: " + (receiveBuffer.size() == 0 ? null : receiveBuffer.get(0).getSeqNum()));
            System.out.println("RECV BUFFER SIZE: " + receiveBuffer.size());
            System.out.println(receiveBuffer);
            System.out.println("IN ORDER ITEMS: " + inOrderBufferedItems);
            
            if (inOrderBufferedItems > 0) {
                myLastAckNumber = receiveBuffer.get(inOrderBufferedItems - 1).getSeqNum();
                packets.addAll(Utils.extractItemsInRange(receiveBuffer, inOrderBufferedItems));
                expectedSequenceNumber = myLastAckNumber + 1;
            }

            // send ACK
            sendACK(myLastAckNumber, receiveBufferSize - receiveBuffer.size());
            System.out.println("SENT ACK " + myLastAckNumber);
            System.out.println("-------------------------");
            if (Utils.isLastPacketReceived(packets)) {
                System.out.println("PACKETS RECEIVED: " + packets.size());
                break;
            }
        }

        packets.sort(Comparator.comparing(TCPPacket::getSeqNum));
        TCPPacket.saveToFile(packets, pathToFile);
    }

    private void sendACK(int ackNumber, int rwnd) throws Exception {
        TCPPacket pkt = new TCPPacket(ackNumber, rwnd);
        sendPacket(pkt);
    }

    @Override
    public void close() {
        udpSocket.close();
    }

    @Override
    public long getSSThreshold() {
        return SSThreshold;
    }

    @Override
    public long getWindowSize() {
        return (long) windowSize;
    }
}
