import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class Utils {
    public static int randomInRange(int min,int max) {
        return min + (int)(Math.random() * ((max - min) + 1));
    }

    public static int findAllInOrderItems(ArrayList<TCPPacket> packets, int firstSeqNum) {
        int inOrderedItems = 0;
        for (int i = 0; i < packets.size(); i++) {
            if(packets.get(i).getSeqNum() == firstSeqNum+i)
                inOrderedItems++;
        }
        return inOrderedItems;
    }

    public static int getLastSeqNum(ArrayList<TCPPacket> receive_buffer) {
        return receive_buffer.get(receive_buffer.size()-1).getSeqNum();
    }

    public static ArrayList<TCPPacket> extractItemsInRange(ArrayList<TCPPacket> list, int lb, int ub) {
        // [lb, ub) (not tested)
        ArrayList<TCPPacket> subList = new ArrayList<>(list.subList(lb, ub));
        list.removeAll(subList);
        return subList;
    }

    public static void addIfNotExists(ArrayList<TCPPacket> receive_buffer, TCPPacket recv_pkt) {
        if(!receive_buffer.contains(recv_pkt)) { //check with sequence number?
            receive_buffer.add(recv_pkt);
        }
    }
}
