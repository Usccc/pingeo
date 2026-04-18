package top.pigimag.pingeo.util;

import java.util.List;

import top.pigimag.pingeo.pi.BedrockServerPinger;
import top.pigimag.pingeo.pi.JavaMinecraftServerPinger;

public class AResponseFormatter {
    private static enum ServerCoVersion{BEDROCK,JAVA}

    private final ServerCoVersion ver;
    private final String host;
    private final int port;
    private final long latencyMillis;
    private final int protocolVersion;
    private final String versionName;
    private final String motd;
    private final String motd2;
    private final int onlinePlayers;
    private final int maxPlayers;
    private final long pingId;
    private final int serverId;
    private final String serverUniqueId;
    private final List<String> playerList;
    private final String rawResponse;

    public AResponseFormatter(BedrockServerPinger.PingResponse pr){
        ver = ServerCoVersion.BEDROCK;
        host = pr.getHost();
        port = pr.getPort();
        latencyMillis = pr.getLatencyMillis();
        protocolVersion = pr.getProtocolVersion();
        versionName = pr.getVersionName();
        motd = pr.getMotd();
        motd2 = pr.getMotd2();
        onlinePlayers = pr.getOnlinePlayers();
        maxPlayers = pr.getMaxPlayers();
        pingId = pr.getPingId();
        serverId = pr.getServerId();
        serverUniqueId = pr.getServerUniqueId();
        playerList = pr.getPlayerList();
        rawResponse = pr.getRawResponse();
    }
    public AResponseFormatter(JavaMinecraftServerPinger.PingResponse pr){
        ver = ServerCoVersion.JAVA;
        host = pr.getHost();
        port = pr.getPort();
        latencyMillis = pr.getLatencyMillis();
        protocolVersion = pr.getProtocolVersion();
        versionName = pr.getVersionName();
        motd = null;
        motd2 = null;
        onlinePlayers = pr.getOnlinePlayers();
        maxPlayers = pr.getMaxPlayers();
        pingId = -1;
        serverId = -1;
        serverUniqueId = null;
        playerList = pr.getPlayerList();
        rawResponse = pr.getRawJson();
    }
    private String toStringBedrock(){
        StringBuilder sb = new StringBuilder();
        sb.append("Bedrock Server Info:\n");
        sb.append("Host: ").append(host).append("\n");
        sb.append("Port: ").append(port).append("\n");
        sb.append("Latency: ").append(formatLatency(latencyMillis)).append(" ms\n");
        sb.append("Version: ").append(versionName).append(" (").append(protocolVersion).append(")\n");
        sb.append("MOTD: ").append(motd).append("\n");
        if (motd2 != null) sb.append("MOTD2: ").append(motd2).append("\n");
        sb.append("Players: ").append(onlinePlayers).append("/").append(maxPlayers).append("\n");
        if (playerList != null && !playerList.isEmpty()) {
            sb.append("Player List: ").append(String.join(", ", playerList)).append("\n");
        }
        sb.append("Ping ID: ").append(pingId).append("\n");
        sb.append("Server ID: ").append(serverId).append("\n");
        sb.append("Server Unique ID: ").append(serverUniqueId).append("\n");
        sb.append("Raw Response: ").append(rawResponse).append("\n");
        return sb.toString();
    }
    private String formatLatency(long l){
        StringBuilder sb = new StringBuilder();
        if (l < 0) {
            throw new IllegalArgumentException("latency is not possible to be less than 0");
        }
        if (l < 100) {
            sb.append(ColoredTextPrinter.GREEN + l + ColoredTextPrinter.RESET);
            return sb.toString();
        }
        if (l < 500) {
            sb.append(ColoredTextPrinter.YELLOW + l + ColoredTextPrinter.RESET);
            return sb.toString();
        }
        if (l < 1000) {
            sb.append(ColoredTextPrinter.MAGENTA + l + ColoredTextPrinter.RESET);
            return sb.toString();
        }
        sb.append(ColoredTextPrinter.RED + l + ColoredTextPrinter.RESET);
        return sb.toString();
    }
    private String toStringJava(){
        StringBuilder sb = new StringBuilder();
        sb.append("Java Minecraft Server Info:\n");
        sb.append("Host: ").append(host).append("\n");
        sb.append("Port: ").append(port).append("\n");
        sb.append("Latency: ").append(formatLatency(latencyMillis)).append(" ms\n");
        sb.append("Version: ").append(versionName).append(" (").append(protocolVersion).append(")\n");
        sb.append("MOTD: ").append(motd).append("\n");
        sb.append("Players: ").append(onlinePlayers).append("/").append(maxPlayers).append("\n");
        if (playerList != null && !playerList.isEmpty()) {
           sb.append("Player List: ").append(String.join(", ", playerList)).append("\n");
        }
        sb.append("Raw Response: ").append(rawResponse).append("\n");
        return sb.toString();
    }
    public String toString(){
        switch (ver) {
            case JAVA:
                return toStringJava();
            case BEDROCK:
                return toStringBedrock();
            default:
                throw new Error();
        }
    }
}
