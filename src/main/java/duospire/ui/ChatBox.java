package duospire.ui;

import basemod.interfaces.TextReceiver;
import basemod.patches.com.megacrit.cardcrawl.helpers.input.ScrollInputProcessor.TextInput;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import duospire.DuoSpire;
import duospire.networking.gameplay.P2P;
import duospire.util.TextureLoader;
import duospire.networking.matchmaking.Matchmaking;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import static com.megacrit.cardcrawl.helpers.FontHelper.layout;
import static duospire.DuoSpire.resourcePath;

public class ChatBox implements TextReceiver {
    public Deque<String> messages = new ArrayDeque<>();

    private static final float NORMAL_FADE_TIME = 6.0f;
    private static final float MID_FADE_TIME = 3.0f;
    private static final float FAST_FADE_TIME = 1.0f;

    private static final float MARKER_WIDTH = 2;

    public static final float WIDTH = 600.0f * Settings.scale;
    private static final float MAX_TEXT_WIDTH = WIDTH - 10;

    private static final float SEPARATOR_HEIGHT = 2.0f * Settings.scale;

    private static final Texture white = TextureLoader.getTexture(resourcePath("img/ui/white.png"));

    private final Color bgColor = new Color(0.3f, 0.3f, 0.3f, 0.85f);
    private final Color separatorColor = Color.LIGHT_GRAY.cpy();
    private final Color textColor = Color.WHITE.cpy();

    private float x, y, messagesY;

    private float lineHeight;
    private float height;
    private float messagesHeight;

    private int maxLines;

    private String inputText;
    private int lastLength, visibleStart;
    private String visibleInputText;

    public boolean active;
    private boolean justEnded;
    public float fadeDelay;
    private float blipTimer;
    private float alpha;

    public ChatBox(float cx, float cy, int maxLines)
    {
        this.maxLines = maxLines;
        FontHelper.tipBodyFont.getData().setScale(1);
        lineHeight = FontHelper.tipBodyFont.getCapHeight() + 4f;
        this.height = (maxLines + 1) * lineHeight + SEPARATOR_HEIGHT;
        messagesHeight = lineHeight * maxLines;
        clearInput();
        active = false;
        justEnded = false;
        alpha = 0;
        blipTimer = 0;

        this.x = cx - WIDTH / 2;
        this.y = cy - this.height / 2;
        messagesY = y + lineHeight + SEPARATOR_HEIGHT;
    }

    public float getHeight() {
        return height;
    }

    public void move(float cx, float cy) {
        this.x = cx - WIDTH / 2;
        this.y = cy - this.height / 2;
        messagesY = y + lineHeight + SEPARATOR_HEIGHT;
    }

    private void clearInput() {
        visibleInputText = inputText = "";
        lastLength = visibleStart = 0;
    }

    @Override
    public String getCurrentText() {
        return inputText;
    }

    @Override
    public void setText(String s) {
        inputText = s;
        if (inputText.length() < lastLength)
            visibleStart -= (lastLength - inputText.length());
        if (visibleStart > inputText.length() || visibleStart < 0)
            visibleStart = 0;
        visibleInputText = inputText.substring(visibleStart);
        while (FontHelper.getWidth(FontHelper.tipBodyFont, visibleInputText, 1) > MAX_TEXT_WIDTH) {
            ++visibleStart;
            if (visibleStart >= inputText.length()) {
                visibleStart = inputText.length();
                visibleInputText = "";
                break;
            }
            visibleInputText = inputText.substring(visibleStart);
        }

        lastLength = inputText.length();
    }

    @Override
    public boolean isDone() {
        return !active;
    }

    @Override
    public int getCharLimit() {
        return 300;
    }

    public void startInput() {
        if (!active) {
            active = true;
            clearInput();
            fadeDelay = MID_FADE_TIME;
        }
        TextInput.startTextReceiver(this);
    }
    public void sendInput() {
        if (P2P.connected()) {
            P2P.sendChatMessage(inputText);
        }
        else if (Matchmaking.inLobby()) {
            Matchmaking.sendChatMessage(inputText);
        }
        fadeDelay = NORMAL_FADE_TIME;
        active = false;
        TextInput.stopTextReceiver(this);
        clearInput();
    }
    public void stopInput() {
        if (fadeDelay > FAST_FADE_TIME)
            fadeDelay = FAST_FADE_TIME;
        active = false;
        TextInput.stopTextReceiver(this);
        clearInput();
    }

