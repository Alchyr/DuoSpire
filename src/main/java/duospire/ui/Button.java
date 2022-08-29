package duospire.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputAction;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

import java.util.function.Supplier;

public class Button {
    private static final float HITBOX_W = 260.0f * Settings.scale;
    private static final float HITBOX_H = 80.0f * Settings.scale;

    public Hitbox hb;
    private String text;
    private float textScale;
    private float controllerImgTextWidth = 0;

    private CInputAction controllerInput;
    private Supplier<Boolean> altInput = ()->false;

    private boolean hidden;
    private boolean enabled;
    private final Color textColor;
    private final Color btnColor;
    private final Color disabledColor;

    public Button(float cx, float cy, String text) {
        this.hb = new Hitbox(0.0F, 0.0F, HITBOX_W, HITBOX_H);
        hb.move(cx, cy);
        hidden = true;
        enabled = true;
        this.textColor = Color.WHITE.cpy();
        this.btnColor = Color.WHITE.cpy();
        this.disabledColor = Color.GRAY.cpy();

        setText(text);
    }
    public Button setCInput(CInputAction input) {
        this.controllerInput = input;
        return this;
    }
    public Button setAltInput(Supplier<Boolean> altInput) {
        this.altInput = altInput;
        return this;
    }
    public void setText(String text) {
        this.text = text;
        this.controllerImgTextWidth = 0;

        if (FontHelper.getSmartWidth(FontHelper.buttonLabelFont, text, 9999.0F, 0.0F) > 200.0F * Settings.scale) {
            textScale = 0.8f;
        }
        else {
            textScale = 1.0f;
        }
    }
    public void enable() {
        enabled = true;
    }
    public void disable() {
        enabled = false;
    }

    public boolean update() {
        if (!this.hidden) {
            this.textColor.a = MathHelper.fadeLerpSnap(this.textColor.a, 1.0F);
            this.btnColor.a = this.textColor.a;

            if (enabled) {
                this.hb.update();
                if (this.hb.justHovered) {
                    CardCrawlGame.sound.play("UI_HOVER");
                }

                if (this.hb.hovered && InputHelper.justClickedLeft) {
                    this.hb.clickStarted = true;
                    CardCrawlGame.sound.play("UI_CLICK_1");
                }

                if (this.hb.clicked || altInput.get()) {
                    this.hb.clicked = false;
                    return true;
                }
                else if (controllerInput != null && controllerInput.isJustPressed()) {
                    controllerInput.unpress();
                    return true;
                }
            }
        }
        return false;
    }

    public void hideInstantly() {
        this.hidden = true;
        this.textColor.a = 0.0F;
        this.btnColor.a = 0.0F;
    }

    public void hide() {
        this.hidden = true;
    }

    public void show() {
        this.hidden = false;
        this.textColor.a = 0.0F;
        this.btnColor.a = 0.0F;
    }

    public void render(SpriteBatch sb) {
        if (!this.hidden) {
            this.renderButton(sb);
            disabledColor.a = textColor.a;
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, text, this.hb.cX, this.hb.cY, enabled ? textColor : disabledColor, textScale);
        }
    }

    private void renderButton(SpriteBatch sb) {
        disabledColor.a = btnColor.a;
        sb.setColor(enabled ? btnColor : disabledColor);
        sb.draw(ImageMaster.REWARD_SCREEN_TAKE_BUTTON, this.hb.cX - 256.0F, this.hb.cY - 128.0F, 256.0F, 128.0F, 512.0F, 256.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 512, 256, false, false);
        if (this.hb.hovered && !this.hb.clickStarted) {
            sb.setBlendFunction(770, 1);
            sb.setColor(new Color(1.0F, 1.0F, 1.0F, 0.3F));
            sb.draw(ImageMaster.REWARD_SCREEN_TAKE_BUTTON, this.hb.cX - 256.0F, this.hb.cY - 128.0F, 256.0F, 128.0F, 512.0F, 256.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 512, 256, false, false);
            sb.setBlendFunction(770, 771);
        }

        if (Settings.isControllerMode && controllerInput != null) {
            if (this.controllerImgTextWidth == 0.0F) {
                this.controllerImgTextWidth = FontHelper.getSmartWidth(FontHelper.buttonLabelFont, text, 99999.0F, 0.0F) / 2.0F;
            }

            disabledColor.a = textColor.a;
            sb.setColor(enabled ? textColor : disabledColor);
            sb.draw(controllerInput.getKeyImg(), this.hb.cX - 32.0F - this.controllerImgTextWidth - 38.0F * Settings.scale, this.hb.cY - 32.0F, 32.0F, 32.0F, 64.0F, 64.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 64, 64, false, false);
        }

        this.hb.render(sb);
    }
}
