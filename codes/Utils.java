public class Utils {
    public static int randomInRange(int min,int max) {
        return min + (int)(Math.random() * ((max - min) + 1));
    }
}
