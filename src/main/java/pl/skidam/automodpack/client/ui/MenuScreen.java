package pl.skidam.automodpack.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import pl.skidam.automodpack.AutoModpackClient;
import pl.skidam.automodpack.AutoModpackMain;
import pl.skidam.automodpack.client.AutoModpackToast;
import pl.skidam.automodpack.client.StartAndCheck;
import pl.skidam.automodpack.client.modpack.CheckModpack;

import static pl.skidam.automodpack.client.StartAndCheck.isChecking;

@Environment(EnvType.CLIENT)
public class MenuScreen extends Screen {
    private Screen parent;
    private MenuScreen.ModpackSelectionListWidget modpackSelectionList;
    final ModpackManager modpackManager;

    public MenuScreen() {
        super(new TranslatableText("gui.automodpack.screen.menu.title").formatted(Formatting.BOLD));
        this.modpackManager = modpackManager;
        assert client != null;
    }
    @Override
    protected void init() {
        this.modpackSelectionList = new MenuScreen.ModpackSelectionListWidget(this.client);
        this.addSelectableChild(this.modpackSelectionList);

        super.init();
        assert this.client != null;

        // get height and with of the screen
        int width = this.width;
        int height = this.height;
        AutoModpackMain.LOGGER.warn("width: " + width + ", height: " + height);

        this.addDrawableChild(new ButtonWidget(this.width / 2 - 210, this.height / 6 + 165, 115, 20, new TranslatableText("gui.automodpack.button.update"), (button) -> {
            AutoModpackToast.add(0);
            if (!isChecking) {
                CheckModpack.isCheckUpdatesButtonClicked = true;
                new StartAndCheck(false, false);
            }
        }));

        //if (out.exists()) { // out == modpackdir
            this.addDrawableChild(new ButtonWidget(this.width / 2 - 90, this.height / 6 + 165, 115, 20, new TranslatableText("gui.automodpack.button.delete"), (button) -> {
                this.client.setScreen(new ConfirmScreen());
            }));
        //}

        // make back to the main menu button
        this.addDrawableChild(new ButtonWidget(this.width / 2 + 100, this.height / 6 + 165, 115, 20, new TranslatableText("gui.automodpack.button.back"), (button) -> {
            this.client.setScreen(parent);
        }));
    }

    private String getSelectedServerIP() {
        return AutoModpackClient.serverIP;
    }

    private String getSelectedModpack() {
        return AutoModpackMain.out.getName();
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.modpackSelectionList.render(matrices, mouseX, mouseY, delta);
        String selectedServerIP = this.getSelectedServerIP();
        String selectedModpack = this.getSelectedModpack();
        drawCenteredText(matrices, this.textRenderer, selectedServerIP, this.width / 2, 60, 16777215);
        drawCenteredText(matrices, this.textRenderer, selectedModpack, this.width / 2, 70, 16777215);

        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 15, 16777215);
        drawCenteredText(matrices, this.textRenderer, new TranslatableText("gui.automodpack.screen.menu.description"), this.width / 2, 30, 16777215);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Environment(EnvType.CLIENT)
    class ModpackSelectionListWidget extends AlwaysSelectedEntryListWidget<MenuScreen.ModpackSelectionListWidget.ModpackEntry> {
        public ModpackSelectionListWidget(MinecraftClient client) {
            super(client, MenuScreen.this.width, MenuScreen.this.height, 32, MenuScreen.this.height - 65 + 4, 18);

            for(LanguageDefinition languageDefinition : MenuScreen.this.languageManager.getAllLanguages()) {
                MenuScreen.ModpackSelectionListWidget.LanguageEntry languageEntry = new MenuScreen.ModpackSelectionListWidget.LanguageEntry(
                        languageDefinition
                );
                this.addEntry(languageEntry);
                if (LanguageOptionsScreen.this.languageManager.getLanguage().getCode().equals(languageDefinition.getCode())) {
                    this.setSelected(languageEntry);
                }
            }

            if (this.getSelectedOrNull() != null) {
                this.centerScrollOn((LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry)this.getSelectedOrNull());
            }

        }

        protected int getScrollbarPositionX() {
            return super.getScrollbarPositionX() + 20;
        }

        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        protected void renderBackground(MatrixStack matrices) {
            LanguageOptionsScreen.this.renderBackground(matrices);
        }

        protected boolean isFocused() {
            return LanguageOptionsScreen.this.getFocused() == this;
        }

        @Environment(EnvType.CLIENT)
        public class ModpackEntry extends Entry<MenuScreen.ModpackSelectionListWidget.ModpackEntry> {
            final LanguageDefinition languageDefinition;

            public LanguageEntry(LanguageDefinition languageDefinition) {
                this.languageDefinition = languageDefinition;
            }

            public void render(
                    MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
            ) {
                String string = this.languageDefinition.toString();
                LanguageOptionsScreen.this.textRenderer
                        .drawWithShadow(
                                matrices,
                                string,
                                (float)(LanguageOptionsScreen.LanguageSelectionListWidget.this.width / 2 - LanguageOptionsScreen.this.textRenderer.getWidth(string) / 2),
                                (float)(y + 1),
                                16777215,
                                true
                        );
            }

            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    this.onPressed();
                    return true;
                } else {
                    return false;
                }
            }

            private void onPressed() {
                LanguageOptionsScreen.LanguageSelectionListWidget.this.setSelected(this);
            }

            public Text getNarration() {
                return new TranslatableText("narrator.select", new Object[]{this.languageDefinition});
            }
        }
    }
}