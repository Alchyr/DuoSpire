package duospire.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamMatchmaking;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.*;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.screens.options.ToggleButton;
import duospire.patches.enums.ToggleType;
import duospire.patches.menu.CoopMenu;
import duospire.util.TextureLoader;
import duospire.networking.matchmaking.LobbyData;
import duospire.networking.matchmaking.Matchmaking;

import java.util.ArrayList;
import java.util.List;

import static duospire.DuoSpire.*;
import static duospire.networking.matchmaking.Matchmaking.*;

public class LobbyMenu {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(makeID("LobbyPanel"));
    private static final String[] TEXT = uiStrings.TEXT;

    private static final Texture panelBack = TextureLoader.getTexture(resourcePath("img/ui/lobbyPanel.png"));
    private static final Texture refreshButton = TextureLoader.getTexture(resourcePath("img/ui/refresh.png"));
    private static final Texture createButton = TextureLoader.getTexture(resourcePath("img/ui/new.png"));
    private static final Texture arrow = TextureLoader.getTexture(resourcePath("img/ui/downArrow.png"));

    private static final float LINE_HEIGHT = FontHelper.buttonLabelFont.getCapHeight() + 8.0f;

    private static final int PANEL_WIDTH = 840;
    private static final int PANEL_HEIGHT = 600;

    private static final float PANEL_X = Settings.WIDTH / 2.0f - PANEL_WIDTH * Settings.scale / 2f;
    private static final float PANEL_Y = Settings.HEIGHT / 2.0f - PANEL_HEIGHT * Settings.scale / 2f;

    private static final float BUTTON_Y = PANEL_Y + PANEL_HEIGHT * Settings.scale + 10.0f * Settings.scale;
    private static final int BUTTON_SIZE = 56;

    private static final float BUTTON_OFFSET = BUTTON_SIZE / 2.0f;

    private static final float REFRESH_X = PANEL_X + (PANEL_WIDTH * Settings.scale) - 90.0f * Settings.scale;
    private static final float CREATE_X = PANEL_X + (PANEL_WIDTH * Settings.scale) - 156.0f * Settings.scale;

    private static final float PANEL_CENTER_X = PANEL_X + (PANEL_WIDTH / 2.0f) * Settings.scale;
    private static final float PANEL_CENTER_Y = PANEL_Y + (PANEL_HEIGHT / 2.0f) * Settings.scale;

    private static final float OTHER_RENDER_OFFSET = 12.0f * Settings.scale;

    private static final float LABEL_Y = PANEL_Y + 525.0f * Settings.scale;
    private static final float OTHER_RENDER_LABEL_Y = LABEL_Y + OTHER_RENDER_OFFSET;
    private static final float LOBBY_START_Y = LABEL_Y - (LINE_HEIGHT + 7.0f * Settings.scale);

    private static final float LOBBY_WIDTH = 700.0f * Settings.scale;

    //for lobby list
    private static final float NAME_LABEL_X = PANEL_X + 75.0f * Settings.scale;
    private static final float ISPUBLIC_LABEL_X = PANEL_X + 450.0f * Settings.scale;
    private static final float ASCENSION_SYMBOL_X = PANEL_X + 340.0f * Settings.scale;
    private static final float HASSAMEMODS_LABEL_X = PANEL_X + 660.0f * Settings.scale;

    private static final float ICON_W = 64.0F * Settings.scale;

    //for lobby creation
    private static final float SAME_MODS_REQUIRED_LABEL_Y = PANEL_Y + 312.0f * Settings.scale;

    private static final float CREATE_TEXT_Y = PANEL_Y + 100.0f * Settings.scale;

    private static final float TOGGLE_X = PANEL_X + 130.0f * Settings.scale;
    private static final float TOGGLE_LABEL_X = PANEL_X + 155.0f * Settings.scale;
    private static final float PUBLIC_TOGGLE_Y = PANEL_Y + 450.0f * Settings.scale;
    private static final float PUBLIC_INPUT_TEXT_Y = PANEL_Y + 462.0f * Settings.scale;
    private static final float PASSWORD_LABEL_Y = PANEL_Y + 390.0f * Settings.scale;
    private static final float PASSWORD_INPUT_Y = PASSWORD_LABEL_Y - LobbyTextInput.HEIGHT / 2.0f * Settings.scale - 12.0f * Settings.scale;

