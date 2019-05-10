import java.util.ArrayList;

class Utils {
    static int findAllInOrderItems(ArrayList<TCPPacket> packets, int firstSeqNum) {
        int inOrderedItems = 0;
        for (int i = 0; i < packets.size(); i++) {
            if (packets.get(i).getSeqNum() == firstSeqNum + i)
                inOrderedItems++;
        }
        return inOrderedItems;
    }

    static ArrayList<TCPPacket> extractItemsInRange(ArrayList<TCPPacket> list, int ub) {
        ArrayList<TCPPacket> subList = new ArrayList<>(list.subList(0, ub));
        list.removeAll(subList);
        return subList;
    }

    static void addIfNotExists(ArrayList<TCPPacket> receive_buffer, TCPPacket recv_pkt) {
        for (TCPPacket packet : receive_buffer)
            if (packet.getSeqNum() == recv_pkt.getSeqNum()) return;
        receive_buffer.add(recv_pkt);
    }

    static long toInt(double num) {
        return (int) Math.floor(num);
    }

    static boolean isLastPacketReceived(ArrayList<TCPPacket> packets) {
        for (int i = packets.size() - 1; i >= 0; --i) {
            if (packets.get(i).isLastPacket()) return true;
        }
        return false;
    }
}
