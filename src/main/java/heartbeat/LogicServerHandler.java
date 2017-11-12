package heartbeat;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 连接空闲 Handler
 * 定义的 LogicServerHandler 没有去继承 SimpleChannelInboundHandler
 * 而是继承 SimpleChannelInboundHandler 的父类 ChannelInboundHandlerAdapter
 */
public class LogicServerHandler extends ChannelHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    // 失败计数器：未收到client端发送的ping请求，存有客户端超时次数
    private ConcurrentMap<String, AtomicInteger> concurrentMap = new ConcurrentHashMap<>();
    // 定义服务端没有收到心跳消息的最大次数，超时次数超过该值则注销连接
    private final static int MAX_IDLE_TIME = 3;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("收到来自客户端的消息：[" + msg + "]");
        // 如果客户端发出的消息是ping，则表示收到数据包
        if (msg.toString().equals("ping")) {
            ctx.writeAndFlush("pong\n");
            String channelId = ctx.channel().id().asLongText();
            // 只要接受到数据包，则清空超时次数
            concurrentMap.remove(channelId);
        } else {
            String channelId = ctx.channel().id().asLongText();
            String resMsg = "hello client " + channelId;
            ctx.writeAndFlush(Unpooled.copiedBuffer(resMsg.getBytes()));
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.info("业务逻辑出错");
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 覆写userEventTriggered()方法处理超时逻辑。当超时次数少于指定次数时，向客户端发送Ping包；当超时次数大于指定次数时，注销客户端连接
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 心跳包检测读超时，判断是否是 IdleStateEvent 事件，是则处理
        if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
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
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("客户端关闭1");
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.disconnect(promise);
        logger.info("客户端关闭2");
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
        logger.info("TCP closed...");
        ctx.close(promise);
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
        logger.info(ctx.channel().remoteAddress() + "客户端读超时 " + times + " 次");
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