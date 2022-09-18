package duospire.networking.matchmaking;

import basemod.ReflectionHacks;
import com.codedisaster.steamworks.*;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.integrations.steam.SteamIntegration;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import duospire.networking.gameplay.P2P;
import duospire.patches.menu.CoopMenu;
import duospire.ui.LobbyMenu;
import duospire.util.GeneralUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static duospire.DuoSpire.*;

public class Matchmaking implements SteamMatchmakingCallback {
    private final ByteBuffer chatMessage = ByteBuffer.allocateDirect(4096);

    public static final String FUNC = "¤»§";

    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(makeID("Matchmaking"));
    private static final String[] TEXT = uiStrings.TEXT;

    private static final String loadingName = "<loading>";

    //Lobby info keys
    public static final String lobbyDuospireKey = "DUOSPIRE" + info.ModVersion.toString();
    public static final String lobbyNameKey = "name";
    public static final String lobbyPublicKey = "is_public";
    public static final String lobbyPasswordKey = "password";
    public static final String lobbyAscensionKey = "ascension";
    public static final String lobbySameModsKey = "same_mods";
    public static final String lobbyModsKey = "mod_list";
    private static final String lobbyKeysUnlockedKey = "final_act";

    //Chat message stuff
    public static final String chatMsgKey = "msg";
    public static final String nameKey = "nme";
    public static final String availableCharKey = "chr";
    public static final String selectedCharKey = "sel";
    public static final String endDataKey = "end";
    public static final String scrollPosKey = "scr";
    public static final String ascensionKey = "asc";
    public static final String embarkKey = "emb";

    public static final String metadataTrue = "true";
    public static final String metadataFalse = "false";

    public static SteamMatchmaking matchmaking;
    public static SteamFriends friends;
    public static Matchmaking handler;
    public static SteamUser currentUser;

    private static boolean searching = false;
    private static boolean triedFar = false;
    public static int goal = 0;
    public static final int JOIN_GOAL = 1, CREATE_GOAL = 2;

    private static LobbyMenu lobbyMenu;

    //lobby info
    public static SteamID currentLobbyID = null;
    public static SteamID otherPlayer = null;
    public static String otherPlayerName = loadingName;

    //public static boolean isHost; //matchmaking.getLobbyOwner


    public static void init()
    {
        dispose();

        if (SteamAPI.isSteamRunning()) {
            handler = new Matchmaking();

            matchmaking = new SteamMatchmaking(handler);
            currentUser = new SteamUser(new SteamUserCallbacks());

            try {
                friends = ReflectionHacks.getPrivateStatic(SteamIntegration.class, "steamFriends");
            }
            catch (Exception ignored) { }
        }

        searching = false;
    }

    public static void setLobbyMenu(LobbyMenu lobbyMenu) {
        Matchmaking.lobbyMenu = lobbyMenu;
    }

    private boolean startNormalSearch()
    {
        if (SteamAPI.isSteamRunning()) {
            searching = true;
            matchmaking.addRequestLobbyListDistanceFilter(SteamMatchmaking.LobbyDistanceFilter.Far);
            matchmakingLogger.info("distance: far");
            matchmaking.addRequestLobbyListStringFilter(lobbyDuospireKey, metadataTrue, SteamMatchmaking.LobbyComparison.Equal);
            //matchmaking.addRequestLobbyListStringFilter(lobbyModsKey, getModList(), SteamMatchmaking.LobbyComparison.Equal);
            //Settings.setFinalActAvailability(); //ensure it's updated.
            //matchmaking.addRequestLobbyListStringFilter(lobbyKeysUnlockedKey, Settings.isFinalActAvailable ? metadataTrue : metadataFalse, SteamMatchmaking.LobbyComparison.Equal);
            //matchmakingLogger.info("4th act unlocked: " + (Settings.isFinalActAvailable ? metadataTrue : metadataFalse));
            SteamAPICall lobbyRequest = matchmaking.requestLobbyList();
            triedFar = false;

            if (lobbyRequest.isValid()) {
                return true;
            }
            else {
                searching = false;
                return false;
            }
        }
        else {
            return false;
        }
    }

