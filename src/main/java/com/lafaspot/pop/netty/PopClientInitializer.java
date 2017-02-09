package com.lafaspot.pop.netty;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import javax.annotation.Nonnull;

import com.lafaspot.logfast.logging.Logger;

/**
* The client initilaized, setup the static encoder/decoders.
*
* @author kraman
*
*/
public class PopClientInitializer extends ChannelInitializer<SocketChannel> {

	/** The Logger object.*/
    private final Logger logger;

    /** 
     * Constructor for the client initializer.
     * @param logger the logger object
     */
    public PopClientInitializer(@Nonnull final Logger logger) {
        this.logger = logger;
    }

   @Override
   protected void initChannel(final SocketChannel ch) throws Exception {

        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.lineDelimiter()));
        ch.pipeline().addLast(new StringDecoder());
        ch.pipeline().addLast(new StringEncoder());
   }

}

