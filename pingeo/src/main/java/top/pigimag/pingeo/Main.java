package top.pigimag.pingeo;

import java.util.Optional;
import top.pigimag.pingeo.opt.OptionsHandler;
import top.pigimag.pingeo.pi.BedrockServerPinger;
import top.pigimag.pingeo.pi.JavaMinecraftServerPinger;
import top.pigimag.pingeo.util.AResponseFormatter;
import java.util.ResourceBundle;
import java.util.Locale;

public class Main {
    private static ResourceBundle messages;

    static {
        Locale locale = Locale.getDefault();
        if (locale.getLanguage().equals("zh")) {
            messages = ResourceBundle.getBundle("messages", Locale.CHINA);
        } else {
            messages = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        }
    }
    public static void main(String[] args) {
        new OptionsHandler(args).ParseIt();
    }
    private static final class AppVersion{
        private final long major;
        private final long minor;
        private final long patch;
        private final String pre;
        private AppVersion(
            long arg0,
            long arg1,
            Optional<Long> arg2,
            Optional<String> arg3
        ){
            major = arg0;
            minor = arg1;
            patch = arg2.isEmpty()?0:arg2.get();
            pre = arg3.isEmpty()?"":arg3.get();
        }
        private static final AppVersion VER = new AppVersion((long)0, (long)2, Optional.of((long)2),Optional.empty());
        public static AppVersion getAppVersion(){
            return VER;
        }
        public long getMajor() {
            return major;
        }
        public long getMinor() {
            return minor;
        }
        public long getPatch() {
            return patch;
        }
        public String getPre() {
            return pre;
        }
        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            final String sd = ".";
            sb.append(getMajor());
            sb.append(sd);
            sb.append(getMinor());
            sb.append(sd);
            sb.append(getPatch());
            if (getPre() == "") return sb.toString();
            sb.append('-').append(getPre());
            return sb.toString();
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (major ^ (major >>> 32));
            result = prime * result + (int) (minor ^ (minor >>> 32));
            result = prime * result + (int) (patch ^ (patch >>> 32));
            result = prime * result + ((pre == null || pre.isEmpty()) ? 0 : pre.hashCode());
            return result;
        }
    }

    public static String getVersion() {
        AppVersion v = AppVersion.getAppVersion();
        return v.toString()+"("+Runtime.version().toString()+")";
    }

    public static void pingJE(String host, int port) {
        try {
            JavaMinecraftServerPinger pinger = new JavaMinecraftServerPinger(host, port);
            JavaMinecraftServerPinger.PingResponse response = pinger.ping();
            AResponseFormatter formatter = new AResponseFormatter(response);
            System.out.println(messages.getString("jeResponse") + formatter.toString());
        } catch (Exception e) {
            System.err.println(messages.getString("jeFailed") + e.getMessage());
        }
    }

    public static void pingBE(String host, int port) {
        try {
            BedrockServerPinger pinger = new BedrockServerPinger(host, port);
            BedrockServerPinger.PingResponse response = pinger.ping();
            AResponseFormatter formatter = new AResponseFormatter(response);
            System.out.println(messages.getString("beResponse") + formatter.toString());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(messages.getString("beFailed") + e.getMessage());
        }
    }


}