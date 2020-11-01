public class Utils {
    private static final int MAX_PORT = 65535;
    private static final int MIN_PORT = 1;
    private static final int MAX_ADDRESS = 255;
    private static final int MIN_ADDRESS = 0;
    // 分割后IP地址的长度
    private static final int SPLIT_ADDRESS = 4;

    public static boolean isIpAddress(String address) {
        String[] splitAddress = address.split("\\.");
        if (splitAddress.length != SPLIT_ADDRESS) {
            return false;
        }

        for (String s : splitAddress) {
            // 无法转int，返回false
            try {
                int i = Integer.parseInt(s);
                if (i > MAX_ADDRESS || i < MIN_ADDRESS) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    public static boolean isPort(String port) {
        try {
            int i = Integer.parseInt(port);
            return i <= MAX_PORT && i >= MIN_PORT;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
