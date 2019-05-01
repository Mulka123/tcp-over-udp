import java.io.IOException;
import java.net.*;

public class Sender {
    public static void main(String[] args) throws Exception {
//        TCPPacket packet = new TCPPacket(true, false, false);
//        TCPPacket packet = new TCPPacket("SALAMSALAM".getBytes());
//        byte[] packet_stream = packet.toStream();
//        DatagramPacket dp = new DatagramPacket(packet_stream, packet_stream.length);
//        TCPPacket newpacket = TCPPacket.toTCPPacket(packet.toStream());
//        byte[] data = dp.getData();
//        TCPPacket converted = TCPPacket.toTCPPacket(data);
//        System.out.println(converted.getData() + " " + converted.isACK() + " " + converted.isSYN());
//
        TCPSocket tcpSocket = new TCPSocketImpl("127.0.0.1", 12344);
        tcpSocket.connect();
//
//        tcpSocket.send("sending.mp3");
        tcpSocket.close();
//        tcpSocket.saveCongestionWindowPlot();
//        byte[] b = "SALAMSALAM".getBytes();
//        Utils.test(b);
    }
}
