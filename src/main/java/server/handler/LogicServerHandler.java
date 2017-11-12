package server.handler;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import protobuf.Command.CommandType;
import protobuf.Message;
import protobuf.Message.MessageBase;
import server.ChannelRepository;

/**
 * 业务逻辑 handler
 */
@Component
@Qualifier("logicServerHandler")
@ChannelHandler.Sharable
public class LogicServerHandler extends ChannelHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AttributeKey<String> clientInfo = AttributeKey.valueOf("clientInfo");

    @Autowired
    @Qualifier("channelRepository")
    private ChannelRepository channelRepository;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message.MessageBase msgBase = (Message.MessageBase)msg;

        logger.info(msgBase.getData());

        ChannelFuture cf = ctx.writeAndFlush(
                MessageBase.newBuilder()
                        .setClientId(msgBase.getClientId())
                        .setCmd(CommandType.UPLOAD_DATA_BACK)
                        .setData("This is upload data back msg")
                        .build()
        );
		// 上一条消息发送成功后，立马推送一条消息
        cf.addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                if (future.isSuccess()){
                    ctx.writeAndFlush(
                            MessageBase.newBuilder()
                                    .setClientId(msgBase.getClientId())
                                    .setCmd(CommandType.PUSH_DATA)
                                    .setData("This is a push msg")
                                    .build()
                    );
                }
            }
        });
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Attribute<String> attr = ctx.attr(clientInfo);
        String clientId = attr.get();
        logger.error("Connection closed, client is " + clientId);
        cause.printStackTrace();
    }
}