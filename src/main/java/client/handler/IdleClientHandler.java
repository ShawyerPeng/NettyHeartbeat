package client.handler;

import client.NettyClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.Command;
import protobuf.Message;

public class IdleClientHandler extends SimpleChannelInboundHandler<Message> {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private NettyClient nettyClient;
    private int heartbeatCount = 0;
    private final static String CLIENTID = "123456789";

    public IdleClientHandler(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            String type = "";
            if (event.state() == IdleState.READER_IDLE) {
                type = "read idle";
            } else if (event.state() == IdleState.WRITER_IDLE) {
                type = "write idle";
            } else if (event.state() == IdleState.ALL_IDLE) {
                type = "all idle";
            }
            logger.debug(ctx.channel().remoteAddress() + " 超时类型：" + type);
            sendPingMsg(ctx);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 发送 ping 消息
     */
    protected void sendPingMsg(ChannelHandlerContext context) {
        context.writeAndFlush(
                Message.MessageBase.newBuilder()
                        .setClientId(CLIENTID)
                        .setCmd(Command.CommandType.PING)
                        .setData("This is a ping msg")
                        .build()
        );
        heartbeatCount++;
        logger.info("Client sent ping msg to " + context.channel().remoteAddress() + ", count: " + heartbeatCount);
    }

    /**
     * 处理断开重连
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        nettyClient.doConnect();
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Message msg) throws Exception {

    }
}