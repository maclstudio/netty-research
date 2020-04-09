package io.netty.example.study.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.study.server.codec.OrderFrameDecoder;
import io.netty.example.study.server.codec.OrderFrameEncoder;
import io.netty.example.study.server.codec.OrderProtocolDecoder;
import io.netty.example.study.server.codec.OrderProtocolEncoder;
import io.netty.example.study.server.codec.handler.MetricHandler;
import io.netty.example.study.server.codec.handler.OrderServerProcessHandler;
import io.netty.example.study.server.codec.handler.ServerIdleCheckHandler;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

import java.util.concurrent.ExecutionException;

/**
 * @author : chenglong.ma@shuyun.com
 * @date : 2020/4/6
 */
public class Server {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.channel(NioServerSocketChannel.class);
        NioEventLoopGroup boss = new NioEventLoopGroup(0, new DefaultThreadFactory("boss"));
        NioEventLoopGroup worker = new NioEventLoopGroup(0, new DefaultThreadFactory("worker"));

        bootstrap.group(boss, worker);
        //可以发送小报文，不要等
        bootstrap.childOption(NioChannelOption.TCP_NODELAY, true);
        //等待连接数，默认128
        bootstrap.option(NioChannelOption.SO_BACKLOG, 1024);


        MetricHandler metricHandler = new MetricHandler();

        UnorderedThreadPoolEventExecutor eventExecutors = new UnorderedThreadPoolEventExecutor(10, new DefaultThreadFactory("business"));
        //以下不用使用池，只会取其中一个线程
        //NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("business"));
        //流量整形，全局配置
        GlobalTrafficShapingHandler globalTrafficShapingHandler = new GlobalTrafficShapingHandler(new NioEventLoopGroup(),
                100 * 1024 * 1024, 100 * 1024 * 1024, 1* 1000);
        bootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("LoggingHandler", new LoggingHandler(LogLevel.DEBUG));

                pipeline.addLast("TSHandler", globalTrafficShapingHandler);
                pipeline.addLast("ServerIdleCheckHandler", new ServerIdleCheckHandler());

                pipeline.addLast("OrderFrameDecoder", new OrderFrameDecoder());
                pipeline.addLast("OrderFrameEncoder", new OrderFrameEncoder());
                pipeline.addLast("OrderProtocolDecoder", new OrderProtocolDecoder());
                pipeline.addLast("OrderProtocolEncoder", new OrderProtocolEncoder());

                pipeline.addLast("MetricHandler", metricHandler);

                //调整flush次数并开启异步情况下多个write刷新一次的功能
                pipeline.addLast("flushEnhance", new FlushConsolidationHandler(5, true));

                pipeline.addLast(eventExecutors, new OrderServerProcessHandler());
            }
        });

        ChannelFuture channelFuture = bootstrap.bind(8090).sync();
        channelFuture.channel().closeFuture().get();
    }
}
