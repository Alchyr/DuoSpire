package duospire.ui;

import basemod.CustomCharacterSelectScreen;
import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.*;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.screens.compendium.CardLibraryScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBarListener;
import duospire.DuoSpire;
import duospire.player.CoopPlayer;
import duospire.statics.Players;
import duospire.networking.gameplay.P2P;
import duospire.networking.matchmaking.Matchmaking;
import duospire.util.TextureLoader;

import java.lang.reflect.Method;
import java.util.*;

import static duospire.DuoSpire.*;
import static duospire.networking.matchmaking.Matchmaking.*;

/*
    Lobby creation/selection is done through a LobbyMenu
    Once in a lobby, the coop screen is really activated
    Exit button changes to exit the lobby
        - Utilize the confirmation thing used when abandoning a run? At least, the visuals

    Two sides. 1/4 on either side is a character button area, middle 1/2 is lobby info stuff and also displays chosen characters
        - Character buttons are laid out in a grid.
            - Size is based on number of buttons. If only 4, can probably make them pretty big and only one per row.
            - They should be scaled to *never* require a scrollbar. Hopefully.
            - Please don't use this mod with 7000 characters.
                - If a scrollbar is required, there would be one on each end.

 */
public class CoopMenuScreen {
    public static class Enum {
        @SpireEnum
        public static MainMenuScreen.CurScreen DUOSPIRE_COOP_MENU;
    }

    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(makeID("CoopMenuScreen"));
    public static final String[] TEXT = uiStrings.TEXT;

    private boolean clickedGroup = false;

    private final List<CharacterOption> characterOptions = new ArrayList<>();
    private final Set<String> otherPlayerOptions = new HashSet<>();

    private int mode;
    private static final int LOBBY_MENU = 0;
    private static final int JOINING_LOBBY = 1;
    private static final int IN_LOBBY = 2;
    private static final int ESTABLISH_P2P = 3;
    private static final int SETUP_GAME = 4;
    private static final int GAMEPLAY = 5;

    //UI
    public MenuCancelButton button = new MenuCancelButton();

    public final LobbyMenu lobbyMenu;

    private final Texture smallGround = TextureLoader.getTexture(resourcePath("img/smallground.png"));
    private final Color groundColor = Color.WHITE.cpy();
    private static final float CHAR_BASE_Y = 360f * Settings.scale;
    private static final float GROUND_Y = CHAR_BASE_Y - 128;
    private static final float ASC_Y = 210f * Settings.scale;
    private static final float EMBARK_Y = 100f * Settings.scale;
    private final CharSelectArea leftSelect = new CharSelectArea(false), rightSelect = new CharSelectArea(true);
    private CharSelectArea currentProcessing = null;

    private final Button embarkButton;

    private static final Color statusMsgColor = Color.WHITE.cpy();
    private String statusMsg = "";

    private static float ASC_LEFT_W;
    private static float ASC_RIGHT_W;
    private Hitbox ascensionModeHb;
    private Hitbox ascLeftHb;
    private Hitbox ascRightHb;

    public boolean isAscensionMode = false;
    public int ascensionLevel = 1;
    private String ascLevelInfoString = "";

    private final Set<String> confirmData = new HashSet<>();

