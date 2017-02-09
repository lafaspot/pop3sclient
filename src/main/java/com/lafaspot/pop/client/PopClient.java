/**
 *
 */
package com.lafaspot.pop.client;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLException;

import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.pop.exception.PopException;
import com.lafaspot.pop.session.PopSession;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

/**
 * POP client that supports secure connection and POP3 protocol.
 *
 * @author kraman
 *
 */
public class PopClient {

    /** instance id used for debug. */
    private final String instanceId = Integer.toString(new Random(System.nanoTime()).nextInt());

    /** counter for sessions. */
    private AtomicInteger sessionCounter = new AtomicInteger(1);

    /** The netty bootstrap. */
    private final Bootstrap bootstrap;

    /** Event loop group that will serve all channels for IMAP client. */
    private final EventLoopGroup group;

    /** The log manger. */
    private final LogManager logManager;

    /** The logger. */
    private Logger logger;

    private final SslContext sslContext;
    /**
     * Constructor to create a new POP client.
     *
     * @param threads number of threads to use
     * @param logManager the log manager
     * @throws PopException on failure
     */
	public PopClient(final int threads, @Nonnull final LogManager logManager) throws PopException {

		this.logManager = logManager;
		LogContext context = new SessionLogContext("PopClient");
		this.logger = logManager.getLogger(context);

		this.bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup(threads);
		try {
			this.sslContext = SslContextBuilder.forClient().build();
			bootstrap.group(group);
			bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true); // (4)
		} catch (SSLException e) {
			throw new PopException(PopException.Type.INTERNAL_FAILURE, e);
		}
	}

	/**
	 * Create PopSession.
	 * @return PopSession
	 */
    public PopSession createSession() {
        return new PopSession(sslContext, bootstrap, logger);
    }

}
