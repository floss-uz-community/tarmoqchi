package uz.server.ws.tcp;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TcpChannelHolder {
    private final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();

    public void add(String channelId, Channel channel) {
        log.info("Adding channel {}", channelId);
        channels.put(channelId, channel);
    }

    public Optional<Channel> get(String channelId) {
        log.info("Getting channel {}", channelId);
        Channel channel = channels.get(channelId);

        return (channel != null && channel.isActive())
                ? Optional.of(channel)
                : Optional.empty();
    }

    public void remove(String channelId) {
        log.info("Removing channel {}", channelId);
        Channel remove = channels.remove(channelId);
        remove.close().addListener(
                future -> {
                    if (future.isSuccess()) {
                        log.info("Channel {} closed successfully", channelId);
                    } else {
                        log.error("Error closing channel {}", channelId, future.cause());
                    }
                }
        );
    }
}