    public void clicked(float tx, float ty) {
        if (active) {
            if (!(x < tx && y < ty && tx < x + WIDTH && ty < y + height)) {
                stopInput();
            }
            else if (ty <= y + lineHeight && DuoSpire.inMultiplayer()) {
                InputHelper.justClickedLeft = false;
                startInput();
            }
        }
    }

    public boolean onPushEnter()
    {
        if (active || DuoSpire.inMultiplayer())
        {
            if (!active)
            {
                startInput();
            }
            else
            {
                justEnded = true;
                if (!inputText.isEmpty())
                {
                    sendInput();
                }
                else {
                    stopInput();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean acceptCharacter(char c) {
        return FontHelper.tipBodyFont.getData().hasGlyph(c);
    }

    public void receiveMessage(String msg)
    {
        if (!msg.isEmpty())
        {
            fadeDelay = NORMAL_FADE_TIME;

            messages.push(msg);
            if (messages.size() > maxLines) //These messages might be more than 1 line, but they're definitely not less than 1 line.
            {
                messages.pollLast();
            }
        }
    }

    private final Rectangle clipBounds = new Rectangle(0, 0, 1, 1);
    public void render(SpriteBatch sb)
    {
        if (!active && !justEnded && Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && !TextInput.isTextInputActive()) {
            startInput();
        }
        justEnded = false;

        blipTimer = (blipTimer + Gdx.graphics.getRawDeltaTime()) % 0.5f;
        fadeDelay = Math.max(0, fadeDelay - Gdx.graphics.getRawDeltaTime());

        if (fadeDelay > 0 || active) {
            bgColor.a = alpha;
            separatorColor.a = alpha;
            textColor.a = alpha;
        }
        else if (DuoSpire.inMultiplayer()) {
            bgColor.a = alpha * 0.35f;
            separatorColor.a = alpha * 0.35f;
            textColor.a = alpha * 0.35f;
        }
        else {
            return;
        }

        clipBounds.set(x, y, WIDTH, height);
        sb.flush();
        if (ScissorStack.pushScissors(clipBounds)) {
            alpha = (active || fadeDelay > 1) ? 1 : fadeDelay;

            sb.setColor(bgColor);
            sb.draw(white, x, y, 0, 0, WIDTH, height, 1.0f, 1.0f, 0, 0, 0, 1, 1, false, false);

            sb.setColor(separatorColor);
            sb.draw(white, x, y + lineHeight, 0, 0, WIDTH, SEPARATOR_HEIGHT, 1.0f, 1.0f, 0, 0, 0, 1, 1, false, false);

            sb.setColor(textColor);
            if (active)
            {
                FontHelper.renderFontLeftDownAligned(sb, FontHelper.tipBodyFont, visibleInputText, x, y, Color.WHITE);
                if (blipTimer < 0.25f)
                    sb.draw(white, x + FontHelper.getWidth(FontHelper.tipBodyFont, visibleInputText, 1) + 1, y + 1, 0, 0, MARKER_WIDTH, lineHeight - 2, 1.0f, 1.0f, 0, 0, 0, 1, 1, false, false);
            }

            float height = 0;
            Iterator<String> msgIterator = messages.iterator();
            FontHelper.tipBodyFont.setColor(textColor);
            while (height < messagesHeight && msgIterator.hasNext()) {
                String msg = msgIterator.next();
                layout.setText(FontHelper.tipBodyFont, msg, textColor, MAX_TEXT_WIDTH, 1, true);
                FontHelper.tipBodyFont.draw(sb, msg, x, y, MAX_TEXT_WIDTH, 1, true);
                //FontHelper.renderFontLeftDownAligned(sb, FontHelper.tipBodyFont, msgIterator.next(), x, messagesY + height, Color.WHITE);
                height += layout.height;
            }
            sb.flush();
            ScissorStack.popScissors();
        }
    }
}
