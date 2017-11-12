package client;

import client.handler.IdleClientHandler;
import client.handler.LogicClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * netty 客户端
 */
public class NettyClient {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final static String HOST = "127.0.0.1";
    private final static int PORT = 8090;
    private final static int READER_IDLE_TIME_SECONDS = 20; // 读操作空闲 20 秒
    private final static int WRITER_IDLE_TIME_SECONDS = 20; // 写操作空闲 20 秒
    private final static int ALL_IDLE_TIME_SECONDS = 40;    // 读写全部空闲 40 秒

    private NioEventLoopGroup workerGroup = new NioEventLoopGroup(4);
    private Bootstrap b;
    private Channel channel;
    private int tryTimes = 0;

    public static void main(String[] args) throws Exception {
        NettyClient client = new NettyClient();
        client.connect(HOST, PORT);
    }

    public void connect(String host, int port) throws Exception {
        try {
            b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();

                    // 心跳
                    p.addLast("idleStateHandler", new IdleStateHandler(READER_IDLE_TIME_SECONDS
                            , WRITER_IDLE_TIME_SECONDS, ALL_IDLE_TIME_SECONDS, TimeUnit.SECONDS));
                    p.addLast("idleTimeoutHandler", new IdleClientHandler(NettyClient.this));

                    p.addLast(new ProtobufVarint32FrameDecoder());
                    p.addLast(new ProtobufDecoder(Message.MessageBase.getDefaultInstance()));

                    p.addLast(new ProtobufVarint32LengthFieldPrepender());
                    p.addLast(new ProtobufEncoder());

                    p.addLast("clientHandler", new LogicClientHandler());
                }
            });
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

        //标准输入
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        //利用死循环，不断读取客户端在控制台上的输入内容
        for (; ; ) {
            channel.writeAndFlush(bufferedReader.readLine() + "\r\n");
        }
    }
}