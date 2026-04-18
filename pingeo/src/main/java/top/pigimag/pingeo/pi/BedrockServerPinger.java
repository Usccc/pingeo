package top.pigimag.pingeo.pi;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Minecraft Bedrock Edition server pinger implementation.
 * Provides synchronous and asynchronous methods to ping Bedrock servers
 * and retrieve server information including latency, version, player counts, etc.
 */
public class BedrockServerPinger {

    private final String host;
    private final int port;

    /**
     * Creates a new Bedrock server pinger for the specified host and port.
     *
     * @param host the server hostname or IP address
     * @param port the server port
     */
    public BedrockServerPinger(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Response data structure containing all information retrieved from a Bedrock server ping.
     */
    public static final class PingResponse implements Serializable {
        /** Server hostname */
        public final String host;
        /** Server port */
        public final int port;
        /** Ping latency in milliseconds */
        public final long latencyMillis;
        /** Protocol version number */
        public final int protocolVersion;
        /** Server version name */
        public final String versionName;
        /** Main MOTD line */
        public final String motd;
        /** Secondary MOTD line */
        public final String motd2;
        /** Current number of online players */
        public final int onlinePlayers;
        /** Maximum number of players */
        public final int maxPlayers;
        /** Ping ID from server response */
        public final long pingId;
        /** Server ID */
        public final int serverId;
        /** Server unique identifier */
        public final String serverUniqueId;
        /** List of online player names (may be empty for Bedrock basic pong) */
        public final List<String> playerList;
        /** Raw server response string */
        public final String rawResponse;

        public PingResponse(
                String host,
                int port,
                long latencyMillis,
                int protocolVersion,
                String versionName,
                String motd,
                String motd2,
                int onlinePlayers,
                int maxPlayers,
                long pingId,
                int serverId,
                String serverUniqueId,
                List<String> playerList,
                String rawResponse) {
            this.host = host;
            this.port = port;
            this.latencyMillis = latencyMillis;
            this.protocolVersion = protocolVersion;
            this.versionName = versionName;
            this.motd = motd;
            this.motd2 = motd2;
            this.onlinePlayers = onlinePlayers;
            this.maxPlayers = maxPlayers;
            this.pingId = pingId;
            this.serverId = serverId;
            this.serverUniqueId = serverUniqueId;
            this.playerList = playerList;
            this.rawResponse = rawResponse;
        }

        @Override
        public String toString() {
            return String.format(
                    "Bedrock %s:%d latency=%dms version=%s protocol=%d players=%d/%d motd=%s%s serverId=%d serverUniqueId=%s",
                    host,
                    port,
                    latencyMillis,
                    versionName,
                    protocolVersion,
                    onlinePlayers,
                    maxPlayers,
                    motd,
                    motd2.isEmpty() ? "" : " / " + motd2,
                    serverId,
                    serverUniqueId);
        }
    }

    /**
     * Synchronously pings the Bedrock server and returns detailed response information.
     *
     * @return PingResponse containing all server information
     * @throws IOException if network communication fails
     */
    public PingResponse ping() throws IOException {
        InetAddress address = InetAddress.getByName(host);
        byte[] magic = new byte[]{
                0x00, (byte) 0xFF, (byte) 0xFF, 0x00,
                (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE,
                (byte) 0xFD, (byte) 0xFD, (byte) 0xFD, (byte) 0xFD,
                0x12, 0x34, 0x56, 0x78
        };

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(3000);
            long start = System.nanoTime();

            ByteBuffer request = ByteBuffer.allocate(1 + Long.BYTES + magic.length + Integer.BYTES)
                    .order(ByteOrder.LITTLE_ENDIAN);
            request.put((byte) 0x01);
            request.putLong(System.currentTimeMillis());
            request.put(magic);
            request.putInt(0);

            byte[] out = request.array();
            DatagramPacket packet = new DatagramPacket(out, out.length, address, port);
            socket.send(packet);

            byte[] inBuffer = new byte[2048];
            DatagramPacket response = new DatagramPacket(inBuffer, inBuffer.length);
            socket.receive(response);
            long latencyMillis = (System.nanoTime() - start) / 1_000_000;

            ByteBuffer buffer = ByteBuffer.wrap(response.getData(), 0, response.getLength())
                    .order(ByteOrder.LITTLE_ENDIAN);
            byte packetId = buffer.get();
            if (packetId != 0x1C) {
                throw new IOException("Unexpected Bedrock response packet id: " + packetId);
            }

            long pingId = buffer.getLong();
            //long serverGuid = buffer.getLong();
            byte[] serverMagic = new byte[16];
            buffer.get(serverMagic);
            byte[] messageBytes = new byte[buffer.remaining()];
            buffer.get(messageBytes);
            String raw = new String(messageBytes, StandardCharsets.UTF_8);
            String[] tokens = raw.split(";", -1);

            int protocolVersion = tokens.length > 2 ? parseIntSafe(tokens[2]) : -1;
            String versionName = tokens.length > 3 ? tokens[3] : "unknown";
            String motd = tokens.length > 1 ? tokens[1] : "";
            String motd2 = tokens.length > 4 ? tokens[4] : "";
            int onlinePlayers = tokens.length > 5 ? parseIntSafe(tokens[5]) : -1;
            int maxPlayers = tokens.length > 6 ? parseIntSafe(tokens[6]) : -1;
            String serverUniqueId = tokens.length > 7 ? tokens[7] : "";
            List<String> playerList = Collections.emptyList();
            // Bedrock basic pong only returns summary fields. Player sample list is not available in this packet.

            return new PingResponse(
                    host,
                    port,
                    latencyMillis,
                    protocolVersion,
                    versionName,
                    motd,
                    motd2,
                    onlinePlayers,
                    maxPlayers,
                    pingId,
                    0, // serverId not used
                    serverUniqueId,
                    playerList,
                    raw);
        }
    }

    /**
     * Asynchronously pings the Bedrock server and returns a CompletableFuture
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

    private static int parseIntSafe(String value) {
        if (value == null || value.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
