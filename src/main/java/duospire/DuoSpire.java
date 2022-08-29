package duospire;

import basemod.BaseMod;
import basemod.interfaces.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.*;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import duospire.networking.gameplay.P2P;
import duospire.patches.menu.CoopMenu;
import duospire.ui.ChatBox;
import duospire.ui.CoopMenuScreen;
import duospire.ui.PingDisplay;
import duospire.util.GeneralUtils;
import duospire.util.KeywordInfo;
import duospire.util.TextureLoader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.Patcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.*;
import duospire.networking.matchmaking.Matchmaking;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scannotation.AnnotationDB;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@SpireInitializer
public class DuoSpire implements
        EditStringsSubscriber,
        EditKeywordsSubscriber,
        EditCardsSubscriber,
        PostInitializeSubscriber,
        RenderSubscriber,
        PostUpdateSubscriber {
    public static final boolean FULL_DEBUG_LOGGING = true;

    public static ModInfo info;
    public static String modID;
    static { loadModInfo(); }
    public static final Logger logger = LogManager.getLogger(modID); //Used to output to the console.
    public static final Logger matchmakingLogger = LogManager.getLogger("DuoSpire Matchmaking");

    public static Prefs coopPrefs;

    public static final int MAX_ASCENSION = 20;

    private static final String resourcesFolder = "duospire";

    //This is used to prefix the IDs of various objects like cards and relics,
    //to avoid conflicts between different mods using the same name for things.
    public static String makeID(String id) {
        return modID + ":" + id;
    }
    public static String oncePrefix(String id) { //Added to modified cards.
        if (id == null)
            return null;
        if (id.startsWith(modID))
            return id;
        return makeID(id);
    }

    public static final Charset CHARSET = StandardCharsets.UTF_8;

    //Co-op stuff
    public static ChatBox chat;
    public static final PingDisplay pingDisplay = new PingDisplay();

    public static boolean inMultiplayer() {
        return Matchmaking.inLobby() || P2P.connected();
    }

    public static void setupDungeon() {
        if (CardCrawlGame.mode == CardCrawlGame.GameMode.CHAR_SELECT)
        {
            TipTracker.neverShowAgain("NEOW_SKIP");

            if (Settings.seed == null) {
                long sourceTime = System.nanoTime();
                Random rng = new Random(sourceTime);
                Settings.seedSourceTimestamp = sourceTime;
                Settings.seed = SeedHelper.generateUnoffensiveSeed(rng);
            } else {
                Settings.seedSet = true;
            }

            CardCrawlGame.mainMenuScreen.isFadingOut = true;
            CardCrawlGame.mainMenuScreen.fadeOutMusic();
            Settings.isDailyRun = false;
            boolean isTrialSeed = TrialHelper.isTrialSeed(SeedHelper.getString(Settings.seed));
            if (isTrialSeed) {
                Settings.specialSeed = Settings.seed;
                long sourceTime = System.nanoTime();
                Random rng = new Random(sourceTime);
                Settings.seed = SeedHelper.generateUnoffensiveSeed(rng);
                Settings.isTrial = true;
            }

            ModHelper.setModsFalse();
            AbstractDungeon.generateSeeds();
            AbstractDungeon.isAscensionMode = CoopMenu.screen.isAscensionMode;
            if (AbstractDungeon.isAscensionMode) {
                AbstractDungeon.ascensionLevel = CoopMenu.screen.ascensionLevel;
            } else {
                AbstractDungeon.ascensionLevel = 0;
            }
        }
    }

    @SuppressWarnings("unused")
    public static void initialize() {
        new DuoSpire();
    }

    public DuoSpire() {
        BaseMod.subscribe(this); //This will make BaseMod trigger all the subscribers at their appropriate times.
        logger.info(modID + " subscribed to BaseMod.");
    }

    @Override
    public void receiveRender(SpriteBatch sb) {
        if (chat != null)
            chat.render(sb);
    }

    @Override
    public void receivePostUpdate() {
        P2P.postUpdate();
    }




    /*All initialization stuff below this point.*/

    @Override
    public void receivePostInitialize() {
        coopPrefs = SaveHelper.getPrefs("DUOSPIRE");

        //This loads the image used as an icon in the in-game mods menu.
        Texture badgeTexture = TextureLoader.getTexture(resourcePath("badge.png"));
        //Set up the mod information displayed in the in-game mods menu.
        //The information used is taken from your pom.xml file.
        String[] text = CardCrawlGame.languagePack.getUIString(makeID(modID)).TEXT;
        BaseMod.registerModBadge(badgeTexture, text[0], GeneralUtils.arrToString(info.Authors), text[1], null);

        chat = new ChatBox(Settings.WIDTH / 2f, Settings.HEIGHT - 200 * Settings.scale, 7);
        chat.move(Settings.WIDTH / 2f, Settings.HEIGHT - 180 * Settings.scale - (chat.getHeight() / 2f));

        CoopMenu.buttonText = text[2];
        CoopMenu.screen = new CoopMenuScreen();

        Matchmaking.init();
        P2P.init();
    }

    @Override
    public void receiveEditCards() {
        //Generated in patching.
    }

    /*----------Localization----------*/

    //This is used to load the appropriate localization files based on language.
    private static String getLangString()
    {
        return Settings.language.name().toLowerCase();
    }
    private static final String defaultLanguage = "eng";

    @Override
    public void receiveEditStrings() {
        /*
            First, load the default localization.
            Then, if the current language is different, attempt to load localization for that language.
            This results in the default localization being used for anything that might be missing.
            The same process is used to load keywords slightly below.
        */
        loadLocalization(defaultLanguage);
        if (!defaultLanguage.equals(getLangString())) {
            loadLocalization(getLangString());
        }
    }

    private void loadLocalization(String lang) {
        //While this does load every type of localization, most of these files are just outlines so that you can see how they're formatted.
        //Feel free to comment out/delete any that you don't end up using.
        BaseMod.loadCustomStringsFile(CardStrings.class,
                localizationPath(lang, "CardStrings.json"));
        BaseMod.loadCustomStringsFile(CharacterStrings.class,
                localizationPath(lang, "CharacterStrings.json"));
        BaseMod.loadCustomStringsFile(EventStrings.class,
                localizationPath(lang, "EventStrings.json"));
        BaseMod.loadCustomStringsFile(OrbStrings.class,
                localizationPath(lang, "OrbStrings.json"));
        BaseMod.loadCustomStringsFile(PotionStrings.class,
                localizationPath(lang, "PotionStrings.json"));
        BaseMod.loadCustomStringsFile(PowerStrings.class,
                localizationPath(lang, "PowerStrings.json"));
        BaseMod.loadCustomStringsFile(RelicStrings.class,
                localizationPath(lang, "RelicStrings.json"));
        BaseMod.loadCustomStringsFile(UIStrings.class,
                localizationPath(lang, "UIStrings.json"));
    }

    @Override
    public void receiveEditKeywords()
    {
        Gson gson = new Gson();
        String json = Gdx.files.internal(localizationPath(defaultLanguage, "Keywords.json")).readString(String.valueOf(StandardCharsets.UTF_8));
        KeywordInfo[] keywords = gson.fromJson(json, KeywordInfo[].class);
        for (KeywordInfo keyword : keywords) {
            registerKeyword(keyword);
        }

        if (!defaultLanguage.equals(getLangString())) {
            try
            {
                json = Gdx.files.internal(localizationPath(getLangString(), "Keywords.json")).readString(String.valueOf(StandardCharsets.UTF_8));
                keywords = gson.fromJson(json, KeywordInfo[].class);
                for (KeywordInfo keyword : keywords) {
                    registerKeyword(keyword);
                }
            }
            catch (Exception e)
            {
                logger.warn(modID + " does not support " + getLangString() + " keywords.");
            }
        }
    }

    private void registerKeyword(KeywordInfo info) {
        BaseMod.addKeyword(modID.toLowerCase(), info.PROPER_NAME, info.NAMES, info.DESCRIPTION);
    }

    //These methods are used to generate the correct filepaths to various parts of the resources folder.
    public static String localizationPath(String lang, String file) {
        return resourcesFolder + "/localization/" + lang + "/" + file;
    }

    public static String resourcePath(String file) {
        return resourcesFolder + "/" + file;
    }
    public static String characterPath(String file) {
        return resourcesFolder + "/character/" + file;
    }
    public static String powerPath(String file) {
        return resourcesFolder + "/powers/" + file;
    }
    public static String relicPath(String file) {
        return resourcesFolder + "/relics/" + file;
    }


    //This determines the mod's ID based on information stored by ModTheSpire.
    private static void loadModInfo() {
        Optional<ModInfo> infos = Arrays.stream(Loader.MODINFOS).filter((modInfo)->{
            AnnotationDB annotationDB = Patcher.annotationDBMap.get(modInfo.jarURL);
            if (annotationDB == null)
                return false;
            Set<String> initializers = annotationDB.getAnnotationIndex().getOrDefault(SpireInitializer.class.getName(), Collections.emptySet());
            return initializers.contains(DuoSpire.class.getName());
        }).findFirst();
        if (infos.isPresent()) {
            info = infos.get();
            modID = info.ID;
        }
        else {
            throw new RuntimeException("Failed to determine mod info/ID based on initializer.");
        }
    }
}