    private static final float SAME_MODS_TOGGLE_Y = PANEL_Y + 300.0f * Settings.scale;

    private static final float INPUT_X = PANEL_X + 200.0f * Settings.scale;

    private static final float ARROW_X = PANEL_X + 770.0f * Settings.scale;
    private static final float NEXT_ARROW_Y = PANEL_Y + 55.0f * Settings.scale;
    private static final float PREV_ARROW_Y = LOBBY_START_Y + 12.0f * Settings.scale;
    private static final int ARROW_WIDTH = 20;
    private static final int ARROW_HEIGHT = 24;
    private static final float ARROW_OFFSET_X = ARROW_WIDTH / 2.0f;
    private static final float ARROW_OFFSET_Y = ARROW_HEIGHT / 2.0f;

    private static final int LOBBIES_PER_PAGE = (int) ((460f * Settings.scale) / LINE_HEIGHT);

    private final Color refreshButtonColor = new Color(0.8f, 0.8f, 0.8f, 1.0f);
    private float refreshButtonScale = 1.0f;

    private final Color createButtonColor = new Color(0.8f, 0.8f, 0.8f, 1.0f);
    private float createButtonScale = 1.0f;

    private final Hitbox refreshButtonHitbox = new Hitbox(REFRESH_X + (BUTTON_SIZE / 2.0f * (1 - Settings.scale)), BUTTON_Y + (BUTTON_SIZE / 2.0f * (1 - Settings.scale)), BUTTON_SIZE * Settings.scale, BUTTON_SIZE * Settings.scale);
    private final Hitbox createButtonHitbox = new Hitbox(CREATE_X + (BUTTON_SIZE / 2.0f * (1 - Settings.scale)), BUTTON_Y + (BUTTON_SIZE / 2.0f * (1 - Settings.scale)), BUTTON_SIZE * Settings.scale, BUTTON_SIZE * Settings.scale);
    private final Hitbox lobbyAreaHitbox = new Hitbox(NAME_LABEL_X - 5.0f * Settings.scale, PANEL_Y + 50.0f * Settings.scale, LOBBY_WIDTH, 450.0f * Settings.scale);

    private final Hitbox prevArrowHitbox = new Hitbox(ARROW_X, PREV_ARROW_Y, ARROW_WIDTH * Settings.scale, ARROW_HEIGHT * Settings.scale);
    private final Hitbox nextArrowHitbox = new Hitbox(ARROW_X, NEXT_ARROW_Y, ARROW_WIDTH * Settings.scale, ARROW_HEIGHT * Settings.scale);

    private final Hitbox lobbyCreateHitbox = new Hitbox(PANEL_CENTER_X - 90.0f * Settings.scale, CREATE_TEXT_Y - LINE_HEIGHT / 2.0f, 180 * Settings.scale, LINE_HEIGHT);

    private final ArrayList<Hitbox> lobbyHitboxes = new ArrayList<>();

    //Lobby Creation UI
    private final float ASC_LEFT_W;
    private final float ASC_RIGHT_W;

    public final LobbyTextInput nameInput = new LobbyTextInput(INPUT_X, LOBBY_START_Y).setCharLimit(15);
    public final ToggleButton publicRoom = new ToggleButton(TOGGLE_X, 0, PUBLIC_TOGGLE_Y, ToggleType.DUOSPIRE_TOGGLE, false);
    public final LobbyTextInput passwordInput = new LobbyTextInput(INPUT_X, PASSWORD_INPUT_Y).setDisplayFilter(LobbyTextInput.passwordFilter);
    public final ToggleButton sameModsToggle = new ToggleButton(TOGGLE_X, 0, SAME_MODS_TOGGLE_Y, ToggleType.DUOSPIRE_TOGGLE, false);

    private final Hitbox ascensionModeHb;
    private final Hitbox ascLeftHb;
    private final Hitbox ascRightHb;

    public boolean visible = false;

    private int mode = 0; //0 is searching/viewing lobbies, 1 is joining a lobby, 2 is entering a password, 3 is creation menu, 4 is lobby being made (then goes to other screen)
    public boolean searching = false;
    private LobbyData waitingLobby = null;
    private float tempMessageTimer = 0; //Temporarily replaces update/rendering. Clicking on lobby area instantly sets timer to 0.
    private String tempMessage = "";

