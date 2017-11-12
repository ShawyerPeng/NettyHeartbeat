package heartbeat;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * SimpleChannelInboundHandler 在接收到数据后会自动 release 掉数据占用的 Bytebuffer 资源 (自动调用 Bytebuffer.release())
 */
public class LogicClientHandler extends SimpleChannelInboundHandler {
    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
        System.out.println("--------messageReceived--- 服务器发来的数据为：[" + msg + "]");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("----------------handler channelActive----- 准备发送数据 -------");
        ctx.write("ping");
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("--------channelRead--- 服务器发来的数据为：[" + msg + "]");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("----------------handler channelReadComplete");
        ctx.flush();
    }


    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("----------------handler exceptionCaught");
        cause.printStackTrace();
        ctx.close();
    }
}