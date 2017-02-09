/**
 *
 */
package com.lafaspot.pop.netty;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.pop.session.PopSession;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

/**
 * Class to decode messages from POP server.
 *
 * @author kraman
 *
 */
public class PopMessageDecoder extends MessageToMessageDecoder<String> {

	/** The session object for this decoder. */
    private final PopSession session;

    /**
     * Constructor for message decoder.
     * @param session the session object
     * @param logger the logger object
     */
    public PopMessageDecoder(@Nonnull final PopSession session, @Nonnull final Logger logger) {
        this.session = session;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final String line, final List<Object> out)
			throws IOException {
    	session.onMessage(line);
    }
}