    private final ArrayList<LobbyData> lobbies = new ArrayList<>();
    private int page = 0;
    private int maxPage = 0;

    private int hoveredIndex = -1;

    public LobbyMenu()
    {
        float x = NAME_LABEL_X - 5.0f * Settings.scale;
        float y = LOBBY_START_Y;
        for (int i = 0; i < LOBBIES_PER_PAGE; ++i)
        {
            lobbyHitboxes.add(new Hitbox(x, y - LINE_HEIGHT / 2 + 1, LOBBY_WIDTH, LINE_HEIGHT - 2));
            y -= LINE_HEIGHT;
        }

        sameModsToggle.enabled = true;

        FontHelper.cardTitleFont.getData().setScale(1.0F);
        this.ASC_LEFT_W = FontHelper.getSmartWidth(FontHelper.cardTitleFont, TEXT[17], 9999.0F, 0.0F); //Width of "Ascension"
        this.ASC_RIGHT_W = FontHelper.getSmartWidth(FontHelper.cardTitleFont, TEXT[18] + "22", 9999.0F, 0.0F); //Width of "Level 22"

        this.ascensionModeHb = new Hitbox(ASC_LEFT_W + 100.0F * Settings.scale, 50.0F * Settings.scale);
        this.ascLeftHb = new Hitbox(70.0F * Settings.scale, 70.0F * Settings.scale);
        this.ascRightHb = new Hitbox(70.0F * Settings.scale, 70.0F * Settings.scale);

        this.ascensionModeHb.move(PANEL_CENTER_X - ASC_LEFT_W / 2.0F - 60.0F * Settings.scale, PANEL_Y + 200.0F * Settings.scale);
        this.ascLeftHb.move(PANEL_CENTER_X + 110.0F * Settings.scale - ASC_RIGHT_W * 0.5F, PANEL_Y + 200.0F * Settings.scale);
        this.ascRightHb.move(PANEL_CENTER_X + 110.0F * Settings.scale + ASC_RIGHT_W * 1.5F, PANEL_Y + 200.0F * Settings.scale);
    }

    public void show(boolean searching)
    {
        checkAscension();
        this.lobbies.clear();
        this.mode = SteamAPI.isSteamRunning() ? 0 : -1;
        this.searching = searching;
        Matchmaking.goal = 0;
        this.visible = true;
    }
    public void show(List<SteamID> lobbies)
    {
        checkAscension();
        this.mode = SteamAPI.isSteamRunning() ? 0 : -1;
        this.visible = true;
        this.searching = false;
        Matchmaking.goal = 0;
        setLobbies(lobbies);
    }

    private void checkAscension() {
        CoopMenu.screen.ascensionLevel = Math.min(coopPrefs.getInteger("LAST_ASCENSION_LEVEL", 1), 20);
        coopPrefs.putInteger("LAST_ASCENSION_LEVEL", CoopMenu.screen.ascensionLevel);
        coopPrefs.flush();
    }

    public void viewLobbies() {
        mode = 0;
    }
    public void setLobbies(List<SteamID> lobbies)
    {
        this.lobbies.clear();
        searching = false;

        if (!lobbies.isEmpty()) {
            for (SteamID lobby : lobbies) {
                LobbyData data = new LobbyData();

                data.id = lobby;
                if (friends != null) {
                    SteamID lobbyOwner = matchmaking.getLobbyOwner(lobby);
                    switch (friends.getFriendRelationship(lobbyOwner)) {
                        case Friend:
                            data.isFriend = true;
                            break;
                        case Ignored:
                        case Blocked:
                            continue;
                    }
                }

                data.name = matchmaking.getLobbyData(lobby, Matchmaking.lobbyNameKey);
                data.sameMods = metadataTrue.equals(matchmaking.getLobbyData(lobby, Matchmaking.lobbySameModsKey));
                data.isPublic = metadataTrue.equals(matchmaking.getLobbyData(lobby, Matchmaking.lobbyPublicKey));
                data.ascension = Integer.parseInt(matchmaking.getLobbyData(lobby, Matchmaking.lobbyAscensionKey));

                this.lobbies.add(data);
            }

            this.lobbies.sort(new LobbyData.LobbyDataComparer());
            page = 0;
            hoveredIndex = -1;
            maxPage = this.lobbies.size() / LOBBIES_PER_PAGE;
        }

        //debug code for viewing layout
        /*while (this.lobbies.size() < LOBBIES_PER_PAGE * 2.5)
        {
            LobbyData testData = new LobbyData();
            testData.id = null;
            testData.isFriend = MathUtils.randomBoolean(0.1f);
            testData.sameMods = MathUtils.randomBoolean();
            testData.isPublic = MathUtils.randomBoolean();
            testData.name = "Lobby " + (this.lobbies.size() + 1);
            this.lobbies.add(testData);
        }
        this.lobbies.sort(new LobbyData.LobbyDataComparer());
        maxPage = this.lobbies.size() / LOBBIES_PER_PAGE;*/
    }

