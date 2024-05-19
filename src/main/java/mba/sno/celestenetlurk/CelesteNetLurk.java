package mba.sno.celestenetlurk;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CelesteNetLurk implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("CelesteNetLurk");
    public static CelesteNetWebsocket websocket;

    @Override
    public void onInitializeClient() {
        websocket = new CelesteNetWebsocket((color, name, text, timestamp) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            String formattedTimestamp = timestamp.format(DateTimeFormatter.ISO_TIME);
            MutableText message = Text.translatable("chat.celeste", formattedTimestamp, name, text).copy();
            Style style = Style.EMPTY.withColor(color);
            message.setStyle(style);
            client.inGameHud.getChatHud().addMessage(message);
        });
    }
}
