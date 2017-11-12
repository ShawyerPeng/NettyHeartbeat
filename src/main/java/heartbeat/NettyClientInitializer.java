package heartbeat;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * 客户端 Initializer
 */
public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {
    // 该变量只能在当前这个类中被使用，并且是带有 static 修饰的静态函数中被调用，且该属性的值将不能被改变。
    private final static int READER_IDLE_TIME_SECONDS = 30; // 读操作空闲 30 秒
    private final static int WRITER_IDLE_TIME_SECONDS = 60; // 写操作空闲 60 秒
    private final static int ALL_IDLE_TIME_SECONDS = 100;   // 读写全部空闲 100 秒

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline p = socketChannel.pipeline();

        //p.addLast("idleStateHandler", new IdleStateHandler(READER_IDLE_TIME_SECONDS, WRITER_IDLE_TIME_SECONDS, ALL_IDLE_TIME_SECONDS, TimeUnit.SECONDS));
        //p.addLast("idleTimeoutHandler", new IdleClientHandler());

        //p.addLast(new ProtobufVarint32FrameDecoder());
        //p.addLast(new ProtobufDecoder(Message.MessageBase.getDefaultInstance()));
        //p.addLast(new ProtobufVarint32LengthFieldPrepender());
        //p.addLast(new ProtobufEncoder());

        //p.addLast(new ObjectDecoder(1024, ClassResolvers.cacheDisabled(this.getClass().getClassLoader())));
        //p.addLast(new ObjectEncoder());

        p.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        p.addLast("decoder", new StringDecoder());
        p.addLast("encoder", new StringEncoder());

        p.addLast("logicClientHandler", new LogicClientHandler());
    }
}