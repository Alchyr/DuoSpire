package duospire.networking.gameplay;

import basemod.BaseMod;
import com.badlogic.gdx.Gdx;
import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamNetworking;
import com.codedisaster.steamworks.SteamNetworkingCallback;
import duospire.DuoSpire;
import duospire.networking.matchmaking.Matchmaking;
import duospire.util.RunningAverage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static duospire.DuoSpire.*;

public class P2P implements SteamNetworkingCallback {
    private static final Logger p2pLogger = LogManager.getLogger("P2P");
    private static final CharsetDecoder decoder = CHARSET.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);

    private static final double NO_CONNECTION_TIME = 999;
    private static final double BAD_CONNECTION_TIME = 10;
    private static final long PING_ID_LIMIT = 0x7ffffffffffffffeL;
    private static final long NON_RESPONSE = -2L;

    private static final byte PING = 0;
    private static final byte CMD = 1;
    private static final byte MSG = 2;

    public static SteamNetworking networking;
    private static P2P callback;

    public static SteamID currentPartner;
    public static String partnerName = "";
    public static boolean connected = false;

    //Do channels start from 0 or 1? Or is it just an arbitrary integer with no limitations other than that you have to read from that specific integer?
    private static final int defaultChannel = 1;

    private static final RunningAverage ping = new RunningAverage(10);
    private static long pingID = 0L;
    private static final HashMap<Long, Double> pingTimers = new HashMap<>();
    private static final float TIMEOUT_TIME = 8;
    private static float retryPingTimer = TIMEOUT_TIME;

    private static final int[] msgSize = new int[] { 0 };
    private static final SteamID msgSource = new SteamID();

    private static final ByteBuffer packetSendBuffer = ByteBuffer.allocateDirect(4096);
    private static final ByteBuffer packetReceiveBuffer = ByteBuffer.allocateDirect(4096);

    public static void init()
    {
        dispose();

        callback = new P2P();
        networking = new SteamNetworking(callback);
        ping.clear(0);
    }

    public static void reset()
    {
        if (currentPartner != null)
        {
            networking.closeP2PSessionWithUser(currentPartner);
            currentPartner = null;
        }
        partnerName = "";
        connected = false;
        p2pLogger.info("Disconnected.");

        ping.clear(0);
        pingID = 0;
        pingTimers.clear();
        retryPingTimer = TIMEOUT_TIME;
    }

    public static boolean connected() {
        if (currentPartner != null && currentPartner.isValid()) {
            return connected;
        }
        return false;
    }

    public static void startConnection(SteamID otherPlayer, String otherPlayerName) {
        currentPartner = otherPlayer;
        partnerName = otherPlayerName;
        connected = false;
        startP2P();
        BaseMod.addTopPanelItem(pingDisplay);
    }

    public static double getMsPing() {
        return ping.avg() * 1000;
    }

    @Override
    public void onP2PSessionConnectFail(SteamID steamID, SteamNetworking.P2PSessionError p2pSessionError) {
        p2pLogger.error("Failed to connect to lobby partner.");
        p2pLogger.error(p2pSessionError);
        if (currentPartner != null && currentPartner.isValid()) {
            startP2P();
        }
    }

    @Override
    public void onP2PSessionRequest(SteamID steamID) {
        p2pLogger.info("Received session request from " + steamID.getAccountID());
        if (Matchmaking.inLobby(steamID) || steamID.equals(Matchmaking.otherPlayer))
        {
            p2pLogger.info("Accepting session request.");
            currentPartner = steamID;
            partnerName = Matchmaking.otherPlayerName;

            if (networking.acceptP2PSessionWithUser(steamID)) {
                p2pLogger.info("P2P session accepted.");
                connected = true;
                retryPingTimer = TIMEOUT_TIME;
                pingID = 0;
                sendPing(NON_RESPONSE);
                BaseMod.addTopPanelItem(pingDisplay);
            }
            else {
                p2pLogger.info("P2P session request was invalid.");
            }
        }
        else {
            p2pLogger.info("P2P session request was from an invalid user.");
        }
    }

    private static void startP2P() {
        packetSendBuffer.clear();
        packetSendBuffer.put(PING);
        packetSendBuffer.flip();
        sendPacket(defaultChannel);
        p2pLogger.info("Sent initial empty ping.");
    }

    //Sending
    private static void sendPing(long respondingTo) {
        retryPingTimer = TIMEOUT_TIME;

        packetSendBuffer.clear();
        packetSendBuffer.put(PING);
        packetSendBuffer.putLong(respondingTo);
        packetSendBuffer.putLong(pingID);
        packetSendBuffer.flip();

        pingTimers.put(pingID, 0.0);
        sendPacket(defaultChannel);
        p2pLogger.info("Sent ping " + pingID);
    }

    public static String sendCommand(String cmd) {
        packetSendBuffer.clear();
        packetSendBuffer.put(CMD);
        packetSendBuffer.put(cmd.getBytes(CHARSET));
        packetSendBuffer.flip();

        sendPacket(defaultChannel);
        return cmd;
    }

    public static void sendChatMessage(String msg) {
        packetSendBuffer.clear();
        packetSendBuffer.put(MSG);
        packetSendBuffer.put(msg.getBytes(CHARSET));
        packetSendBuffer.flip();

        sendPacket(defaultChannel);
    }

    private static void sendPacket(int channel) {
        if (currentPartner == null || !currentPartner.isValid())
            return;

        try {
            networking.sendP2PPacket(currentPartner, packetSendBuffer, SteamNetworking.P2PSend.Reliable, channel);
        }
        catch (SteamException e) {
            throw new RuntimeException(e);
        }
    }

    //Receiving
    public static void postUpdate()
    {
        try
        {
            if (currentPartner != null && currentPartner.isValid()) {
                retryPingTimer -= Math.min(0.25f, Gdx.graphics.getRawDeltaTime());
                Iterator<Map.Entry<Long, Double>> pingIterator = pingTimers.entrySet().iterator();
                Map.Entry<Long, Double> pingEntry;
                while (pingIterator.hasNext()) {
                    pingEntry = pingIterator.next();
                    pingEntry.setValue(pingEntry.getValue() + Gdx.graphics.getRawDeltaTime());
                    if (pingEntry.getValue() > TIMEOUT_TIME) {
                        ping.add(NO_CONNECTION_TIME);
                        pingIterator.remove();
                    }
                }

                msgSize[0] = 0;
                if (networking.isP2PPacketAvailable(defaultChannel, msgSize))
                {
                    connected = true;
                    packetReceiveBuffer.clear();

                    if (msgSize[0] > packetReceiveBuffer.capacity())
                        p2pLogger.warn((msgSize[0] - packetReceiveBuffer.capacity()) + " bytes lost.");

                    if (networking.readP2PPacket(msgSource, packetReceiveBuffer, defaultChannel) > 0 && packetReceiveBuffer.hasRemaining())
                    {
                        msgSize[0] -= packetReceiveBuffer.limit();
                        switch (packetReceiveBuffer.get()) {
                            case PING:
                                receivePing();
                                break;
                            case CMD:
                                receiveCmd();
                                break;
                            case MSG:
                                receiveMsg();
                                break;
                            default:
                                p2pLogger.info("Unknown packet type.");
                                break;
                        }
                    }
                }

                if (retryPingTimer <= 0) {
                    retryPingTimer = TIMEOUT_TIME;
                    sendPing(NON_RESPONSE);
                }
            }
        }
        catch (Exception e)
        {
            p2pLogger.error(e.getMessage());
        }
    }

    private static void receivePing() {
        retryPingTimer = TIMEOUT_TIME;

        if (P2P.packetReceiveBuffer.remaining() >= 16) {
            long respondPingID = P2P.packetReceiveBuffer.getLong();
            long recPingID = P2P.packetReceiveBuffer.getLong();

            Double delay = pingTimers.remove(respondPingID);
            if (delay == null)
                delay = respondPingID == NON_RESPONSE ? 0 : BAD_CONNECTION_TIME;
            ping.add(delay);

            p2pLogger.info("Received ping " + recPingID + " in response to ping " + respondPingID);

            if (recPingID > pingID) {
                pingID = recPingID;
                sendPing(recPingID);
            }
            else if (recPingID == pingID) {
                ++pingID;
                if (pingID > PING_ID_LIMIT)
                    pingID = -1;
                sendPing(recPingID);
            }
            else if (recPingID == -1) {
                pingID = 0;
                sendPing(recPingID);
            }
            //Older pings will not be responded to
        }
    }

    private static void receiveMsg() {
        try {
            String msg = decoder.decode(packetReceiveBuffer).toString();
            if (msgSource.isValid()) {
                if (msgSource.equals(currentPartner)) {
                    chat.receiveMessage(partnerName + ": " + msg.substring(3));
                }
                else {
                    chat.receiveMessage("?: " + msg.substring(3));
                }
            }
        }
        catch (CharacterCodingException e) {
            e.printStackTrace();
        }
    }

    private static void receiveCmd() {
        try {
            String msg = decoder.decode(packetReceiveBuffer).toString();
            if (FULL_DEBUG_LOGGING)
                p2pLogger.info("Received command: " + msg);

        }
        catch (CharacterCodingException e) {
            e.printStackTrace();
        }
    }


    public static void dispose()
    {
        reset();
        if (networking != null)
        {
            networking.dispose();
            networking = null;
        }
        callback = null;
    }
}
