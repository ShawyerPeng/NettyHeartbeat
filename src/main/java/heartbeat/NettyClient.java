package heartbeat;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Netty 客户端
 */
public class NettyClient {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final static String HOST = "127.0.0.1";
    private final static int PORT = 8899;

    private NioEventLoopGroup workerGroup = new NioEventLoopGroup(4);
    private Bootstrap b;
    private Channel channel;
    // 重试次数
    private int tryTimes = 0;

    public static void main(String[] args) {
        // 配置客户端NIO线程组
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    .handler(new NettyClientInitializer())
                    .option(ChannelOption.TCP_NODELAY, true);

            // 发起异步连接操作
            ChannelFuture f = bootstrap.connect(HOST, PORT).sync();
            Channel channel = f.channel();

            // 标准输入
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            // 利用死循环，不断读取客户端在控制台上的输入内容
            while (true) {
                // 将读取的输入写入outbundle处理后发出
                channel.writeAndFlush(bufferedReader.readLine() + "\r\n").sync();
            }

            // 当代客户端链路关闭，服务器同步连接断开时,这句代码才会往下执行
            // 也就是服务端执行完这一句: ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            //channel.closeFuture().sync();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            // 优雅退出，释放NIO线程组
            group.shutdownGracefully();
        }
    }

    public void connect() throws Exception {
        try {
            b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.handler(new NettyClientInitializer());
            b.option(ChannelOption.SO_KEEPALIVE, true);
            doConnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 重连，连接失败 10 秒后重试连接
     */
    public void doConnect() throws IOException {
        if (channel != null && channel.isActive()) {
            return;
        }
        tryTimes++;
        logger.info("" + tryTimes);
        ChannelFuture future = b.connect(HOST, PORT);

        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture futureListener) throws Exception {
                if (futureListener.isSuccess()) {
                    channel = futureListener.channel();
                    channel.closeFuture();
                    workerGroup.shutdownGracefully();
                    logger.info("Connect to server successfully!");
                } else {
                    logger.warn("Failed to connect to server, try connect after 10s");
                    futureListener.channel().eventLoop().schedule(() -> {
                        try {
                            doConnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }, 10, TimeUnit.SECONDS);
                }
            }
        });

        // 标准输入
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        // 利用死循环，不断读取客户端在控制台上的输入内容
        for (; ; ) {
            channel.writeAndFlush(bufferedReader.readLine() + "\r\n");
        }
    }
}