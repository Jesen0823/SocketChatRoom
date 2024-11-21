public class MessageCreator {
    private static final String SN_HEADER="收到广播，我是(SN):";
    private static final String A_HEADER ="这是广播，收到请回复,端口等你(Port)：";

    public static String buildWithPort(int port){
        return A_HEADER+port;
    }

    public static int parsePort(String data){
        if (data.startsWith(A_HEADER)){
            return Integer.parseInt(data.substring(A_HEADER.length()));
        }
        return -1;
    }

    public static String buildWithSN(String sn){
        return SN_HEADER+sn;
    }

    public static String parseSN(String data){
        if (data.startsWith(SN_HEADER)){
            return data.substring(SN_HEADER.length());
        }
        return null;
    }
}
