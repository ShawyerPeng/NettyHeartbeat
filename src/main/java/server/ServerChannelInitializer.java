package server;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import protobuf.Message;
import server.handler.IdleServerHandler;

import java.util.concurrent.TimeUnit;

/**
 * Socket初始化
 */
@Component
@Qualifier("serverChannelInitializer")
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    // 该变量只能在当前这个类中被使用，并且是带有 static 修饰的静态函数中被调用，且该属性的值将不能被改变。
    private final static int READER_IDLE_TIME_SECONDS = 20;// 读操作空闲 20 秒
    private final static int WRITER_IDLE_TIME_SECONDS = 20;// 写操作空闲 20 秒
    private final static int ALL_IDLE_TIME_SECONDS = 40;// 读写全部空闲 40 秒

    @Autowired
    @Qualifier("authServerHandler")
    private ChannelHandlerAdapter authServerHandler;

    @Autowired
    @Qualifier("logicServerHandler")
    private ChannelHandlerAdapter logicServerHandler;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline p = socketChannel.pipeline();

        p.addLast("idleStateHandler", new IdleStateHandler(READER_IDLE_TIME_SECONDS,
                WRITER_IDLE_TIME_SECONDS, ALL_IDLE_TIME_SECONDS, TimeUnit.SECONDS));
        p.addLast("idleTimeoutHandler", new IdleServerHandler());

        p.addLast(new ProtobufVarint32FrameDecoder());
        p.addLast(new ProtobufDecoder(Message.MessageBase.getDefaultInstance()));

        p.addLast(new ProtobufVarint32LengthFieldPrepender());
        p.addLast(new ProtobufEncoder());

        p.addLast("authServerHandler", authServerHandler);
        p.addLast("logicServerHandler", logicServerHandler);
    }
}