    public CoopMenuScreen() {
        mode = LOBBY_MENU;
        lobbyMenu = new LobbyMenu();
        Matchmaking.setLobbyMenu(lobbyMenu);

        FontHelper.cardTitleFont.getData().setScale(1.0F);
        ASC_LEFT_W = FontHelper.getSmartWidth(FontHelper.cardTitleFont, CharacterSelectScreen.TEXT[6], 9999.0F, 0.0F);
        ASC_RIGHT_W = FontHelper.getSmartWidth(FontHelper.cardTitleFont, CharacterSelectScreen.TEXT[7] + "22", 9999.0F, 0.0F);

        this.ascensionModeHb = new Hitbox(ASC_LEFT_W + 100.0F * Settings.scale, 70.0F * Settings.scale);
        this.ascLeftHb = new Hitbox(70.0F * Settings.scale, 70.0F * Settings.scale);
        this.ascRightHb = new Hitbox(70.0F * Settings.scale, 70.0F * Settings.scale);

        this.ascensionModeHb.move(Settings.WIDTH / 2.0F - ASC_LEFT_W / 2.0F - 50.0F * Settings.scale, ASC_Y);
        this.ascLeftHb.move(Settings.WIDTH / 2.0F + 120F * Settings.scale - ASC_RIGHT_W * 0.25F, ASC_Y);
        this.ascRightHb.move(Settings.WIDTH / 2.0F + 120F * Settings.scale + ASC_RIGHT_W * 1.25F, ASC_Y);

        //centerWidth = Settings.WIDTH - (leftSelect.width + CharSelectArea.SIDE_SPACE + 20f * Settings.scale) * 2;

        embarkButton = new Button(Settings.WIDTH / 2.0f, EMBARK_Y, CharacterSelectScreen.TEXT[1]);

        groundColor.a = 0;
    }

    public void open() {
        CardCrawlGame.mainMenuScreen.screen = Enum.DUOSPIRE_COOP_MENU;
        CardCrawlGame.mainMenuScreen.darken();
        showLobbies(true);

        //enterLobby();
    }
    private void close() {
        Matchmaking.stopCoop();
    }

    public boolean viewingLobby() {
        return CardCrawlGame.mode == CardCrawlGame.GameMode.CHAR_SELECT
                && CardCrawlGame.mainMenuScreen.screen == Enum.DUOSPIRE_COOP_MENU
                && mode == IN_LOBBY;
    }

    public void startEmbark() {
        if (Matchmaking.inLobby() && isHost && Matchmaking.otherPlayer != null && Matchmaking.inLobby(Matchmaking.otherPlayer)) {
            if (leftSelect.selected != null && rightSelect.selected != null) {
                this.button.hide();

                Matchmaking.sendMessage(Matchmaking.FUNC + embarkKey + getOtherCharString());
                Matchmaking.sendSelectedChar(getLocalCharString());
                Matchmaking.lockLobby();
                mode = ESTABLISH_P2P;
                statusMsg = TEXT[2];
                P2P.startConnection(Matchmaking.otherPlayer, Matchmaking.otherPlayerName);
            }
        }
    }
    public void subEmbark() {
        if (Matchmaking.otherPlayer != null && Matchmaking.inLobby(Matchmaking.otherPlayer)) {
            mode = ESTABLISH_P2P;
            statusMsg = TEXT[2];
        }
    }
    public void finishEmbark() {
        if (Matchmaking.otherPlayer != null && Matchmaking.inLobby(Matchmaking.otherPlayer)) {
            mode = GAMEPLAY;

            CardCrawlGame.mainMenuScreen.isFadingOut = true;
            CardCrawlGame.mainMenuScreen.fadeOutMusic();
        }
    }

    private void sendDungeonInfo() {
        confirmData.clear();

        if (Settings.isTrial)
        {
            confirmData.add(P2P.sendCommand(P2P.TRIAL + Settings.specialSeed));
        }
        confirmData.add(P2P.sendCommand((Settings.seedSet ? P2P.SEEDSET : P2P.SEEDNORM) + Settings.seed.toString()));

        //Ascension level 0 = ascension disabled
        confirmData.add(P2P.sendCommand(P2P.ASCENSION + AbstractDungeon.ascensionLevel));

        //MultiplayerHelper.sendP2PString("start_game");
        //MultiplayerHelper.sendP2PMessage("Starting game...");
        matchmakingLogger.info("Sent dungeon info:");
        for (String s : confirmData) {
            matchmakingLogger.info(" - " + s);
        }
    }
    public void receiveConfirm(String data) {
        if (confirmData.remove(data)) {
            if (FULL_DEBUG_LOGGING)
                matchmakingLogger.info(data + " confirmed.");
        }
        else {
            matchmakingLogger.warn("Received unknown confirmation: " + data);
        }
    }

