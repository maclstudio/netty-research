package io.netty.example.study.client.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.example.study.common.Operation;
import io.netty.example.study.common.RequestMessage;
import io.netty.example.study.util.IdUtil;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * @author : chenglong.ma@shuyun.com
 * @date : 2020/4/6
 */
public class OperationToRequestMessageEncoder extends MessageToMessageEncoder<Operation> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Operation operation, List<Object> out) throws Exception {
        RequestMessage requestMessage = new RequestMessage(IdUtil.nextId(), operation);
        out.add(requestMessage);
    }
}
