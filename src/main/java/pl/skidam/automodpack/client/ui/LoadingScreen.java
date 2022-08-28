package pl.skidam.automodpack.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import pl.skidam.automodpack.client.modpack.CheckModpack;
import pl.skidam.automodpack.client.modpack.DownloadModpack;
import pl.skidam.automodpack.utils.Download;
import pl.skidam.automodpack.utils.UnZipper;

@Environment(EnvType.CLIENT)
public class LoadingScreen extends Screen {

    public LoadingScreen() {
        super(new TranslatableText("gui.automodpack.screen.loading.title").formatted(Formatting.BOLD));
        // It needs to be here to restart unzip progress value
        UnZipper.progress = 0;
    }

    private TranslatableText getStage() {
        TranslatableText stage = new TranslatableText("");
        if (CheckModpack.update) {
            if (getStep().equals(new TranslatableText("gui.automodpack.screen.loading.download"))) {
                if (DownloadModpack.maxInputs == 0) {
                    stage = new TranslatableText("gui.automodpack.screen.loading.wait");
                } else if (DownloadModpack.minInputs <= DownloadModpack.maxInputs) {
                    stage = new TranslatableText(DownloadModpack.minInputs + "/" + DownloadModpack.maxInputs);
                }
            }
        }
        return stage;
    };

    private TranslatableText getPercentage() {
        TranslatableText percentage = new TranslatableText(Download.progress + "%");
        if (Download.progress == 100) {
            percentage = new TranslatableText(UnZipper.progress + "%");
            if (!CheckModpack.update) {
                if (UnZipper.progress == 100) {
                    percentage = new TranslatableText("gui.automodpack.screen.loading.wait");
                }
            }
        }
        return percentage;
    }

    private TranslatableText getStep() {
        TranslatableText step;
        if (CheckModpack.update) {
            step = new TranslatableText("gui.automodpack.screen.loading.download"); // Downloading...
            if (DownloadModpack.minInputs > DownloadModpack.maxInputs) {
                step = new TranslatableText("gui.automodpack.screen.loading.finish"); // Finishing...
                if (Download.progress == 100) {
                    step = new TranslatableText("gui.automodpack.screen.loading.extract"); // Extracting modpack...
                }
                if (UnZipper.progress == 100) {
                    step = new TranslatableText("gui.automodpack.screen.loading.finish"); // Finishing...
                }
            }
        } else {
            step = new TranslatableText("gui.automodpack.screen.loading.download"); // Downloading...
            if (Download.progress == 100) {
                step = new TranslatableText("gui.automodpack.screen.loading.extract"); // Extracting modpack...
            }
            if (UnZipper.progress == 100) {
                step = new TranslatableText("gui.automodpack.screen.loading.finish"); // Finishing...
            }
        }
        return step;
    }

    private String getInternetConnectionSpeed() {
        if (Download.progress > 0 && Download.progress < 100) {
            return Download.averageInternetConnectionSpeed;
        } else {
            return "";
        }
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        TranslatableText percentage = this.getPercentage();
        TranslatableText step = this.getStep();
        TranslatableText stage = this.getStage();
        String internetConnectionSpeed = this.getInternetConnectionSpeed();
        drawCenteredText(matrices, this.textRenderer, step, this.width / 2, 80, 16777215);
        drawCenteredText(matrices, this.textRenderer, stage, this.width / 2, 95, 16777215);
        drawCenteredText(matrices, this.textRenderer, percentage, this.width / 2, 110, 16777215);
        drawCenteredText(matrices, this.textRenderer, internetConnectionSpeed, this.width / 2, 125, 16777215);
        super.render(matrices, mouseX, mouseY, delta);
    }

    public boolean shouldCloseOnEsc() { return false; }

}
