package top.pigimag.pingeo.pi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Minecraft Java Edition server pinger implementation.
 * Provides synchronous and asynchronous methods to ping Java servers
 * and retrieve server information including latency, version, player counts, player list, etc.
 */
public class JavaMinecraftServerPinger {

    private final String host;
    private final int port;

    /**
     * Creates a new Java server pinger for the specified host and port.
     *
     * @param host the server hostname or IP address
     * @param port the server port
     */
    public JavaMinecraftServerPinger(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Response data structure containing all information retrieved from a Java server ping.
     */
    public static final class PingResponse {
        /** Server hostname */
        private final String host;
        /** Server port */
        private final int port;
        /** Ping latency in milliseconds */
        private final long latencyMillis;
        /** Server version name */
        private final String versionName;
        /** Protocol version number */
        private final int protocolVersion;
        /** Server description/MOTD */
        private final String description;
        /** Current number of online players */
        private final int onlinePlayers;
        /** Maximum number of players */
        private final int maxPlayers;
        /** List of online player names (sample from server) */
        private final List<String> playerList;
        /** Raw JSON response from server */
        private final String rawJson;

        public PingResponse(
                String host,
                int port,
                long latencyMillis,
                String versionName,
                int protocolVersion,
                String description,
                int onlinePlayers,
                int maxPlayers,
                List<String> playerList,
                String rawJson) {
            this.host = host;
            this.port = port;
            this.latencyMillis = latencyMillis;
            this.versionName = versionName;
            this.protocolVersion = protocolVersion;
            this.description = description;
            this.onlinePlayers = onlinePlayers;
            this.maxPlayers = maxPlayers;
            this.playerList = playerList;
            this.rawJson = rawJson;
        }

        @Override
        public String toString() {
            return String.format(
                    "Java %s:%d latency=%dms version=%s protocol=%d players=%d/%d description=%s players=%s",
                    host,
                    port,
                    latencyMillis,
                    versionName,
                    protocolVersion,
                    onlinePlayers,
                    maxPlayers,
                    description,
                    playerList);
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public long getLatencyMillis() {
            return latencyMillis;
        }

        public String getVersionName() {
            return versionName;
        }

        public int getProtocolVersion() {
            return protocolVersion;
        }

        public String getDescription() {
            return description;
        }

        public int getOnlinePlayers() {
            return onlinePlayers;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public List<String> getPlayerList() {
            return playerList;
        }

        public String getRawJson() {
            return rawJson;
        }
    }

    /**
     * Synchronously pings the Java Minecraft server and returns detailed response information.
     *
     * @return PingResponse containing all server information
     * @throws IOException if network communication fails
     */
    public PingResponse ping() throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            socket.setSoTimeout(3000);
            long start = System.nanoTime();

            OutputStream output = socket.getOutputStream();
            InputStream input = socket.getInputStream();

            byte[] handshake = createHandshakePacket(host, port);
            sendPacket(output, handshake);
            sendPacket(output, new byte[]{0x00});

            byte[] responsePacket = receivePacket(input);
            long latencyMillis = (System.nanoTime() - start) / 1_000_000;

            try (ByteArrayInputStream packetStream = new ByteArrayInputStream(responsePacket)) {
                int packetId = readVarInt(packetStream);
                if (packetId != 0x00) {
                    throw new IOException("Unexpected Java ping packet id: " + packetId);
                }
                //int stringLength = readVarInt(packetStream);
                byte[] jsonBytes = packetStream.readAllBytes();
                String rawJson = new String(jsonBytes, StandardCharsets.UTF_8);

                String versionName = extractJsonString(rawJson, "\"name\"");
                int protocolVersion = extractJsonInt(rawJson, "\"protocol\"");
                String description = extractDescription(rawJson);
                int onlinePlayers = extractJsonInt(rawJson, "\"online\"");
                int maxPlayers = extractJsonInt(rawJson, "\"max\"");
                List<String> playerList = extractPlayerNames(rawJson);

                return new PingResponse(
                        host,
                        port,
                        latencyMillis,
                        versionName,
                        protocolVersion,
                        description,
                        onlinePlayers,
                        maxPlayers,
                        playerList,
                        rawJson);
            }
        }
    }

    /**
     * Asynchronously pings the Java Minecraft server and returns a CompletableFuture
     * that will complete with the detailed response information.
     *
     * @return CompletableFuture that completes with PingResponse containing all server information
     */
    public CompletableFuture<PingResponse> pingAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ping();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static byte[] createHandshakePacket(String host, int port) throws IOException {
        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        writeVarInt(packet, 0x00);
        writeVarInt(packet, 757);
        writeVarInt(packet, host.length());
        packet.write(host.getBytes(StandardCharsets.UTF_8));
        packet.write((port >> 8) & 0xFF);
        packet.write(port & 0xFF);
        writeVarInt(packet, 1);
        return packet.toByteArray();
    }

    private static void sendPacket(OutputStream output, byte[] data) throws IOException {
        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        writeVarInt(packet, data.length);
        packet.write(data);
        output.write(packet.toByteArray());
        output.flush();
    }

    private static byte[] receivePacket(InputStream input) throws IOException {
        int packetLength = readVarInt(input);
        byte[] packetData = new byte[packetLength];
        int read = 0;
        while (read < packetLength) {
            int count = input.read(packetData, read, packetLength - read);
            if (count < 0) {
                throw new IOException("Unexpected end of stream while reading Minecraft response");
            }
            read += count;
        }
        return packetData;
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }

    private static int readVarInt(InputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        int read;
        do {
            read = in.read();
            if (read == -1) {
                throw new IOException("Unexpected end of stream while reading VarInt");
            }
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new IOException("VarInt is too big");
            }
        } while ((read & 0x80) != 0);
        return result;
    }

