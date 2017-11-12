package server.handler;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 连接空闲 Handler
 */
@Component
public class IdleServerHandler extends ChannelHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    // 失败计数器：未收到client端发送的ping请求
    private int unRecPingTimes = 0;
    // 定义服务端没有收到心跳消息的最大次数
    private static final int MAX_UN_REC_PING_TIMES = 3;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                // 读超时
                logger.debug(ctx.channel().remoteAddress() + " 超时类型：read idle");
                // 失败计数器次数大于等于3次的时候，关闭链接，等待client重连
                if (unRecPingTimes >= MAX_UN_REC_PING_TIMES) {
                    System.out.println("===服务端===(读超时，关闭chanel)");
                    // 连续超过N次未收到client的ping消息，那么关闭该通道，等待client重连
                    ctx.channel().close();
                } else {
                    // 失败计数器加1
                    unRecPingTimes++;
                }
            } else if (event.state() == IdleState.WRITER_IDLE) {
                // 写超时
                logger.debug(ctx.channel().remoteAddress() + " 超时类型：write idle");
            } else if (event.state() == IdleState.ALL_IDLE) {
                // 总超时
                logger.debug(ctx.channel().remoteAddress() + " 超时类型：all idle");
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
