package server.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import protobuf.Command;
import protobuf.Message;
import server.ChannelRepository;

/**
 * 连接认证 Handler
 * 1. 连接成功后客户端发送 CommandType.AUTH 指令，Sever 端验证通过后返回 CommandType.AUTH_BACK 指令
 * 2. 处理心跳指令
 * 3. 触发下一个 Handler
 */
@Component
@Qualifier("authServerHandler")
@ChannelHandler.Sharable
public class AuthServerHandler extends ChannelHandlerAdapter {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private final AttributeKey<String> clientInfo = AttributeKey.valueOf("clientInfo");

    @Autowired
    @Qualifier("channelRepository")
    private ChannelRepository channelRepository;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message.MessageBase msgBase = (Message.MessageBase) msg;
        String clientId = msgBase.getClientId();

        Channel ch = channelRepository.get(clientId);
        if (null == ch) {
            ch = ctx.channel();
            channelRepository.put(clientId, ch);
        }
        // 认证处理
        if (msgBase.getCmd().equals(Command.CommandType.AUTH)) {
            log.info("我是验证处理逻辑");
            Attribute<String> attr = ctx.attr(clientInfo);
            attr.set(clientId);
            channelRepository.put(clientId, ctx.channel());

            ctx.writeAndFlush(createData(clientId, Command.CommandType.AUTH_BACK, "This is response data").build());
        } else if (msgBase.getCmd().equals(Command.CommandType.PING)) {
            // 处理 ping 消息
            ctx.writeAndFlush(createData(clientId, Command.CommandType.PONG, "This is pong data").build());
        } else {
            if (ch.isOpen()) {
                // 触发下一个 handler
                ctx.fireChannelRead(msg);
                log.info("我进入业务处理逻辑");
            }
        }
        ReferenceCountUtil.release(msg);
    }

    private Message.MessageBase.Builder createData(String clientId, Command.CommandType cmd, String data) {
        Message.MessageBase.Builder msg = Message.MessageBase.newBuilder();
        msg.setClientId(clientId);
        msg.setCmd(cmd);
        msg.setData(data);
        return msg;
    }
}