package duospire.ui;

import basemod.BaseMod;
import basemod.TopPanelItem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import duospire.networking.gameplay.P2P;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static duospire.DuoSpire.makeID;

public class PingDisplay extends TopPanelItem {
    private static final float centerOffset = 64f / 2 * Settings.scale;
    private static final NumberFormat pingFormat = NumberFormat.getIntegerInstance();
    private static final Color BAD_PING_COLOR = Color.RED.cpy();
    private static final Color OK_PING_COLOR = Color.YELLOW.cpy();
    private static final Color GOOD_PING_COLOR = Color.GREEN.cpy();
    private static final double GOOD_PING_THRESHOLD = 200, OK_PING_THRESHOLD = 500;

    public PingDisplay() {
        super(null, makeID("PingDisplay"));
        setClickable(false);
    }

    @Override
    public void render(SpriteBatch sb, Color color) {
        //x/y are bottom left corner. Size is 64x64 * Settings.scale.
        if (P2P.currentPartner != null && P2P.currentPartner.isValid()) {
            double ping = P2P.getMsPing();
            if (ping < GOOD_PING_THRESHOLD) {
                FontHelper.renderFontCentered(sb, FontHelper.tipBodyFont, pingFormat.format(ping), x + centerOffset, y + centerOffset, GOOD_PING_COLOR);
            }
            else if (ping < OK_PING_THRESHOLD) {
                FontHelper.renderFontCentered(sb, FontHelper.tipBodyFont, pingFormat.format(ping), x + centerOffset, y + centerOffset, OK_PING_COLOR);
            }
            else {
                FontHelper.renderFontCentered(sb, FontHelper.tipBodyFont, pingFormat.format(ping), x + centerOffset, y + centerOffset, BAD_PING_COLOR);
            }
        }
        else {
            BaseMod.removeTopPanelItem(this);
        }
    }

    @Override
    protected void onClick() {

    }

    @Override
    protected void onHover() {

    }

    @Override
    protected void onUnhover() {

    }
}
