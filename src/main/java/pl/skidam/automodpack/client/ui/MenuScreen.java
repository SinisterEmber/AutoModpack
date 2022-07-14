package pl.skidam.automodpack.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import pl.skidam.automodpack.AutoModpackMain;
import pl.skidam.automodpack.client.AutoModpackToast;
import pl.skidam.automodpack.client.StartAndCheck;
import pl.skidam.automodpack.client.modpack.CheckModpack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

import static pl.skidam.automodpack.AutoModpackClient.selected_modpack;
import static pl.skidam.automodpack.AutoModpackClient.serverIP;
import static pl.skidam.automodpack.AutoModpackMain.LOGGER;
import static pl.skidam.automodpack.AutoModpackMain.out;
import static pl.skidam.automodpack.client.StartAndCheck.isChecking;

@Environment(EnvType.CLIENT)
public class MenuScreen extends Screen {
    private MenuScreen.ModpackSelectionListWidget modpackSelectionList;
    public String modpack;

    public MenuScreen() {
        super(new LiteralText("Auto").formatted(Formatting.GOLD).append(new LiteralText("Modpack").formatted(Formatting.WHITE).append(new LiteralText(" Menu").formatted(Formatting.GRAY)).formatted(Formatting.BOLD)));
        assert client != null;
    }
    @Override
    protected void init() {
        // load saved selected modpack from ./AutoModpack/selected-modpack.txt file
        if (selected_modpack.exists()) {
            try {
                FileReader fr = new FileReader(selected_modpack);
                Scanner inFile = new Scanner(fr);
                if (inFile.hasNextLine()) {
                    out = new File(inFile.nextLine());
                    ModpackSelectionListWidget.ModpackEntry modpackEntry = this.modpackSelectionList.getSelectedOrNull();
                    assert modpackEntry != null;
                    this.modpack = modpackEntry.modpackDefinition;
                }
                inFile.close();
            } catch (Exception e) { // ignore
            }
        }

        this.modpackSelectionList = new ModpackSelectionListWidget(this.client);
        this.addSelectableChild(this.modpackSelectionList);


        super.init();
        assert this.client != null;

//        // get height and with of the screen
//        int width = this.width;
//        int height = this.height;
//        AutoModpackMain.LOGGER.warn("width: " + width + ", height: " + height);

        this.addDrawableChild(new ButtonWidget(this.width / 2 - 210, this.height - 38, 115, 20, new TranslatableText("gui.automodpack.button.update"), (button) -> {
            AutoModpackToast.add(0);
            if (!isChecking) {
                CheckModpack.isCheckUpdatesButtonClicked = true;
                new StartAndCheck(false, false);
            }
        }));

        //if (out.exists()) { // out == modpackdir
            this.addDrawableChild(new ButtonWidget(this.width / 2 - 90, this.height - 38, 115, 20, new TranslatableText("gui.automodpack.button.delete"), (button) -> {
                this.client.setScreen(new ConfirmScreen());
            }));
        //}

        // make back to the main menu button
        this.addDrawableChild(new ButtonWidget(this.width / 2 + 100, this.height - 38, 115, 20, new TranslatableText("gui.automodpack.button.back"), (button) -> {
            if (this.modpackSelectionList.getSelectedOrNull() != null) {
                out = new File(this.modpackSelectionList.getSelectedOrNull().modpackDefinition);

                if (!selected_modpack.exists()) {
                    try {
                        selected_modpack.createNewFile();
                    } catch (IOException e) {
                        LOGGER.error("Couldn't create ./AutoModpack/modpacks/selected-modpack.txt file");
                    }
                }

                try {
                    FileWriter fw = new FileWriter(selected_modpack);
                    fw.write(String.valueOf(out));
                    fw.close();
                } catch (IOException e) {
                    LOGGER.error("Couldn't save serverIP to ./AutoModpack/modpacks/selected-modpack.txt file");
                }

            }

            this.client.setScreen(new TitleScreen());


        }));
    }

    private String getSelectedServerIP() {
        return serverIP;
    }

    private String getSelectedModpack() {
        return AutoModpackMain.out.getName();
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.modpackSelectionList.render(matrices, mouseX, mouseY, delta);
        String selectedServerIP = this.getSelectedServerIP();
        String selectedModpack = this.getSelectedModpack();
        //drawCenteredText(matrices, this.textRenderer, selectedServerIP, this.width / 2, 60, 16777215);
        //drawCenteredText(matrices, this.textRenderer, selectedModpack, this.width / 2, 70, 16777215);

        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 15, 16777215);
        //drawCenteredText(matrices, this.textRenderer, new TranslatableText("gui.automodpack.screen.menu.description"), this.width / 2, 20, 16777215);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Environment(EnvType.CLIENT)
    class ModpackSelectionListWidget extends AlwaysSelectedEntryListWidget<MenuScreen.ModpackSelectionListWidget.ModpackEntry> {
        public ModpackSelectionListWidget(MinecraftClient client) {
            super(client, MenuScreen.this.width, MenuScreen.this.height, 32, MenuScreen.this.height - 65 + 4, 18);

            // get all modpacks from modpacksDir folder

            for (File modpack : Objects.requireNonNull(AutoModpackMain.modpacksDir.listFiles())) {
                if (modpack.getName().endsWith(".zip")) {
                    this.addEntry(new ModpackSelectionListWidget.ModpackEntry(modpack.getName()));
                    if (modpack.getName().equals(String.valueOf(out))) {
                        setSelected(new ModpackEntry(modpack.getName())); // TODO fix it, help needed pls :)
                        LOGGER.error("Found selected modpack: " + modpack.getName());
                    }
                }
            }


            if (this.getSelectedOrNull() != null) {
                this.centerScrollOn((MenuScreen.ModpackSelectionListWidget.ModpackEntry) this.getSelectedOrNull());
            }

        }

        protected int getScrollbarPositionX() {
            return super.getScrollbarPositionX() + 20;
        }

        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        protected void renderBackground(MatrixStack matrices) {
            MenuScreen.this.renderBackground(matrices);
        }

        protected boolean isFocused() {
            return MenuScreen.this.getFocused() == this;
        }

        @Environment(EnvType.CLIENT)
        public class ModpackEntry extends Entry<MenuScreen.ModpackSelectionListWidget.ModpackEntry> {
            final String modpackDefinition;

            public ModpackEntry(String modpackDefinition) {
                this.modpackDefinition = modpackDefinition;
            }

            public void render(
                MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                String string = this.modpackDefinition;
                MenuScreen.this.textRenderer
                        .drawWithShadow(
                                matrices,
                                string,
                                (float)(MenuScreen.ModpackSelectionListWidget.this.width / 2 - MenuScreen.this.textRenderer.getWidth(string) / 2),
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
                ModpackSelectionListWidget.this.setSelected(this);
                out = new File(this.modpackDefinition);

                if (!selected_modpack.exists()) {
                    try {
                        selected_modpack.createNewFile();
                    } catch (IOException e) {
                        LOGGER.error("Couldn't create ./AutoModpack/modpacks/selected-modpack.txt file");
                    }
                }

                try {
                    FileWriter fw = new FileWriter(selected_modpack);
                    fw.write(String.valueOf(out));
                    fw.close();
                } catch (IOException e) {
                    LOGGER.error("Couldn't save serverIP to ./AutoModpack/modpacks/selected-modpack.txt file");
                }
            }

            public Text getNarration() {
                return new TranslatableText("narrator.select", this.modpackDefinition);
            }
        }
    }
}