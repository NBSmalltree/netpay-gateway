package com.atom.netpaygateway.codec;

import com.atom.netpaygateway.constants.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.CharsetUtil;

import java.util.List;

/**
 * 自定义 Socket XML解析器
 *
 * @author Tom
 * @date 11/3/2025
 */
public class XmlMessageDecoder extends ByteToMessageDecoder {

    /**
     * XML报文终止符号byte
     */
    private static final byte[] END_MARK_BYTES = Constants.END_MARK.getBytes(CharsetUtil.UTF_8);

    /**
     * 最大报文长度
     */
    private static final int MAX_FRAME_SIZE = 1024 * 1024 * 4;

    /**
     * 自定义解码器
     *
     * @param ctx 参数说明
     * @param byteBuf 参数说明
     * @param list 参数说明
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {
        // 检查缓冲区大小是否超过最大帧大小
        if (byteBuf.readableBytes() > MAX_FRAME_SIZE) {
            byteBuf.clear();
            throw new TooLongFrameException("Frame too large");
        }

        // 检查是否包含结束标记
        int endIndex = indexOf(byteBuf, END_MARK_BYTES);
        if (endIndex != -1) {
            // 计算完整报文的长度
            int length = endIndex + END_MARK_BYTES.length;
            // 提取完整报文
            ByteBuf fullMessage = byteBuf.readRetainedSlice(length);
            // 将完整报文添加到列表中
            list.add(fullMessage);
            // 丢弃已处理的数据
            byteBuf.discardReadBytes();
        }
    }

    /**
     * 获取指定字节数组在指定字节数组中的索引
     *
     * @param haystack 参数说明
     * @param needle 参数说明
     * @return 参数说明
     */
    private int indexOf(ByteBuf haystack, byte[] needle) {
        for (int i = 0; i < haystack.readableBytes() - needle.length + 1; i++) {
            boolean match = true;
            for (int j = 0; j < needle.length; j++) {
                if (haystack.getByte(i + j) != needle[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 异常处理
     *
     * @param ctx 参数说明
     * @param cause 参数说明
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