    private void startFarSearch()
    {
        searching = true;
        matchmaking.addRequestLobbyListDistanceFilter(SteamMatchmaking.LobbyDistanceFilter.Worldwide);
        matchmakingLogger.info("distance: worldwide");
        matchmaking.addRequestLobbyListStringFilter(lobbyDuospireKey, metadataTrue, SteamMatchmaking.LobbyComparison.Equal);
        //matchmaking.addRequestLobbyListStringFilter(lobbyModsKey, getModList(), SteamMatchmaking.LobbyComparison.Equal);
        //matchmaking.addRequestLobbyListStringFilter(lobbyCharacterKey, CardCrawlGame.chosenCharacter.name(), SteamMatchmaking.LobbyComparison.Equal);
        //matchmakingLogger.info("chosen character: " + CardCrawlGame.chosenCharacter.name());
        //Settings.setFinalActAvailability(); //ensure it's updated.
        //matchmaking.addRequestLobbyListStringFilter(lobbyKeysUnlockedKey, Settings.isFinalActAvailable ? metadataTrue : metadataFalse, SteamMatchmaking.LobbyComparison.Equal);
        //matchmakingLogger.info("4th act unlocked: " + (Settings.isFinalActAvailable ? metadataTrue : metadataFalse));
        matchmaking.requestLobbyList();
        triedFar = true;
    }

    public static boolean startFindLobby()
    {
        if (matchmaking != null)
        {
            if (!searching) {
                matchmakingLogger.info("Mod list: " + getModList());
                return handler.startNormalSearch();
            }
            return true;
        }
        else
        {
            matchmakingLogger.error("ERROR: Attempting to find Steam Lobby while SteamMatchmaking has not been initialized.");
            return false;
        }
    }

    public static void stopCoop()
    {
        if (searching)
        {
            matchmakingLogger.info("Leaving queue.");
            searching = false;
        }
        goal = 0;

        isHost = false;
        otherPlayer = null;
        otherPlayerName = loadingName;
        if (currentLobbyID != null && matchmaking != null)
        {
            matchmakingLogger.info("Left lobby " + currentLobbyID + ".");
            matchmaking.leaveLobby(currentLobbyID);
            currentLobbyID = null;
        }
        /*if (currentPartner != null)
        {
            MultiplayerHelper.sendP2PString("stop");
            currentPartner = null;
        }
        stopGameStart();*/
    }
    public static void leave()
    {
        isHost = false;
        otherPlayer = null;
        otherPlayerName = loadingName;
        if (currentLobbyID != null && matchmaking != null)
        {
            matchmakingLogger.info("Left lobby " + currentLobbyID + ".");
            matchmaking.leaveLobby(currentLobbyID);
            currentLobbyID = null;
        }
    }

