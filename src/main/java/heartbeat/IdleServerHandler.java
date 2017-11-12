package heartbeat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 连接空闲 Handler
 */
public class IdleServerHandler extends ChannelHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    // 失败计数器：未收到client端发送的ping请求，存有客户端超时次数
    private ConcurrentMap<String, AtomicInteger> concurrentMap = new ConcurrentHashMap<>();
    // 定义服务端没有收到心跳消息的最大次数，超时次数超过该值则注销连接
    private final static int MAX_IDLE_TIME = 3;

    // 定义了心跳时要发送到客户端的内容
    private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("HEARTBEAT", CharsetUtil.UTF_8));

    /**
     * 覆写userEventTriggered()方法处理超时逻辑。当超时次数少于指定次数时，向客户端发送Ping包；当超时次数大于指定次数时，注销客户端连接
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 心跳包检测读超时
        if (evt instanceof IdleStateEvent) {
            // 发送的心跳并添加一个侦听器，将心跳内容发送给客户端，如果发送操作失败将关闭连接
            ctx.writeAndFlush(HEARTBEAT_SEQUENCE.duplicate()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                // 读超时
                processReadIdle(ctx);
            } else if (event.state() == IdleState.WRITER_IDLE) {
                // 写超时
                processWriteIdle(ctx);
            } else if (event.state() == IdleState.ALL_IDLE) {
                // 读超时
                processAllIdle(ctx);
            }
        } else {
            // 事件不是一个 IdleStateEvent 的话，就将它传递给下一个处理程序
            super.userEventTriggered(ctx, evt);
        }
    }

    private void processReadIdle(ChannelHandlerContext ctx) {
        String channelId = ctx.channel().id().asLongText();
        AtomicInteger idleTimes = concurrentMap.get(channelId);
        if (null == idleTimes) {
            idleTimes = new AtomicInteger(1);
            concurrentMap.putIfAbsent(channelId, idleTimes);
        }
        // 失败计数器加1
        int times = idleTimes.getAndIncrement();
        logger.info(ctx.channel().remoteAddress() + "客户端读超时 " + times + "次");
        // 失败计数器次数大于等于3次的时候，关闭链接，等待client重连
        if (times >= MAX_IDLE_TIME) {
            logger.info("===服务端===(读超时，关闭chanel)");
            ctx.channel().close();
        }
    }

    private void processWriteIdle(ChannelHandlerContext ctx) {
        logger.info(ctx.channel().remoteAddress() + "客户端写超时");
    }

    private void processAllIdle(ChannelHandlerContext ctx) {
        logger.info(ctx.channel().remoteAddress() + "客户端读写全超时");
    }
}