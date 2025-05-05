package uz.server.service.tcp.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uz.server.domain.entity.TcpTunnel;
import uz.server.ws.Sender;
import uz.server.ws.tcp.TcpChannelHolder;

import java.nio.ByteBuffer;

@Slf4j
@Getter
@RequiredArgsConstructor
public class TcpHandler extends ChannelDuplexHandler {
    private final TcpTunnel tunnel;
    private final Sender sender;
    private final TcpChannelHolder tcpChannelHolder;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        String channelId = channel.id().asShortText();

        log.info("TCP connected: tunnel={}, channel={}", tunnel.getId(), channelId);
        tcpChannelHolder.add(channelId, channel);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;

        ByteBuffer byteBuffer = byteBuf.nioBuffer();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);

        sender.sendBinary(this.tunnel.getSessionId(), ByteBuffer.wrap(bytes));

        byteBuf.release();
    }
}