    public static boolean inLobby()
    {
        return currentLobbyID != null && currentLobbyID.isValid();
    }
    public static boolean inLobby(SteamID member)
    {
        if (currentLobbyID != null && currentLobbyID.isValid() && matchmaking != null)
        {
            int max = matchmaking.getNumLobbyMembers(currentLobbyID);
            for (int i = 0; i < max; ++i)
            {
                if (member.equals(matchmaking.getLobbyMemberByIndex(currentLobbyID, i)))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onLobbyInvite(SteamID steamID, SteamID steamID1, long l) {

    }

    @Override
    public void onLobbyEnter(SteamID steamIDLobby, int chatPermissions, boolean blocked, SteamMatchmaking.ChatRoomEnterResponse response) {
        otherPlayer = null;
        otherPlayerName = loadingName;
        if (goal == JOIN_GOAL)
        {
            goal = 0;

            matchmakingLogger.info("Lobby entered: " + steamIDLobby);
            matchmakingLogger.info("  - response: " + response);

            int numMembers = matchmaking.getNumLobbyMembers(steamIDLobby);
            matchmakingLogger.info("  - " + numMembers + " members in lobby");

            if (numMembers != 2)
            {
                matchmaking.leaveLobby(steamIDLobby);

                Matchmaking.startFindLobby();
                lobbyMenu.show(true);
                lobbyMenu.showTempMessage(TEXT[5], 2);
            }
            else {
                CardCrawlGame.sound.play("DECK_OPEN");

                currentLobbyID = steamIDLobby;
                isHost = false;
                otherPlayer = matchmaking.getLobbyOwner(steamIDLobby);

                CoopMenu.screen.prepEnterLobby();
                sendStsName();
                sendAvailableCharacters();
            }
        }
        else
        {
            matchmakingLogger.info("Lobby entered. As queue has been exited, immediately leaving.");
            matchmaking.leaveLobby(steamIDLobby);
        }
    }

    @Override
    public void onLobbyChatUpdate(SteamID lobbyID, SteamID IDChanged, SteamID IDSource, SteamMatchmaking.ChatMemberStateChange chatMemberStateChange) {
        matchmakingLogger.info("Lobby chat update for " + lobbyID);
        matchmakingLogger.info("  - user changed: " + IDChanged.getAccountID());
        matchmakingLogger.info("  - made by user: " + IDSource.getAccountID());
        matchmakingLogger.info("  - state changed: " + chatMemberStateChange.name());

        if (currentLobbyID != null && currentUser != null) {
            if (isHost) {
                if (chatMemberStateChange == SteamMatchmaking.ChatMemberStateChange.Entered) {
                    if (matchmaking.getNumLobbyMembers(lobbyID) == 2) {
                        CardCrawlGame.sound.play("DECK_OPEN");
                        otherPlayer = IDChanged;
                        otherPlayerName = loadingName;
                        sendLobbyInfo();
                    } else {
                        matchmakingLogger.info("Incorrect number of players: " + matchmaking.getNumLobbyMembers(lobbyID));
                    }
                }
                else {
                    if (P2P.currentPartner != null && P2P.currentPartner.equals(otherPlayer)) {
                        //Swapping to p2p
                        otherPlayer = null;
                        otherPlayerName = loadingName;
                    }
                    //Left, disconnected, kicked, banned
                    else if (otherPlayer != null && matchmaking.getNumLobbyMembers(lobbyID) == 1) {
                        CardCrawlGame.sound.play("DECK_CLOSE");
                        otherPlayer = null;
                        otherPlayerName = loadingName;

                        CoopMenu.screen.resetOtherPlayer();
                    }
                }
            }
            else {
                if (chatMemberStateChange != SteamMatchmaking.ChatMemberStateChange.Entered) {
                    if (P2P.currentPartner != null && !P2P.currentPartner.equals(otherPlayer)) {
                        CardCrawlGame.sound.play("DECK_CLOSE");
                        otherPlayer = null;
                        otherPlayerName = loadingName;

                        CoopMenu.screen.resetOtherPlayer();

                        leave();
                        CoopMenu.screen.showLobbies(true);
                        CoopMenu.screen.lobbyMenu.showTempMessage(TEXT[4], 3);
                    }
                }
            }
        }
    }

    public static void sendChatMessage(String msg) {
        sendMessage(FUNC + chatMsgKey + msg);
    }
    public static void sendMessage(String msg)
    {
        if (currentLobbyID != null && currentLobbyID.isValid())
        {
            if (FULL_DEBUG_LOGGING)
                matchmakingLogger.info("Sending message: " + msg);
            if (!matchmaking.sendLobbyChatMsg(currentLobbyID, msg))
            {
                matchmakingLogger.error("Message failed to send.");
            }
            /*else
            {
                matchmakingLogger.info("Message sent successfully.");
            }*/
        }
        else
        {
            matchmakingLogger.info("No current lobby to send message to!");
        }
    }

    @Override
    public void onLobbyMatchList(int resultCount) {
        if (searching)
        {
            searching = false;
            matchmakingLogger.info("Found " + resultCount + " lobbies.");

            if (resultCount == 0 && !triedFar) {
                matchmakingLogger.info("Trying farther distances.");
                startFarSearch();
            }
            else if (resultCount == 0)
            {
                lobbyMenu.setLobbies(new ArrayList<>());
            }
            else if (resultCount > 0)
            {
                ArrayList<SteamID> lobbies = new ArrayList<>();
                for (int i = 0; i < resultCount; i++) {
                    SteamID lobby = matchmaking.getLobbyByIndex(i);
                    String sameMods = matchmaking.getLobbyData(lobby, lobbySameModsKey);
                    if (metadataTrue.equals(sameMods)) {
                        String modList = matchmaking.getLobbyData(lobby, lobbyModsKey);
                        if (getModList().equals(modList)) {
                            lobbies.add(lobby);
                        }
                    }
                    else {
                        lobbies.add(lobby);
                    }
                }
                listLobbies(lobbies);

                lobbyMenu.setLobbies(lobbies);

                /*
                int lastJoinAttempt = 0;

                matchmakingLogger.info("Joining the first lobby.");
                while (lastJoinAttempt < lobbies.size() && !lobbies.get(lastJoinAttempt).isValid())
                {
                    matchmakingLogger.info("Lobby " + (++lastJoinAttempt) + " is invalid, testing next lobby.");
                }

                if (lastJoinAttempt >= lobbies.size())
                {
                    searching = false;
                    matchmakingLogger.info("Attempting to create a new lobby.");
                    matchmakingLogger.info("Creating public lobby for 2 players.");
                    matchmaking.createLobby(SteamMatchmaking.LobbyType.Public,2);
                    joinorcreate = true;
                }
                else
                {
                    matchmakingLogger.info("Joining...");
                    matchmaking.joinLobby(lobbies.get(lastJoinAttempt));
                    searching = false;
                    isHost = false;
                    joinorcreate = true;
                }*/
            }
        }
    }

    @Override
    public void onLobbyCreated(SteamResult result, SteamID steamIDLobby) {
        if (goal == CREATE_GOAL)
        {
            goal = 0;

            matchmakingLogger.info("Lobby created: " + steamIDLobby);
            matchmakingLogger.info("  - result: " + result.name());

            switch (result) {
                case OK:
                    SteamID hostID = matchmaking.getLobbyOwner(steamIDLobby);

                    if (!currentUser.getSteamID().equals(hostID)) {
                        matchmakingLogger.error("Made lobby but current user is not host?");
                        matchmaking.leaveLobby(steamIDLobby);
                        lobbyMenu.showTempMessage(TEXT[6], 3);

                        startFindLobby();
                        lobbyMenu.show(true);
                    }
                    else {
                        String modList = getModList();
                        matchmakingLogger.info("  - lobby modlist: " + modList);
                        matchmaking.setLobbyData(steamIDLobby, lobbySameModsKey, lobbyMenu.sameModsToggle.enabled ? metadataTrue : metadataFalse);
                        matchmaking.setLobbyData(steamIDLobby, lobbyModsKey, modList);
                        matchmaking.setLobbyData(steamIDLobby, lobbyPublicKey, lobbyMenu.publicRoom.enabled ? metadataTrue : metadataFalse);
                        matchmaking.setLobbyData(steamIDLobby, lobbyAscensionKey, AbstractDungeon.isAscensionMode ? String.valueOf(AbstractDungeon.ascensionLevel) : "0");

                        matchmaking.setLobbyData(steamIDLobby, lobbyNameKey, lobbyMenu.nameInput.getCurrentText());

                        if (!lobbyMenu.publicRoom.enabled)
                        {
                            matchmaking.setLobbyData(steamIDLobby, lobbyPasswordKey, String.valueOf(lobbyMenu.passwordInput.getCurrentText().hashCode()));
                        }

                        matchmaking.setLobbyData(steamIDLobby, lobbyDuospireKey, metadataTrue);

                        currentLobbyID = steamIDLobby;
                        isHost = true;
                        matchmakingLogger.info(currentUser.getSteamID());

                        CardCrawlGame.sound.play("DECK_OPEN");
                        CoopMenu.screen.prepEnterLobby();
                        CoopMenu.screen.enterLobby();
                    }
                    break;
                case NoConnection:
                    lobbyMenu.showTempMessage(TEXT[7], 3);
                    lobbyMenu.viewLobbies();
                    break;
                case Timeout:
                    lobbyMenu.showTempMessage(TEXT[8], 3);
                    lobbyMenu.viewLobbies();
                    break;
                default:
                    lobbyMenu.showTempMessage(TEXT[6], 3);
                    lobbyMenu.viewLobbies();
                    break;
            }
        }
        else
        {
            if (result == SteamResult.OK)
            {
                matchmakingLogger.info("Lobby created successfully. However, queue has been exited, so immediately leaving lobby.");
            }
            matchmaking.setLobbyJoinable(steamIDLobby, false);
            matchmaking.leaveLobby(steamIDLobby);
        }
    }

    private final SteamMatchmaking.ChatEntry receivedChatEntry = new SteamMatchmaking.ChatEntry();
    @Override
    public void onLobbyChatMessage(SteamID lobbyID, SteamID userID, SteamMatchmaking.ChatEntryType chatEntryType, int msgIndex) {
        if (FULL_DEBUG_LOGGING) {
            matchmakingLogger.info("Lobby chat message for " + lobbyID);
            matchmakingLogger.info("  - from user: " + userID.getAccountID());
        }
        /*matchmakingLogger.info("  - chat entry type: " + chatEntryType);
        matchmakingLogger.info("  - chat id: #" + msgIndex);*/

        if (!userID.equals(currentUser.getSteamID())) {
            try {
                chatMessage.clear();
                int lim = matchmaking.getLobbyChatEntry(lobbyID, msgIndex, receivedChatEntry, chatMessage) - 1;
                //-1 as it includes a null terminator, which is unnecessary for a java string
                if (lim <= 0)
                    return;
                chatMessage.limit(lim);
                String msg = CHARSET.decode(chatMessage).toString();

                if (FULL_DEBUG_LOGGING)
                    matchmakingLogger.info(msg);

                if (msg.startsWith(FUNC)) {
                    if (inLobby(userID)) {
                        msg = msg.substring(3);

                        if (msg.length() < 3) {
                            return;
                        }
                        switch (msg.substring(0, 3)) {
                            case chatMsgKey:
                                if (msg.length() > 3)
                                    chat.receiveMessage(otherPlayerName + ": " + msg.substring(3));
                                break;
                            case nameKey:
                                if (msg.length() > 3) {
                                    otherPlayerName = msg.substring(3);
                                }
                                else {
                                    otherPlayerName = TEXT[9];
                                }
                                break;
                            case availableCharKey:
                                if (msg.length() > 3) {
                                    CoopMenu.screen.addAvailableCharacters(msg.substring(3).split(FUNC));
                                }
                                break;
                            case selectedCharKey:
                                if (msg.length() > 3) {
                                    CoopMenu.screen.setOtherPlayerChar(msg.substring(3));
                                }
                                break;
                            case scrollPosKey:
                                if (msg.length() > 3) {
                                    try {
                                        CoopMenu.screen.setOtherPlayerScroll(Float.parseFloat(msg.substring(3)));
                                    }
                                    catch (NumberFormatException e) {
                                        matchmakingLogger.error("Failed to parse float value.");
                                    }
                                }
                                break;
                            case endDataKey:
                                matchmakingLogger.info("All data received.");
                                CoopMenu.screen.enterLobby();
                                break;
                            case ascensionKey:
                                String[] data = msg.substring(3).split(FUNC);
                                if (data.length == 2) {
                                    try {
                                        CoopMenu.screen.setAscension(data[0].equals(metadataTrue), Integer.parseInt(data[1]));
                                    }
                                    catch (NumberFormatException e) {
                                        matchmakingLogger.error("Failed to parse Ascension level.");
                                    }
                                    catch (Exception e) {
                                        GeneralUtils.logStackTrace(matchmakingLogger, e);
                                    }
                                }
                                break;
                            case embarkKey:
                                //When that embark button gets clicked, whatever the host thinks your character is, is what you're getting.
                                if (msg.length() > 3) {
                                    CoopMenu.screen.subEmbark();
                                    CoopMenu.screen.setPlayerChar(msg.substring(3));
                                }
                                break;
                            default:
                                matchmakingLogger.info("Unknown func msg key \"" + msg.substring(0, 3) + "\"");
                                break;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                matchmakingLogger.error(e.getMessage());
            }
        }
    }

    @Override
    public void onLobbyKicked(SteamID lobbyID, SteamID user, boolean dc) {
        System.out.println("Kicked from lobby: " + lobbyID);
        System.out.println("  - by user: " + (user == null ? "N/A" : user.getAccountID()));
        System.out.println("  - kicked due to disconnect: " + (dc ? "yes" : "no"));

        currentLobbyID = null;
        isHost = false;
        otherPlayer = null;
        otherPlayerName = loadingName;
        lobbyMenu.show(true);
        //show temp message?
    }

    @Override
    public void onLobbyDataUpdate(SteamID lobbyID, SteamID steamIDMember, boolean success) {

    }

    @Override
    public void onLobbyGameCreated(SteamID steamID, SteamID steamID1, int i, short i1) {

    }

    @Override
    public void onFavoritesListChanged(int i, int i1, int i2, int i3, int i4, boolean b, int i5) {

    }

    @Override
    public void onFavoritesListAccountsUpdated(SteamResult steamResult) {

    }

    public static void lockLobby() {
        if (currentLobbyID != null && currentLobbyID.isValid() && isHost) {
            matchmaking.setLobbyJoinable(currentLobbyID, false);
        }
        else {
            matchmakingLogger.info("Failed to lock lobby.");
        }
    }

    private void listLobbies(ArrayList<SteamID> lobbies) {
        int index = 1;
        for (SteamID lobby : lobbies) {
            matchmakingLogger.info("   Match " + index++ + ":");
            if (lobby.isValid()) {
                int members = matchmaking.getNumLobbyMembers(lobby);
                matchmakingLogger.info(members + " members");
            } else {
                matchmakingLogger.info("Invalid SteamID");
            }
        }
    }

    private static String modlist = null;
    public static String getModList()
    {
        if (modlist == null) {
            StringBuilder sb = new StringBuilder();
            ArrayList<String> modData = new ArrayList<>();
            for (ModInfo m : Loader.MODINFOS)
            {
                modData.add(m.ID + ":" + m.ModVersion.toString());
            }
            modData.sort(String::compareTo);
            for (String s : modData)
            {
                sb.append(s).append("|");
            }
            modlist = sb.toString();
        }
        return modlist;
    }
    //Should only be called by host.
    private static void sendLobbyInfo() {
        sendStsName();
        sendAvailableCharacters();
        sendAscension(CoopMenu.screen.isAscensionMode, CoopMenu.screen.ascensionLevel);
        sendMessage(FUNC + selectedCharKey + CoopMenu.screen.getLocalChar());
        sendMessage(FUNC + endDataKey);
    }
    public static void sendAscension(boolean isAscensionMode, int ascensionLevel) {
        sendMessage(FUNC + ascensionKey + isAscensionMode + FUNC + ascensionLevel);
    }
    public static void sendStsName() {
        sendMessage(FUNC + nameKey + CardCrawlGame.playerName);
    }
    public static void sendSelectedChar(CharacterOption option) {
        if (otherPlayer != null && inLobby(otherPlayer)) {
            sendMessage(FUNC + selectedCharKey + (option == null ? "null" : option.c.chosenClass.name()));
        }
    }
    public static void sendSelectedChar(String optionName) {
        if (otherPlayer != null && inLobby(otherPlayer)) {
            sendMessage(FUNC + selectedCharKey + optionName);
        }
    }
    public static void sendScrollPos(float percent) {
        if (otherPlayer != null && inLobby(otherPlayer)) {
            sendMessage(FUNC + scrollPosKey + percent);
        }
    }
    private static void sendAvailableCharacters() {
        List<String> msgs = new ArrayList<>();
        StringBuilder sb = new StringBuilder(FUNC).append(availableCharKey);
        boolean sendSb = false;

        for (AbstractPlayer.PlayerClass c : AbstractPlayer.PlayerClass.values()) {
            sb.append(c.name()).append(FUNC);
            sendSb = true;
            if (sb.length() > 256) {
                msgs.add(sb.toString());
                sb.setLength(0);
                sb.append(FUNC).append(availableCharKey);
                sendSb = false;
            }
        }

        for (String s : msgs) {
            sendMessage(s);
        }
        if (sendSb) {
            sendMessage(sb.toString());
        }
    }

    public static void dispose()
    {
        if (matchmaking != null)
        {
            leave();
            matchmaking.dispose();
            matchmaking = null;
        }
        if (currentUser != null)
        {
            currentUser.dispose();
            currentUser = null;
        }
    }
}
