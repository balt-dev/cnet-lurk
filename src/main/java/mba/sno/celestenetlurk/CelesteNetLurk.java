package mba.sno.celestenetlurk;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CelesteNetLurk implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("CelesteNetLurk");
    public static CelesteNetWebsocket websocket;
    private static KeyBinding toggle;
    private static MinecraftClient client;

    private void attemptSendChatMessage(Text txt) {
        InGameHud inGameHud = client.inGameHud;
        if (inGameHud == null) { return; }
        ChatHud chatHud = inGameHud.getChatHud();
        chatHud.addMessage(txt);
    }

    @Override
    public void onInitializeClient() {
        client = MinecraftClient.getInstance();

        websocket = new CelesteNetWebsocket(new CelesteNetWebsocket.MessageInterface() {
            @Override
            public void onMessage(TextColor color, String name, String text, LocalDateTime timestamp) {
                Text dateFormat = Text.translatable("chat.celeste.timestamp");
                String formattedTimestamp = timestamp.format(DateTimeFormatter.ofPattern(dateFormat.getString()));
                MutableText message = Text.translatable("chat.celeste", formattedTimestamp, name, text).copy();
                Style style = Style.EMPTY.withColor(color);
                message.setStyle(style);
                attemptSendChatMessage(message);
            }

            @Override
            public void onError(Exception e) {
                MutableText errorMessage = Text.literal("%s".formatted(e.toString())).copy();
                errorMessage.setStyle(Style.EMPTY.withColor(TextColor.parse("red")));
                attemptSendChatMessage(errorMessage);
            }
        });

        toggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.celeste.toggle", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_R, // The keycode of the key
                "category.celeste" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.END_CLIENT_TICK.register(cl -> {
            if (toggle.wasPressed()) {
                if (websocket.isClosed()) {
                    attemptSendChatMessage(Text.translatable("chat.celeste.reconnect"));
                    websocket.reconnect();
                } else {
                    attemptSendChatMessage(Text.translatable("chat.celeste.disconnect"));
                    websocket.close();
                }
            }
        });
    }
}
