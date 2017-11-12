package heartbeat;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 服务器端 Initializer
 */
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    // 读操作超时20秒
    private final static int READER_IDLE_TIME_SECONDS = 20;
    // 写操作超时20秒
    private final static int WRITER_IDLE_TIME_SECONDS = 20;
    // 读写全部超时40秒
    private final static int ALL_IDLE_TIME_SECONDS = 40;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline p = socketChannel.pipeline();

        p.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        p.addLast("decoder", new StringDecoder());
        p.addLast("encoder", new StringEncoder());

        // 空闲状态处理器，用于检测通信Channel的读写状态超时，以此来实现心跳检测
        // IdleStateHandler 将通过 IdleStateEvent 调用 userEventTriggered ，如果连接没有接收或发送数据超过 60 秒钟
        // 定义的服务器端读事件的时间，当客户端 5s 时间没有往服务器写数据（服务器端就是读操作）则触发 IdleStateEvent 事件
        // 服务器端写事件的时间，当服务器端 7s 的时间没有向客户端写数据，则触发 IdleStateEvent 事件
        // 当客户端没有往服务器端写数据和服务器端没有往客户端写数据 10s 的时间，则触发 IdleStateEvent 事件
        p.addLast("idleStateHandler", new IdleStateHandler(5,7,10, TimeUnit.SECONDS));
        // 对空闲检测进一步处理的Handler
        p.addLast("idleTimeoutHandler", new LogicServerHandler());
    }
}