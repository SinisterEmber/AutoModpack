package pl.skidam.automodpack.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.skidam.automodpack.AutoModpackMain;
import pl.skidam.automodpack.client.AutoModpackToast;
import pl.skidam.automodpack.client.StartAndCheck;
import pl.skidam.automodpack.client.modpack.CheckModpack;
import pl.skidam.automodpack.client.ui.ConfirmScreen;
import pl.skidam.automodpack.client.ui.MenuScreen;
import pl.skidam.automodpack.config.Config;

import static pl.skidam.automodpack.AutoModpackMain.*;
import static pl.skidam.automodpack.client.StartAndCheck.isChecking;

@Mixin(TitleScreen.class)
public class UpdateButtonMixin extends Screen {

    private static final Identifier ICON_TEXTURE = new Identifier(AutoModpackMain.MOD_ID, "button.png");

    public UpdateButtonMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "initWidgetsNormal" )
    private void AutoModpackUpdateButton(int y, int spacingY, CallbackInfo ci) {
        int Y_BUTTON = 24;
        if (AutoModpackMain.isModMenu) {
            Y_BUTTON = 0;
        }

        this.addDrawableChild(
            new TexturedButtonWidget(this.width / 2 + 104, y + Y_BUTTON, 20, 20, 0, 0, 20, ICON_TEXTURE, 32, 64,
                button -> this.client.setScreen(new MenuScreen()),
                new TranslatableText("gui.automodpack.button.menu")
            )
        );
    }
}