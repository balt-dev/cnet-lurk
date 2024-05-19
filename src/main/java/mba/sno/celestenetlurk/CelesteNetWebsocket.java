package mba.sno.celestenetlurk;

import java.net.URI;
import java.time.ZoneId;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.JsonSyntaxException;
import net.minecraft.text.TextColor;
import com.google.gson.Gson;

public class CelesteNetWebsocket extends WebSocketClient {

    static Gson gson = new Gson();
    MessageInterface onMessage;

    enum State {
        Waiting,
        CommandID,
        Skip,
        Payload
    }

    State state;
    Instant connectionStart;

    public CelesteNetWebsocket(MessageInterface messageInterface) {
        super(URI.create("wss://celestenet.0x0a.de/api/ws/"));
        this.connectionStart = Instant.now();
        this.onMessage = messageInterface;
        this.state = State.Waiting;
        this.connect();
    }

    @Override
    public void onOpen(ServerHandshake data) {
        CelesteNetLurk.LOGGER.info("Connected to CelesteNet in {}", Duration.between(Instant.now(), this.connectionStart).toString());
    }

    @Override
    public void onMessage(String message) {
        // Messages are sent in a triple of
        // - cmd
        // - <Command ID>
        // - <Payload>
        // We only want to pay attention to chat messages.
        switch(this.state) {
            case Waiting:
                if (!message.equals("cmd")) {
                    CelesteNetLurk.LOGGER.warn("Found a non-cmd message in Waiting mode! The webhook might be desynced.");
                } else {
                    this.state = State.CommandID;
                }
                break;
            case CommandID:
                if (message.equals("chat")) {
                    this.state = State.Payload;
                } else {
                    this.state = State.Skip;
                }
                break;
            case Payload:
                try {
                    CelesteNetMessage obj = gson.fromJson(message, CelesteNetMessage.class);
                    TextColor color = TextColor.parse(obj.Color);
                    long id = obj.ID;
                    String name;
                    if (id == 0xFFFFFFFFL) {
                        // This is a message from the internal server
                        name = "**SERVER**";
                    } else {
                        name = obj.Name;
                    }
                    String text = obj.Text;
                    LocalDateTime timestamp = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli((long) obj.DateTime), ZoneId.systemDefault()
                    );
                    this.onMessage.onMessage(color, name, text, timestamp);
                } catch (JsonSyntaxException err) {
                    CelesteNetLurk.LOGGER.warn("Failed to parse JSON of message! The webhook might be desynced.");
                }
                // Payload falls through here to reset state
            case Skip:
                this.state = State.Waiting;
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // The close codes are documented in class org.java_websocket.framing.CloseFrame
        if (remote) {
            CelesteNetLurk.LOGGER.error("Server disconnect from CelesteNet with code {} and reason {}", code, reason);
        } else {
            CelesteNetLurk.LOGGER.info("Client disconnect from CelesteNet with code {} and reason {}", code, reason);
        }
    }

    @Override
    public void onError(Exception e) {
        CelesteNetLurk.LOGGER.error("Encountered error in CelesteNet connection", e);
    }

    public interface MessageInterface {
        void onMessage(TextColor color, String name, String text, LocalDateTime timestamp);
    }
}

/*
    Sample message
    {
      "ID": 3711057335,
      "PlayerID": 17988,
      "Name": "Rainn",
      "Targets": null,
      "Color": "#FFFFFF",
      "DateTime": 1705401863492.186,
      "Tag": "",
      "Text": "qt do dashless+"
    }
*/

class CelesteNetMessage {
    protected String Color;
    protected String Name;
    protected String Text;
    protected long ID;
    protected double DateTime;
}