    public void showTempMessage(String msg, float time) {
        tempMessageTimer = time;
        tempMessage = msg;
    }

    public void hide()
    {
        this.visible = false;
        mode = 0;
    }

    public void update()
    {
        if (visible)
        {
            if (tempMessageTimer > 0) {
                tempMessageTimer -= Gdx.graphics.getRawDeltaTime();

                lobbyAreaHitbox.update();
                updateTopButtons();

                if (refreshButtonHitbox.hovered && InputHelper.justClickedLeft)
                {
                    InputHelper.justClickedLeft = false;
                    findLobbies();

                    tempMessageTimer = 0;
                }
                else if (createButtonHitbox.hovered && InputHelper.justClickedLeft)
                {
                    InputHelper.justClickedLeft = false;
                    startCreate();

                    tempMessageTimer = 0;
                }
                if ((lobbyAreaHitbox.hovered && InputHelper.justClickedLeft) || tempMessageTimer < 0) {
                    InputHelper.justClickedLeft = false;
                    tempMessageTimer = 0;
                }
                return;
            }
            switch (mode) {
                case 0:
                {
                    lobbyAreaHitbox.update();
                    updateTopButtons();

                    hoveredIndex = -1;
                    if (refreshButtonHitbox.hovered && InputHelper.justClickedLeft) {
                        InputHelper.justClickedLeft = false;
                        findLobbies();
                    }
                    else if (createButtonHitbox.hovered && InputHelper.justClickedLeft) {
                        InputHelper.justClickedLeft = false;
                        startCreate();
                    }
                    else if (lobbyAreaHitbox.hovered && !searching) {
                        int max = Math.min(lobbies.size(), (page * LOBBIES_PER_PAGE + LOBBIES_PER_PAGE));

                        int index = 0;
                        for (int i = page * LOBBIES_PER_PAGE; i < max && index < lobbyHitboxes.size(); ++i) {
                            lobbyHitboxes.get(index).update();

                            if (lobbyHitboxes.get(index).hovered) {
                                hoveredIndex = i;
                                if (InputHelper.justClickedLeft) {
                                    InputHelper.justClickedLeft = false;
                                    if (lobbies.get(i).id != null && lobbies.get(i).id.isValid()) {
                                        mode = 1;
                                        matchmakingLogger.info("Joining...");

                                        if (lobbies.get(i).isPublic) {
                                            matchmaking.joinLobby(lobbies.get(i).id);
                                            Matchmaking.goal = JOIN_GOAL;
                                        }
                                        else {
                                            waitingLobby = lobbies.get(i);
                                            getPassword();
                                        }
                                    }
                                    else {
                                        showTempMessage(TEXT[7], 1);
                                        lobbies.remove(i).invalidate();
                                    }
                                }
                                break;
                            }
                            ++index;
                        }
                    }

                    nextArrowHitbox.update();
                    prevArrowHitbox.update();

                    if (nextArrowHitbox.hovered && InputHelper.justClickedLeft) {
                        InputHelper.justClickedLeft = false;
                        if (page < maxPage)
                            ++page;
                    }
                    else if (prevArrowHitbox.hovered && InputHelper.justClickedLeft) {
                        InputHelper.justClickedLeft = false;
                        if (page > 0)
                            --page;
                    }
                    break;
                }
                case 1: //joining
                case 4: //Waiting for lobby creation
                {
                    lobbyAreaHitbox.update();
                    updateTopButtons();

                    //Cancel join if buttons are clicked
                    if (refreshButtonHitbox.hovered && InputHelper.justClickedLeft) {
                        InputHelper.justClickedLeft = false;
                        Matchmaking.goal = 0;

                        findLobbies();
                    }
                    else if (createButtonHitbox.hovered && InputHelper.justClickedLeft) {
                        InputHelper.justClickedLeft = false;
                        Matchmaking.goal = 0;

                        startCreate();
                    }
                    break;
                }
                case 2: //password
                {
                    lobbyAreaHitbox.update();
                    updateTopButtons();

                    if (refreshButtonHitbox.hovered && InputHelper.justClickedLeft) {
                        InputHelper.justClickedLeft = false;

                        findLobbies();
                    }
                    else if (createButtonHitbox.hovered && InputHelper.justClickedLeft) {
                        InputHelper.justClickedLeft = false;

                        startCreate();
                    }
                    else if (lobbyCreateHitbox.hovered && InputHelper.justClickedLeft) {
                        tryPassword();
                    }
                    else {
                        passwordInput.update();
                    }
                    break;
                }
                case 3: //creating
                {
                    lobbyAreaHitbox.update();
                    updateTopButtons();

                    if (refreshButtonHitbox.hovered && InputHelper.justClickedLeft) {
                        InputHelper.justClickedLeft = false;
                        findLobbies();
                    }
                    else {
                        nameInput.update();
                        publicRoom.update();
                        lobbyCreateHitbox.update();

                        ascensionModeHb.update();
                        ascRightHb.update();
                        ascLeftHb.update();

                        if (lobbyCreateHitbox.hovered && InputHelper.justClickedLeft) {
                            mode = 4;

                            Matchmaking.goal = CREATE_GOAL;
                            logger.info("Attempting to create a new lobby.");
                            matchmaking.createLobby(SteamMatchmaking.LobbyType.Public, 2);
                            break;
                        }

                        if (!publicRoom.enabled) {
                            passwordInput.update();
                        }

                        sameModsToggle.update();

                        if (InputHelper.justClickedLeft) {
                            if (ascensionModeHb.hovered) {
                                ascensionModeHb.clickStarted = true;
                            }
                            if (CoopMenu.screen.isAscensionMode) {
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
                            CoopMenu.screen.isAscensionMode = !CoopMenu.screen.isAscensionMode;
                            Settings.gamePref.putBoolean("Ascension Mode Default", CoopMenu.screen.isAscensionMode);
                            Settings.gamePref.flush();
                        }

                        if (ascLeftHb.clicked || CInputActionSet.pageLeftViewDeck.isJustPressed()) {
                            ascLeftHb.clicked = false;

                            if (CoopMenu.screen.ascensionLevel > 1) {
                                changeAscensionLevel(--CoopMenu.screen.ascensionLevel);
                            }
                        }
                        if (ascRightHb.clicked || CInputActionSet.pageRightViewExhaust.isJustPressed()) {
                            ascRightHb.clicked = false;

                            if (CoopMenu.screen.ascensionLevel < MAX_ASCENSION) {
                                changeAscensionLevel(++CoopMenu.screen.ascensionLevel);
                            }
                        }

                        AbstractDungeon.isAscensionMode = CoopMenu.screen.isAscensionMode;
                        if (CoopMenu.screen.isAscensionMode) {
                            AbstractDungeon.ascensionLevel = CoopMenu.screen.ascensionLevel;
                        }
                        else {
                            AbstractDungeon.ascensionLevel = 0;
                        }
                    }
                    break;
                }//Cancel creation if buttons are clicked
            }
        }
    }

    private void updateTopButtons() {
        refreshButtonHitbox.update();
        createButtonHitbox.update();

        if (refreshButtonHitbox.hovered) {
            refreshButtonScale = 1.15f;
            refreshButtonColor.r = 1.0f;
            refreshButtonColor.g = 1.0f;
            refreshButtonColor.b = 1.0f;
        }
        else
        {
            refreshButtonScale = 1.0f;
            refreshButtonColor.r = 0.8f;
            refreshButtonColor.g = 0.8f;
            refreshButtonColor.b = 0.8f;
        }

        if (createButtonHitbox.hovered) {
            createButtonScale = 1.15f;
            createButtonColor.r = 1.0f;
            createButtonColor.g = 1.0f;
            createButtonColor.b = 1.0f;
        }
        else
        {
            createButtonScale = 1.0f;
            createButtonColor.r = 0.8f;
            createButtonColor.g = 0.8f;
            createButtonColor.b = 0.8f;
        }
    }

    public void findLobbies() {
        searching = true;
        mode = 0;
        passwordInput.reset();
        waitingLobby = null;
        this.lobbies.clear();
        if (!Matchmaking.startFindLobby()) {
            showTempMessage(TEXT[24], 3f);
            searching = false;
        }
    }
    private void startCreate() {
        mode = 3;

        if (!publicRoom.enabled)
            publicRoom.toggle();

        nameInput.reset();
        nameInput.setText(CardCrawlGame.playerName + TEXT[10]);
        waitingLobby = null;
        passwordInput.reset();

        CoopMenu.screen.isAscensionMode = Settings.gamePref.getBoolean("Ascension Mode Default", false);
    }

    private void getPassword() {
        mode = 2;
        passwordInput.reset();
        passwordInput.setOnEnter(this::tryPassword);
    }

    private void tryPassword() {
        if (mode == 2 && waitingLobby != null)
        {
            if (waitingLobby.isValid && waitingLobby.id.isValid()) {
                if (String.valueOf(passwordInput.getCurrentText().hashCode()).equals(matchmaking.getLobbyData(waitingLobby.id, Matchmaking.lobbyPasswordKey)))
                {
                    mode = 1;
                    matchmakingLogger.info("Joining...");

                    matchmaking.joinLobby(waitingLobby.id);
                    Matchmaking.goal = JOIN_GOAL;
                    waitingLobby = null;
                }
                else
                {
                    showTempMessage(TEXT[16], 1);
                }
            }
            else {
                showTempMessage(TEXT[7], 1);
                lobbies.remove(waitingLobby);
                waitingLobby.invalidate();
                waitingLobby = null;
            }
        }
    }

    private void changeAscensionLevel(int level) {
        if (level >= 1 && level <= MAX_ASCENSION) {
            CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel = level;
            CardCrawlGame.mainMenuScreen.charSelectScreen.ascLevelInfoString = CharacterSelectScreen.A_TEXT[level - 1];
            coopPrefs.putInteger("LAST_ASCENSION_LEVEL", level);
            coopPrefs.flush();
        }
    }

    public void render(SpriteBatch sb)
    {
        if (visible)
        {
            sb.setColor(Color.WHITE);
            sb.draw(panelBack, PANEL_X, PANEL_Y, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, Settings.scale, Settings.scale, 0, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, false, false);

            if (mode >= 0) {
                renderTopButtons(sb);

                if (tempMessageTimer > 0) {
                    FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, tempMessage, PANEL_CENTER_X, PANEL_CENTER_Y, Color.WHITE);
                    return;
                }

                switch (mode) {
                    case 0: //Viewing lobbies/searching
                        if (searching) {
                            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[6], PANEL_CENTER_X, PANEL_CENTER_Y, Color.WHITE);
                        }
                        else if (lobbies.isEmpty())
                        {
                            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[3], PANEL_CENTER_X, PANEL_CENTER_Y, Color.WHITE);
                        }
                        else {
                            renderLobbySelect(sb);
                        }
                        break;
                    case 1: //Joining
                        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[8], PANEL_CENTER_X, PANEL_CENTER_Y, Color.WHITE);
                        break;
                    case 2: //Password
                        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[15], PANEL_CENTER_X, PANEL_CENTER_Y, Color.WHITE);

