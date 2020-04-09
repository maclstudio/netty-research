package io.netty.example.study.server.codec.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author : chenglong.ma@shuyun.com
 * @date : 2020/4/9
 */
@Slf4j
public class ServerIdleCheckHandler extends IdleStateHandler {
    public ServerIdleCheckHandler() {
        super(10L, 0, 0, TimeUnit.SECONDS);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if(evt == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT) {
            log.warn("idle state happen, so close collection");
            ctx.close();
            return;
        }
        super.channelIdle(ctx, evt);
    }
}
