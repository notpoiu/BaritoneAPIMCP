package baritone.api.utils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;

public class BaritoneToast extends Gui {

    private static BaritoneToast currentToast;
    private String title;
    private String subtitle;
    private long firstDrawTime;
    private boolean newDisplay;
    private long totalShowTime;

    public BaritoneToast(ChatComponentText titleComponent, ChatComponentText subtitleComponent, long totalShowTime) {
        this.title = titleComponent.getUnformattedText();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getUnformattedText();
        this.totalShowTime = totalShowTime;
    }

    public void draw(Gui toastGui, long delta) {
        if (this.newDisplay) {
            this.firstDrawTime = delta;
            this.newDisplay = false;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("textures/gui/toasts.png"));
        GlStateManager.color(1.0F, 1.0F, 1.0F, 255.0f);
        toastGui.drawTexturedModalRect(0, 0, 0, 32, 160, 32);

        if (this.subtitle == null) {
            Minecraft.getMinecraft().fontRendererObj.drawString(this.title, 18, 12, -11534256);
        } else {
            Minecraft.getMinecraft().fontRendererObj.drawString(this.title, 18, 7, -11534256);
            Minecraft.getMinecraft().fontRendererObj.drawString(this.subtitle, 18, 18, -16777216);
        }
    }

    public void setDisplayedText(ChatComponentText titleComponent, ChatComponentText subtitleComponent) {
        this.title = titleComponent.getUnformattedText();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getUnformattedText();
        this.newDisplay = true;
    }

    public static void addOrUpdate(ChatComponentText title, ChatComponentText subtitle, long totalShowTime) {
        if (currentToast == null) {
            currentToast = new BaritoneToast(title, subtitle, totalShowTime);
        } else {
            currentToast.setDisplayedText(title, subtitle);
        }
    }

    public static void drawCurrentToast(Gui toastGui, long delta) {
        if (currentToast != null) {
            currentToast.draw(toastGui, delta);
        }
    }
}