    public void update() {
        this.button.update();
        switch (mode) {
            case LOBBY_MENU:
                if (this.button.hb.clicked || InputHelper.pressedEscape) {
                    InputHelper.pressedEscape = false;
                    this.button.hb.clicked = false;

                    this.button.hide();
                    CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
                    CardCrawlGame.mainMenuScreen.lighten();
                    close();
                    return;
                }
                lobbyMenu.update();
                break;
            case IN_LOBBY:
                if (this.button.hb.clicked || InputHelper.pressedEscape) {
                    InputHelper.pressedEscape = false;
                    this.button.hb.clicked = false;

                    mode = LOBBY_MENU;
                    Matchmaking.leave();
                    Matchmaking.startFindLobby();
                    lobbyMenu.show(true);
                    this.button.show(CardLibraryScreen.TEXT[0]);
                    return;
                }
                groundColor.a = Math.min(1, groundColor.a + Gdx.graphics.getDeltaTime() * 1.5f);
                currentProcessing = leftSelect;
                leftSelect.update(isHost);
                currentProcessing = rightSelect;
                rightSelect.update(!isHost);
                currentProcessing = null;

                updateAscension();

                if (embarkButton.update()) {
                    startEmbark();
                    return;
                }
                break;
            case ESTABLISH_P2P:
                if (P2P.connected()) {
                    Matchmaking.leave();

                    mode = SETUP_GAME;
                    statusMsg = TEXT[3];
                    //Send data for game to ensure everything is consistent.

                    Players.setCoopPlayers(isHost ? 0 : 1, getLeftPlayerInstance(), getRightPlayerInstance());
                    CardCrawlGame.chosenCharacter = CoopPlayer.Enum.coopPlayer;
                    if (isHost) {
                        DuoSpire.setupDungeon();
                        sendDungeonInfo();
                    }
                }
                break;
            case SETUP_GAME:
                if (isHost) {
                    if (confirmData.isEmpty()) {
                        //The host is waiting for the second player to confirm they've received all data by sending the data back and it must match.
                        statusMsg = TEXT[4];
                        CardCrawlGame.mainMenuScreen.isFadingOut = true;
                        CardCrawlGame.mainMenuScreen.fadeOutMusic();
                        //See CardCrawlGame line 790 for code that embarks upon fade out completion
                        //TODO - Set up players
                        P2P.sendCommand(P2P.START_RUN);
                        mode = GAMEPLAY;
                    }
                } //Non-host player is just waiting for a "start game" message from host
                break;
            default:
        }

        updateScrolling();
    }

    private void updateScrolling() {
        if (isHost) {
            leftSelect.updateScrolling(true);
            rightSelect.updateScrolling(false);
        }
        else {
            rightSelect.updateScrolling(true);
            leftSelect.updateScrolling(false);
        }
    }

    private void updateAscension() {
        ascensionModeHb.update();
        ascRightHb.update();
        ascLeftHb.update();

        if (isHost) {
            boolean changed = false;

            if (InputHelper.justClickedLeft) {
                if (ascensionModeHb.hovered) {
                    ascensionModeHb.clickStarted = true;
                }
                if (isAscensionMode) {
                    if (ascRightHb.hovered) {
                        ascRightHb.clickStarted = true;
                    }
                    else if (ascLeftHb.hovered) {
                        ascLeftHb.clickStarted = true;
                    }
                }
            }

            if (ascensionModeHb.clicked || CInputActionSet.proceed.isJustPressed()) {
                ascensionModeHb.clicked = false;
                isAscensionMode = !isAscensionMode;
                Settings.gamePref.putBoolean("Ascension Mode Default", isAscensionMode);
                Settings.gamePref.flush();

                changed = true;
            }

            if (ascLeftHb.clicked || CInputActionSet.pageLeftViewDeck.isJustPressed()) {
                ascLeftHb.clicked = false;

                if (ascensionLevel > 1) {
                    changeAscensionLevel(--ascensionLevel);
                    changed = true;
                }
            }
            if (ascRightHb.clicked || CInputActionSet.pageRightViewExhaust.isJustPressed()) {
                ascRightHb.clicked = false;

                if (ascensionLevel < MAX_ASCENSION) {
                    changeAscensionLevel(++ascensionLevel);
                    changed = true;
                }
            }

            if (changed) {
                Matchmaking.sendAscension(isAscensionMode, ascensionLevel);
            }
        }
    }

