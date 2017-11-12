package client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.Command;
import protobuf.Message;

public class LogicClientHandler extends SimpleChannelInboundHandler<Message.MessageBase> {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final static String CLIENTID = "123456789";

    // 连接成功后，向 server 发送消息
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Message.MessageBase.Builder authMsg = Message.MessageBase.newBuilder();
        authMsg.setClientId(CLIENTID);
        authMsg.setCmd(Command.CommandType.AUTH);
        authMsg.setData("This is auth data");

        ctx.writeAndFlush(authMsg.build());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.debug("连接断开");
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Message.MessageBase msg) throws Exception {
        if (msg.getCmd().equals(Command.CommandType.AUTH_BACK)) {
            logger.debug("验证成功");
            ctx.writeAndFlush(Message.MessageBase.newBuilder()
                    .setClientId(CLIENTID)
                    .setCmd(Command.CommandType.PUSH_DATA)
                    .setData("This is upload data")
                    .build());
        } else if (msg.getCmd().equals(Command.CommandType.PING)) {
            // 接收到 server 发送的 ping 指令
            logger.info(msg.getData());
        } else if (msg.getCmd().equals(Command.CommandType.PONG)) {
            // 接收到 server 发送的 pong 指令
            logger.info(msg.getData());
        } else if (msg.getCmd().equals(Command.CommandType.PUSH_DATA)) {
            // 接收到 server 推送数据
            logger.info(msg.getData());
        } else if (msg.getCmd().equals(Command.CommandType.PUSH_DATA_BACK)) {
            // 接收到 server 返回数据
            logger.info(msg.getData());
        } else {
            logger.info(msg.getData());
        }
    }
}