                        FontHelper.renderFontLeftTopAligned(sb, FontHelper.buttonLabelFont, TEXT[9], NAME_LABEL_X, PASSWORD_LABEL_Y, Color.WHITE);
                        passwordInput.render(sb);

                        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[25], PANEL_CENTER_X, CREATE_TEXT_Y, lobbyCreateHitbox.hovered ? Color.GOLD : Color.WHITE);
                        break;
                    case 3: //Creation menu
                        renderCreationUI(sb);
                        break;
                    case 4: //Creating
                        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[22], PANEL_CENTER_X, PANEL_CENTER_Y, Color.WHITE);
                        break;
                }
            }
            else {
                FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[26], PANEL_CENTER_X, PANEL_CENTER_Y, Color.WHITE);
            }
        }
    }

    private void renderTopButtons(SpriteBatch sb) {
        sb.setColor(refreshButtonColor);
        sb.draw(refreshButton, REFRESH_X, BUTTON_Y, BUTTON_OFFSET, BUTTON_OFFSET, BUTTON_SIZE, BUTTON_SIZE, refreshButtonScale * Settings.scale, refreshButtonScale * Settings.scale, 0, 0, 0, BUTTON_SIZE, BUTTON_SIZE, false, false);

        sb.setColor(createButtonColor);
        sb.draw(createButton, CREATE_X, BUTTON_Y, BUTTON_OFFSET, BUTTON_OFFSET, BUTTON_SIZE, BUTTON_SIZE, createButtonScale * Settings.scale, createButtonScale * Settings.scale, 0, 0, 0, BUTTON_SIZE, BUTTON_SIZE, false, false);

    }

    private void renderLobbySelect(SpriteBatch sb)
    {
        sb.setColor(Color.WHITE);
        if (page < maxPage)
        {
            sb.draw(arrow, ARROW_X, NEXT_ARROW_Y, ARROW_OFFSET_X, ARROW_OFFSET_Y, ARROW_WIDTH, ARROW_HEIGHT, Settings.scale * (nextArrowHitbox.hovered ? 1.1f: 1.0f), Settings.scale * (nextArrowHitbox.hovered ? 1.1f: 1.0f), 0, 0, 0, ARROW_WIDTH, ARROW_HEIGHT, false, false);
        }
        if (page > 0)
        {
            sb.draw(arrow, ARROW_X, PREV_ARROW_Y, ARROW_OFFSET_X, ARROW_OFFSET_Y, ARROW_WIDTH, ARROW_HEIGHT, Settings.scale * (prevArrowHitbox.hovered ? 1.1f: 1.0f), Settings.scale * (prevArrowHitbox.hovered ? 1.1f: 1.0f), 0, 0, 0, ARROW_WIDTH, ARROW_HEIGHT, false, true);
        }

        FontHelper.renderFontLeftTopAligned(sb, FontHelper.buttonLabelFont, TEXT[0], NAME_LABEL_X, OTHER_RENDER_LABEL_Y, Color.GOLD);
        FontHelper.renderFontCenteredTopAligned(sb, FontHelper.buttonLabelFont, TEXT[1], ISPUBLIC_LABEL_X, LABEL_Y, Color.GOLD);
        sb.draw(ImageMaster.TP_ASCENSION, ASCENSION_SYMBOL_X - ICON_W / 2f, LABEL_Y - ICON_W / 2f, ICON_W, ICON_W);
        FontHelper.renderFontCenteredTopAligned(sb, FontHelper.buttonLabelFont, TEXT[2], HASSAMEMODS_LABEL_X, LABEL_Y, Color.GOLD);

        int max = Math.min(lobbies.size(), (page * LOBBIES_PER_PAGE + LOBBIES_PER_PAGE));
        float y = LOBBY_START_Y;
        Color textColor;
        for (int i = page * LOBBIES_PER_PAGE; i < max; ++i)
        {
            if (hoveredIndex == i && lobbies.get(i).isValid)
            {
                textColor = Color.GOLD;
            }
            else
            {
                textColor = lobbies.get(i).isFriend ? Color.GREEN : Color.WHITE;
            }
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.buttonLabelFont, lobbies.get(i).name, NAME_LABEL_X, y + OTHER_RENDER_OFFSET, textColor);
            if (lobbies.get(i).isValid)
            {
                FontHelper.renderFontCenteredTopAligned(sb, FontHelper.buttonLabelFont, String.valueOf(lobbies.get(i).ascension), ASCENSION_SYMBOL_X, y, textColor);
                FontHelper.renderFontCenteredTopAligned(sb, FontHelper.buttonLabelFont, lobbies.get(i).isPublic ? TEXT[4] : TEXT[5], ISPUBLIC_LABEL_X, y, textColor);
                FontHelper.renderFontCenteredTopAligned(sb, FontHelper.buttonLabelFont, lobbies.get(i).sameMods ? TEXT[4] : TEXT[5], HASSAMEMODS_LABEL_X, y, textColor);
            }
            y -= LINE_HEIGHT;
        }
    }

    private void renderCreationUI(SpriteBatch sb)
    {
        //name
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.buttonLabelFont, TEXT[0], NAME_LABEL_X, LABEL_Y, Color.WHITE);
        nameInput.render(sb);

        //public
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.buttonLabelFont, TEXT[11], TOGGLE_LABEL_X, PUBLIC_INPUT_TEXT_Y, Color.WHITE);
        publicRoom.render(sb);

        if (!publicRoom.enabled)
        {
            //password
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.buttonLabelFont, TEXT[9], NAME_LABEL_X, PASSWORD_LABEL_Y, Color.WHITE);
            passwordInput.render(sb);
        }

        FontHelper.renderFontLeftTopAligned(sb, FontHelper.buttonLabelFont, TEXT[12], TOGGLE_LABEL_X, SAME_MODS_REQUIRED_LABEL_Y, Color.WHITE);
        sameModsToggle.render(sb);

        //ascension
        renderAscensionOptions(sb);

        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[13], PANEL_CENTER_X, CREATE_TEXT_Y, lobbyCreateHitbox.hovered ? Color.GOLD : Color.WHITE);
    }

    private void renderAscensionOptions(SpriteBatch sb)
    {
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.OPTION_TOGGLE, PANEL_CENTER_X - ASC_LEFT_W - 16.0F - 40.0F * Settings.scale, PANEL_Y + 200.0F * Settings.scale - 16.0F, 16.0F, 16.0F, 32.0F, 32.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 32, 32, false, false);// 533
        if (this.ascensionModeHb.hovered) {
            FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, TEXT[17], PANEL_CENTER_X - ASC_LEFT_W / 2.0F - 10 * Settings.scale, PANEL_Y + 200.0F * Settings.scale, Settings.GREEN_TEXT_COLOR);
            TipHelper.renderGenericTip((float)InputHelper.mX - 140.0F * Settings.scale, (float)InputHelper.mY + 100.0F * Settings.scale, TEXT[19], TEXT[20]);
        }
        else {
            FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, TEXT[17], PANEL_CENTER_X - ASC_LEFT_W / 2.0F - 10 * Settings.scale, PANEL_Y + 200.0F * Settings.scale, Settings.GOLD_COLOR);
        }

        if (CoopMenu.screen.isAscensionMode) {
            FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, TEXT[18] + CoopMenu.screen.ascensionLevel, PANEL_CENTER_X + ASC_RIGHT_W / 2.0F + 110.0F * Settings.scale, PANEL_Y + 200.0F * Settings.scale, Settings.BLUE_TEXT_COLOR);
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.OPTION_TOGGLE_ON, PANEL_CENTER_X - ASC_LEFT_W - 16.0F - 40.0F * Settings.scale, PANEL_Y + 200.0F * Settings.scale - 16.0F, 16.0F, 16.0F, 32.0F, 32.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 32, 32, false, false);


            if (!this.ascLeftHb.hovered && !Settings.isControllerMode) {
                sb.setColor(Color.LIGHT_GRAY);
            }
            else {
                sb.setColor(Color.WHITE);
            }

            sb.draw(ImageMaster.CF_LEFT_ARROW, this.ascLeftHb.cX - 24.0F, this.ascLeftHb.cY - 24.0F, 24.0F, 24.0F, 48.0F, 48.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 48, 48, false, false);
            if (!this.ascRightHb.hovered && !Settings.isControllerMode) {
                sb.setColor(Color.LIGHT_GRAY);
            }
            else {
                sb.setColor(Color.WHITE);
            }

            sb.draw(ImageMaster.CF_RIGHT_ARROW, this.ascRightHb.cX - 24.0F, this.ascRightHb.cY - 24.0F, 24.0F, 24.0F, 48.0F, 48.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 48, 48, false, false);
            if (Settings.isControllerMode) {
                sb.draw(CInputActionSet.proceed.getKeyImg(), this.ascensionModeHb.cX - 100.0F * Settings.scale - 32.0F, this.ascensionModeHb.cY - 32.0F, 32.0F, 32.0F, 64.0F, 64.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 64, 64, false, false);
                sb.draw(CInputActionSet.pageLeftViewDeck.getKeyImg(), this.ascLeftHb.cX - 60.0F * Settings.scale - 32.0F, this.ascLeftHb.cY - 32.0F, 32.0F, 32.0F, 64.0F, 64.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 64, 64, false, false);
                sb.draw(CInputActionSet.pageRightViewExhaust.getKeyImg(), this.ascRightHb.cX + 60.0F * Settings.scale - 32.0F, this.ascRightHb.cY - 32.0F, 32.0F, 32.0F, 64.0F, 64.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 64, 64, false, false);
            }
        }

        this.ascensionModeHb.render(sb);
        this.ascLeftHb.render(sb);
        this.ascRightHb.render(sb);
    }
}