    private void changeAscensionLevel(int level) {
        if (level >= 1 && level <= MAX_ASCENSION) {
            ascensionLevel = level;
            ascLevelInfoString = CharacterSelectScreen.A_TEXT[level - 1];
            coopPrefs.putInteger("LAST_ASCENSION_LEVEL", level);
            coopPrefs.flush();
        }
    }
    public void setAscension(boolean enabled, int level) {
        isAscensionMode = enabled;
        Settings.gamePref.putBoolean("Ascension Mode Default", isAscensionMode);
        Settings.gamePref.flush();

        changeAscensionLevel(level);
    }

    public void render(SpriteBatch sb) {
        switch (mode) {
            case IN_LOBBY:
                renderInLobby(sb);
            case LOBBY_MENU:
                this.lobbyMenu.render(sb);
                this.button.render(sb);
                break;
            default:
                FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, statusMsg, Settings.WIDTH / 2f, Settings.HEIGHT / 2f, statusMsgColor);
                break;
        }
    }

    private void renderInLobby(SpriteBatch sb) {
        sb.setColor(groundColor);
        sb.draw(smallGround, Settings.WIDTH / 2f - 350, GROUND_Y, 350, 128, 700, 256, Settings.scale, Settings.scale, 0, 0, 0, 700, 256, false, false);

        leftSelect.renderCharacter(sb);
        rightSelect.renderCharacter(sb);
        leftSelect.render(sb, isHost);
        rightSelect.render(sb, !isHost);
        embarkButton.render(sb);
        renderAscensionMode(sb);
    }

    private void renderAscensionMode(SpriteBatch sb) {
        sb.setColor(isHost ? Color.WHITE : Color.GRAY);
        sb.draw(ImageMaster.OPTION_TOGGLE, (float)Settings.WIDTH / 2.0F - ASC_LEFT_W - 16.0F - 40.0F * Settings.scale, this.ascensionModeHb.cY - 16.0F, 16.0F, 16.0F, 32.0F, 32.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 32, 32, false, false);

        if (this.ascensionModeHb.hovered) {
            FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, CharacterSelectScreen.TEXT[6], (float)Settings.WIDTH / 2.0F - ASC_LEFT_W / 2.0F, this.ascensionModeHb.cY, Settings.GREEN_TEXT_COLOR);
            TipHelper.renderGenericTip((float)InputHelper.mX - 140.0F * Settings.scale, (float)InputHelper.mY + 120.0F * Settings.scale, CharacterSelectScreen.TEXT[8], CharacterSelectScreen.TEXT[9]);
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, CharacterSelectScreen.TEXT[6], (float)Settings.WIDTH / 2.0F - ASC_LEFT_W / 2.0F, this.ascensionModeHb.cY, Settings.GOLD_COLOR);
        }

        FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, CharacterSelectScreen.TEXT[7] + ascensionLevel, (float)Settings.WIDTH / 2.0F + ASC_RIGHT_W / 2.0F + 120.0F * Settings.scale, this.ascensionModeHb.cY, Settings.BLUE_TEXT_COLOR);
        if (isAscensionMode) {
            sb.setColor(isHost ? Color.WHITE : Color.GRAY);
            sb.draw(ImageMaster.OPTION_TOGGLE_ON, (float)Settings.WIDTH / 2.0F - ASC_LEFT_W - 16.0F - 40.0F * Settings.scale, this.ascensionModeHb.cY - 16.0F, 16.0F, 16.0F, 32.0F, 32.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 32, 32, false, false);

            FontHelper.renderFontCentered(sb, FontHelper.cardDescFont_N, this.ascLevelInfoString, (float)Settings.WIDTH / 2.0F, ASC_Y - 35.0F * Settings.scale, Settings.CREAM_COLOR);
        }

        if (!this.ascLeftHb.hovered && !Settings.isControllerMode) {
            sb.setColor(isHost ? Color.LIGHT_GRAY : Color.DARK_GRAY);
        } else {
            sb.setColor(isHost ? Color.WHITE : Color.GRAY);
        }

        sb.draw(ImageMaster.CF_LEFT_ARROW, this.ascLeftHb.cX - 24.0F, this.ascLeftHb.cY - 24.0F, 24.0F, 24.0F, 48.0F, 48.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 48, 48, false, false);
        if (!this.ascRightHb.hovered && !Settings.isControllerMode) {
            sb.setColor(isHost ? Color.LIGHT_GRAY : Color.DARK_GRAY);
        } else {
            sb.setColor(isHost ? Color.WHITE : Color.GRAY);
        }

        sb.draw(ImageMaster.CF_RIGHT_ARROW, this.ascRightHb.cX - 24.0F, this.ascRightHb.cY - 24.0F, 24.0F, 24.0F, 48.0F, 48.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 48, 48, false, false);
        if (Settings.isControllerMode) {
            sb.setColor(isHost ? Color.WHITE : Color.GRAY);
            sb.draw(CInputActionSet.proceed.getKeyImg(), this.ascensionModeHb.cX - 100.0F * Settings.scale - 32.0F, this.ascensionModeHb.cY - 32.0F, 32.0F, 32.0F, 64.0F, 64.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 64, 64, false, false);
            sb.draw(CInputActionSet.pageLeftViewDeck.getKeyImg(), this.ascLeftHb.cX - 60.0F * Settings.scale - 32.0F, this.ascLeftHb.cY - 32.0F, 32.0F, 32.0F, 64.0F, 64.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 64, 64, false, false);
            sb.draw(CInputActionSet.pageRightViewExhaust.getKeyImg(), this.ascRightHb.cX + 60.0F * Settings.scale - 32.0F, this.ascRightHb.cY - 32.0F, 32.0F, 32.0F, 64.0F, 64.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 64, 64, false, false);
        }

        this.ascensionModeHb.render(sb);
        this.ascLeftHb.render(sb);
        this.ascRightHb.render(sb);
    }

    public void showLobbies(boolean startSearch) {
        this.button.show(CardLibraryScreen.TEXT[0]);

        Matchmaking.leave();
        mode = LOBBY_MENU;
        embarkButton.hideInstantly();
        lobbyMenu.show(startSearch);
        if (startSearch) {
            lobbyMenu.findLobbies();
        }
    }
    public void prepEnterLobby() {
        leftSelect.selected = null;
        rightSelect.selected = null;
        mode = JOINING_LOBBY;
        lobbyMenu.hide();

        otherPlayerOptions.clear();
        updateCharacters();

        this.statusMsg = TEXT[1];
    }
    public void enterLobby() {
        this.button.show(TEXT[0]);
        this.embarkButton.show();

        mode = IN_LOBBY;
        lobbyMenu.hide();
        updateCharacters();
        ascLevelInfoString = CharacterSelectScreen.A_TEXT[ascensionLevel - 1];
        leftSelect.show();
        rightSelect.show();

        if (leftSelect.selected != null && rightSelect.selected != null) {
            embarkButton.enable();
        }
        else {
            embarkButton.disable();
        }
    }

    public void resetOtherPlayer() {
        otherPlayerOptions.clear();
        if (isHost) {
            rightSelect.selected = null;
        }
        else {
            leftSelect.selected = null;
        }
        updateCharacters();
    }

    public void addAvailableCharacters(String[] chars) {
        otherPlayerOptions.addAll(Arrays.asList(chars));
        updateCharacters();
    }
    private void updateCharacters() {
        characterOptions.clear();
        if (CardCrawlGame.mainMenuScreen.charSelectScreen instanceof CustomCharacterSelectScreen) {
            characterOptions.addAll(ReflectionHacks.getPrivate(CardCrawlGame.mainMenuScreen.charSelectScreen, CustomCharacterSelectScreen.class, "allOptions"));
        }
        else {
            characterOptions.addAll(CardCrawlGame.mainMenuScreen.charSelectScreen.options);
        }
        if (!otherPlayerOptions.isEmpty()) {
            characterOptions.removeIf((o)->!otherPlayerOptions.contains(o.c.chosenClass.name()));
        }
        leftSelect.setOptions(characterOptions);
        rightSelect.setOptions(characterOptions);
    }

    public void selectCharOption(CharacterOption option) {
        if (currentProcessing != null) {
            currentProcessing.selected = option;

            if (isHost && leftSelect.selected != null && rightSelect.selected != null) {
                embarkButton.enable();
            }

            Matchmaking.sendSelectedChar(option);
        }
    }
    public void setPlayerChar(String charName) {
        if ((isHost ? leftSelect : rightSelect).setCharacter(charName)) {
            if (isHost && leftSelect.selected != null && rightSelect.selected != null) {
                embarkButton.enable();
            }
            return;
        }

        matchmakingLogger.info("Failed to set character to " + charName);
    }
    public void setOtherPlayerChar(String charName) {
        if ((isHost ? rightSelect : leftSelect).setCharacter(charName)) {
            if (isHost && leftSelect.selected != null && rightSelect.selected != null) {
                embarkButton.enable();
            }
            return;
        }

        //Most likely case is that joining the lobby removed the chosen character as the joining player did not have them
        matchmakingLogger.info("Failed to set character to " + charName);
    }
    public void setOtherPlayerScroll(float percent) {
        (isHost ? rightSelect : leftSelect).scrolledUsingBar(percent);
    }

    public AbstractPlayer getLeftPlayerInstance() {
        if (leftSelect.selected == null)
            return null;

        return null;
    }
    public AbstractPlayer getRightPlayerInstance() {
        if (rightSelect.selected == null)
            return null;

        return Players.getAltPlayer(rightSelect.selected.c);
    }

    public String getLocalCharString() {
        if (isHost) {
            return leftSelect.selected != null ? leftSelect.selected.c.chosenClass.name() : "null";
        }
        else {
            return rightSelect.selected != null ? rightSelect.selected.c.chosenClass.name() : "null";
        }
    }
    public String getOtherCharString() {
        if (isHost) {
            return rightSelect.selected != null ? rightSelect.selected.c.chosenClass.name() : "null";
        }
        else {
            return leftSelect.selected != null ? leftSelect.selected.c.chosenClass.name() : "null";
        }
    }

    private static class CharSelectArea implements ScrollBarListener {
        private static final Color LIGHT_GRAY = Color.LIGHT_GRAY.cpy(), WHITE = Color.WHITE.cpy(),
                                DARK_LIGHT_GRAY = Color.DARK_GRAY.cpy(), DARK_WHITE = Color.GRAY.cpy();
        private static final float SIDE_SPACE = 120f * Settings.scale;
        private static final int CHAR_OPTION_W = 220;
        private static final float SCROLL_W = 80f, CHAR_OPTION_SCALED = CHAR_OPTION_W * Settings.scale;
        private static final float CHAR_RENDER_MIN = -CHAR_OPTION_SCALED, CHAR_RENDER_MAX = Settings.HEIGHT + CHAR_OPTION_SCALED;
        private static final float PLAYER_OFFSET = 100f * Settings.scale;

        private static Method renderButton;
        static {
            try {
                renderButton = CharacterOption.class.getDeclaredMethod("renderOptionButton", SpriteBatch.class);
                renderButton.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        private boolean scrollEnabled = false;
        final ScrollBar scrollBar;

        private boolean grabbedScreen = false;
        private float grabStartY = 0.0F, currentDiffY = 0.0F;

        public final float lx, rx, width, charOptionX, playerX, playerY;

        private final float scrollLowerBound = 0;
        private float scrollUpperBound = 0;

        private final List<CharacterOption> characterOptions = new ArrayList<>();
        private final List<Float> charOptionY = new ArrayList<>();
        private final HashMap<Hitbox, Boolean> hoverMap = new HashMap<>();
        private final HashMap<Hitbox, Boolean> clickMap = new HashMap<>();
        private CharacterOption selected = null;

        public CharSelectArea(boolean onRight) {
            this.width = CHAR_OPTION_SCALED + SCROLL_W * Settings.scale;
            float offset = SIDE_SPACE + width - CHAR_OPTION_SCALED / 2f;
            this.charOptionX = onRight ? Settings.WIDTH - offset : offset;
            this.lx = onRight ? Settings.WIDTH - SIDE_SPACE - width : SIDE_SPACE;
            this.rx = onRight ? Settings.WIDTH - SIDE_SPACE : SIDE_SPACE + width;
            offset = SIDE_SPACE + SCROLL_W * Settings.scale / 2;

            playerX = Settings.WIDTH / 2f + (onRight ? PLAYER_OFFSET : -PLAYER_OFFSET);
            playerY = CHAR_BASE_Y + (onRight ? -15f * Settings.scale : 12f * Settings.scale);

            float barHeight = onRight ? Settings.HEIGHT - 256.0f * Settings.scale : (float)Settings.HEIGHT * 0.55f;
            float barTop = Settings.HEIGHT - 128.0f * Settings.scale;

            this.scrollBar = new ScrollBar(this, onRight ? Settings.WIDTH - offset : offset,
                    barTop - barHeight / 2f, barHeight);
        }

        public void show() {
            selected = null;
            this.currentDiffY = this.scrollLowerBound;
            hoverMap.clear();
            clickMap.clear();
        }

        public boolean setCharacter(String charName) {
            for (CharacterOption option : characterOptions) {
                if (charName.equals(option.c.chosenClass.name())) {
                    selected = option;
                    return true;
                }
            }
            if (charName.equals("null")) {
                selected = null;
                return true;
            }
            return false;
        }

        public void update(boolean interactable) {
            boolean wasClicking = InputHelper.justClickedLeft;
            if (!interactable)
                InputHelper.justClickedLeft = false;

            int lim = Math.min(characterOptions.size(), charOptionY.size());
            float cx, cy, tempy;
            boolean wasHovered, wasClicked;
            for (int i = 0; i < lim; ++i) {
                CharacterOption option = characterOptions.get(i);
                tempy = charOptionY.get(i) + currentDiffY;
                if (tempy > CHAR_RENDER_MIN && tempy < CHAR_RENDER_MAX) {
                    if (option.equals(selected))
                        option.selected = true;

                    cx = option.hb.cX;
                    cy = option.hb.cY;
                    option.hb.move(charOptionX, tempy);
                    wasHovered = option.hb.hovered;
                    wasClicked = option.hb.clickStarted;

                    option.hb.hovered = hoverMap.getOrDefault(option.hb, false);
                    option.hb.clickStarted = clickMap.getOrDefault(option.hb, false);
                    option.update();
                    hoverMap.put(option.hb, option.hb.hovered);
                    clickMap.put(option.hb, option.hb.clickStarted);

                    option.hb.hovered = wasHovered;
                    option.hb.clickStarted = wasClicked;
                    option.hb.move(cx, cy);
                    option.selected = false;
                }
            }

            InputHelper.justClickedLeft = wasClicking;
        }

        public void updateScrolling(boolean interactable) {
            if (scrollEnabled) {
                boolean hovered = InputHelper.mX >= this.lx && InputHelper.mX <= this.rx;

                if (interactable) {
                    float lastPos = currentDiffY;

                    boolean isScrollBarScrolling = this.scrollBar.update();
                    if (!isScrollBarScrolling) {
                        int y = InputHelper.mY;
                        if (!this.grabbedScreen) {
                            if (InputHelper.scrolledDown) {
                                this.currentDiffY += Settings.SCROLL_SPEED;
                            } else if (InputHelper.scrolledUp) {
                                this.currentDiffY -= Settings.SCROLL_SPEED;
                            }
                            if (hovered) {
                                if (InputHelper.justClickedLeft) {
                                    InputHelper.justClickedLeft = false;
                                    this.grabbedScreen = true;
                                    this.grabStartY = y - this.currentDiffY;
                                }
                            }
                        } else if (InputHelper.isMouseDown) {
                            this.currentDiffY = y - this.grabStartY;
                        } else {
                            this.grabbedScreen = false;
                        }
                        limitScrolling();
                        updateBarPosition();
                    }

                    if (lastPos != currentDiffY) {
                        Matchmaking.sendScrollPos((currentDiffY - scrollLowerBound) / (scrollUpperBound - scrollLowerBound));
                    }
                }
                else {
                    limitScrolling();
                    updateBarPosition();
                }
            }
        }

        protected void render(SpriteBatch sb, boolean primary) {
            try {
                if (!primary) {
                    Color.WHITE.set(DARK_WHITE);
                    Color.LIGHT_GRAY.set(DARK_LIGHT_GRAY);
                }

                if (scrollEnabled)
                    scrollBar.render(sb);

                int lim = Math.min(characterOptions.size(), charOptionY.size());
                float cx, cy, tempy;
                boolean wasHovered;
                for (int i = 0; i < lim; ++i) {
                    CharacterOption option = characterOptions.get(i);
                    tempy = charOptionY.get(i) + currentDiffY;
                    if (tempy > CHAR_RENDER_MIN && tempy < CHAR_RENDER_MAX) {
                        if (option.equals(selected))
                            option.selected = true;

                        cx = option.hb.cX;
                        cy = option.hb.cY;
                        option.hb.move(charOptionX, tempy);
                        wasHovered = option.hb.hovered;

                        option.hb.hovered = hoverMap.getOrDefault(option.hb, false);
                        renderButton.invoke(option, sb);

                        option.hb.hovered = wasHovered;
                        option.hb.move(cx, cy);
                        option.selected = false;
                    }
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            finally {
                if (!primary) {
                    Color.WHITE.set(WHITE);
                    Color.LIGHT_GRAY.set(LIGHT_GRAY);
                }
            }
        }

        protected void renderCharacter(SpriteBatch sb) {
            if (selected != null && selected.c != null) {
                selected.c.movePosition(playerX, playerY);
                selected.c.renderPlayerImage(sb);
            }
        }

        public void setOptions(List<CharacterOption> characterOptions) {
            this.characterOptions.clear();
            this.charOptionY.clear();
            this.characterOptions.addAll(characterOptions);

            float height = this.characterOptions.size() * CHAR_OPTION_SCALED;

            float startY = Settings.HEIGHT / 2f + height / 2f - CHAR_OPTION_SCALED / 2;

            scrollEnabled = height >= Settings.HEIGHT;
            if (scrollEnabled) {
                startY = Settings.HEIGHT - CHAR_OPTION_SCALED;
                this.scrollUpperBound = CHAR_OPTION_SCALED + (height - Settings.HEIGHT);
            }
            else {
                this.currentDiffY = 0;
                this.scrollUpperBound = Settings.DEFAULT_SCROLL_LIMIT;
            }

            for (int i = 0; i < this.characterOptions.size(); ++i) {
                charOptionY.add(startY);
                startY -= CHAR_OPTION_SCALED;
            }

            if (selected != null && !this.characterOptions.contains(selected)) {
                selected = null;
                sendSelectedChar("null");
            }
        }

        @SuppressWarnings("SuspiciousNameCombination")
        private void limitScrolling() {
            if (this.currentDiffY < this.scrollLowerBound) {
                this.currentDiffY = MathHelper.scrollSnapLerpSpeed(this.currentDiffY, this.scrollLowerBound);
            } else if (this.currentDiffY > this.scrollUpperBound) {
                this.currentDiffY = MathHelper.scrollSnapLerpSpeed(this.currentDiffY, this.scrollUpperBound);
            }
        }

        public void scrolledUsingBar(float newPercent) {
            if (scrollEnabled) {
                this.currentDiffY = MathHelper.valueFromPercentBetween(this.scrollLowerBound, this.scrollUpperBound, newPercent);
                updateBarPosition();
            }
        }
        private void updateBarPosition() {
            if (scrollEnabled) {
                float percent = MathHelper.percentFromValueBetween(this.scrollLowerBound, this.scrollUpperBound, this.currentDiffY);
                this.scrollBar.parentScrolledToPercent(percent);
            }
        }
    }
}