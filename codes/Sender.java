import java.io.IOException;
import java.net.*;

public class Sender {
    public static void main(String[] args) throws Exception {
        TCPSocket tcpSocket = new TCPSocketImpl("127.0.0.1", 12324);
        tcpSocket.connect();
        tcpSocket.send("1MB.txt");
        tcpSocket.close();
    }
}