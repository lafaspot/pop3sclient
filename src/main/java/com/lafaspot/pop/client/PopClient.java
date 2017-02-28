/**
 *
 */
package com.lafaspot.pop.client;

import java.util.Random;
import java.util.concurrent.TimeUnit;
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

    /** The netty bootstrap. */
    private final Bootstrap bootstrap;

    /** Event loop group that will serve all channels for IMAP client. */
    private final EventLoopGroup group;

    /** The log manger. */
    private final LogManager logManager;

    /** The logger. */
    private Logger logger;

    /** The SSL context. */
    private final SslContext sslContext;
    
    /** The quit period time for shutdown. */
    private static final long SHUTDOWN_QUIET_PERIOD_MILLIS = 5000L;
    
    /** The timeout for shutdown. */
    private static final long SHUTDOWN_TIMEOUT_MILLIS = 30000L;
    
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
            this.bootstrap.group(this.group);
            this.bootstrap.channel(NioSocketChannel.class);
            this.bootstrap.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        } catch (final SSLException e) {
            throw new PopException(PopException.Type.INTERNAL_FAILURE, e);
        }
    }

    /**
     * Create PopSession.
     * 
     * @return PopSession
     */
    public PopSession createSession() {
        return new PopSession(this.sslContext, this.bootstrap, this.logger);
    }

    /**
     * Shut down the pop client.
     */
    public void shutdown() {
        this.group.shutdownGracefully(SHUTDOWN_QUIET_PERIOD_MILLIS, SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }
}