    private static String extractJsonString(String json, String key) {
        int index = json.indexOf(key);
        if (index < 0) {
            return "unknown";
        }
        int start = json.indexOf('"', index + key.length());
        if (start < 0) {
            return "unknown";
        }
        int end = json.indexOf('"', start + 1);
        if (end < 0) {
            return "unknown";
        }
        return json.substring(start + 1, end);
    }

    private static int extractJsonInt(String json, String key) {
        int index = json.indexOf(key);
        if (index < 0) {
            return -1;
        }
        int colon = json.indexOf(':', index + key.length());
        if (colon < 0) {
            return -1;
        }
        int start = colon + 1;
        while (start < json.length() && !Character.isDigit(json.charAt(start)) && json.charAt(start) != '-') {
            start++;
        }
        if (start >= json.length()) {
            return -1;
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String extractDescription(String json) {
        int index = json.indexOf("\"description\"");
        if (index < 0) {
            return "unknown";
        }
        int colon = json.indexOf(':', index);
        if (colon < 0) {
            return "unknown";
        }
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length()) {
            return "unknown";
        }
        if (json.charAt(start) == '{') {
            int nameIndex = json.indexOf("\"text\"", start);
            if (nameIndex < 0) {
                return "unknown";
            }
            return extractJsonString(json.substring(nameIndex), "\"text\"");
        }
        int quote = json.indexOf('"', start);
        if (quote < 0) {
            return "unknown";
        }
        int endQuote = json.indexOf('"', quote + 1);
        if (endQuote < 0) {
            return "unknown";
        }
        return json.substring(quote + 1, endQuote);
    }

    private static List<String> extractPlayerNames(String json) {
        int sampleIndex = json.indexOf("\"sample\"");
        if (sampleIndex < 0) {
            return List.of();
        }
        int arrayStart = json.indexOf('[', sampleIndex);
        int arrayEnd = json.indexOf(']', arrayStart);
        if (arrayStart < 0 || arrayEnd < 0) {
            return List.of();
        }
        String arrayText = json.substring(arrayStart + 1, arrayEnd);
        List<String> players = new ArrayList<>();
        int pos = 0;
        while (pos < arrayText.length()) {
            int nameIndex = arrayText.indexOf("\"name\"", pos);
            if (nameIndex < 0) {
                break;
            }
            String playerName = extractJsonString(arrayText.substring(nameIndex), "\"name\"");
            players.add(playerName);
            pos = nameIndex + 6;
        }
        return players;
    }
}
