/**
 *
 */
package com.lafaspot.pop.client;

import java.util.Iterator;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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

    /** The logger. */
    private Logger logger;

    /** The SSL context. */
    private final SslContext sslContext;

    /**
     * Constructor to create a new POP client.
     *
     * @param threads number of threads to use
     * @param logManager the log manager
     * @throws PopException on failure
     */
    public PopClient(final int threads, @Nonnull final LogManager logManager) throws PopException {

        LogContext context = new SessionLogContext("PopClient");
        this.logger = logManager.getLogger(context);

        this.bootstrap = new Bootstrap();
        this.group = new NioEventLoopGroup(threads);
        try {
            this.sslContext = SslContextBuilder.forClient().build();
            this.bootstrap.group(this.group);
            this.bootstrap.channel(NioSocketChannel.class);
            this.bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        } catch (final SSLException e) {
            throw new PopException(PopException.Type.INTERNAL_FAILURE, e);
        }
    }

    /**
     * Constructor to create a new POP client with the system properties.
     *
     * @param properties the bootstrap properties
     * @param threads number of threads to use
     * @param logManager the log manager
     * @throws PopException on failure
     */
    public PopClient(final int threads, @Nonnull final LogManager logManager, @Nonnull final Properties properties) throws PopException {

        LogContext context = new SessionLogContext("PopClient");
        this.logger = logManager.getLogger(context);
        this.bootstrap = new Bootstrap();
        this.group = new NioEventLoopGroup(threads);
        try {
            this.sslContext = SslContextBuilder.forClient().build();
            this.bootstrap.group(this.group);
            this.bootstrap.channel(NioSocketChannel.class);
            this.bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            final Iterator<Object> it = properties.keySet().iterator();
            while (it.hasNext()) {
                final ChannelOption obj = (ChannelOption) it.next();
                this.bootstrap.option(obj, properties.get(obj));
            }
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
     *
     * @param quietPeriod quiet period to ensure no tasks submitted
     * @param timeout timeout of shutdown
     */
    public void shutdown(final long quietPeriod, final long timeout) {
        this.group.shutdownGracefully(quietPeriod, timeout, TimeUnit.MILLISECONDS);
    }
}
