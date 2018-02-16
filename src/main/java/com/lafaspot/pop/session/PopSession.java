/**
 *
 */
package com.lafaspot.pop.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.pop.command.PopCommand;
import com.lafaspot.pop.command.PopCommandResponse;
import com.lafaspot.pop.exception.PopException;
import com.lafaspot.pop.exception.PopException.Type;
import com.lafaspot.pop.netty.PopMessageDecoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * The PopSession object, used to conenct and send commands to the server.
 *
 * @author kraman
 *
 */
public class PopSession {

    /** Future for the current command being executed. */
    // private PopFuture<PopCommandResponse> currentCommandFuture;

    /** State of the sesson. */
    private final AtomicReference<State> stateRef;

    /** Netty bootstrap object. */
    private final Bootstrap bootstrap;

    /** The logger object. */
    private final Logger logger;

    /** The Netty session object. */
    private Channel sessionChannel;

    /** current list of commands being processed. */
    private final LinkedHashSet<PopCommand> commandList = new LinkedHashSet<PopCommand>();

    /** The SslContext object. */
    private final SslContext sslContext;
    /** Max line length. */
    private static final int MAX_LINE_LENGTH = 8192;

    /** The string identifier for ssl handler. */
    public static final String SSL_HANDLER = "sslHandler";

    /** The string identifier for inactivity handler. */
    public static final String INACTIVITY_HANDLER = "inactivityHandler";

    /** The string identifier for delimiter. */
    public static final String DELIMITER = "delimiter";

    /** The string identifier for encoder. */
    public static final String ENCODER = "encoder";

    /** The string identifier for decoder. */
    public static final String DECODER = "decoder";

    /** The string identifier for pop handler. */
    public static final String POP_HANDLER = "popHandler";

    /**
     * Constructor for PopSession, used to communicate with a POP server.
     *
     * @param sslContext the ssl context object
     * @param bootstrap the Netty bootstrap object
     * @param logger the logger object
     */
    public PopSession(@Nonnull final SslContext sslContext, @Nonnull final Bootstrap bootstrap, @Nonnull final Logger logger) {
        this.sslContext = sslContext;
        this.bootstrap = bootstrap;
        this.logger = logger;
        this.stateRef = new AtomicReference<>(State.NULL);
    }

    /**
     * Returns the channel that the pop client is using to connect to the remote server.
     *
     * @return the channel used to connect to the remote server or null if the session is not connected.
     */
    public Channel getChannel() {
        return sessionChannel;
    }

    /**
     * Connect to the specified POP server with the autoread option.
     *
     * @param server the server to connect to
     * @param port to connect to
     * @param connectTimeout timeout value
     * @param inactivityTimeout timeout value
     * @return future object for connect
     * @throws PopException on failure
     */
    public PopFuture<PopCommandResponse> connect(@Nonnull final String server, final int port, final int connectTimeout, final int inactivityTimeout)
            throws PopException {
        return connect(server, port, connectTimeout, inactivityTimeout, Arrays.asList(new String[] {}));
    }

    /**
     * Connect to the specified POP server with the autoread option.
     *
     * @param server the server to connect to
     * @param port to connect to
     * @param connectTimeout timeout value
     * @param inactivityTimeout timeout value
     * @param sniList list of server name indicators
     * @return future object for connect
     * @throws PopException on failure
     */

    public PopFuture<PopCommandResponse> connect(@Nonnull final String server, final int port, final int connectTimeout, final int inactivityTimeout,
            @Nonnull final List<String> sniList) throws PopException {
        logger.debug(" +++ connect to  " + server, null);

        if (!stateRef.compareAndSet(State.NULL, State.CONNECTED)) {
            throw new PopException(Type.INVALID_STATE);
        }

        final List<SNIServerName> serverList = new ArrayList<SNIServerName>();
        if (null != sniList && !sniList.isEmpty()) {
            try {
                for (final String sni : sniList) {
                    serverList.add(new SNIHostName(sni));
                }
            } catch (IllegalArgumentException iae) {
                throw new PopException(PopException.Type.INVALID_ARGUMENTS);
            }
        }

        final PopSession thisSession = this;
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                if (!serverList.isEmpty()) {
                    final SSLParameters params = new SSLParameters();
                    params.setServerNames(serverList);
                    final SSLEngine engine = SslContextBuilder.forClient().build().newEngine(ch.alloc());
                    engine.setSSLParameters(params);
                    pipeline.addLast(SSL_HANDLER, new SslHandler(engine));
                } else {
                    pipeline.addLast(SSL_HANDLER, sslContext.newHandler(ch.alloc(), server, port));
                }

                pipeline.addLast(INACTIVITY_HANDLER, new PopInactivityHandler(thisSession, inactivityTimeout, logger));
                pipeline.addLast(DELIMITER, new DelimiterBasedFrameDecoder(MAX_LINE_LENGTH, Delimiters.lineDelimiter()));
                pipeline.addLast(DECODER, new StringDecoder());
                pipeline.addLast(ENCODER, new StringEncoder());
                pipeline.addLast(POP_HANDLER, new PopMessageDecoder(thisSession, logger));
            }
        });

        final PopCommand cmd = new PopCommand(PopCommand.Type.INVALID_POP_COMMAND_CONNECT);
        ChannelFuture nettyConnectFuture;
        PopFuture<PopCommandResponse> connectFuture;
        try {
            nettyConnectFuture = bootstrap.connect(server, port); // .sync();
            sessionChannel = nettyConnectFuture.channel();
            connectFuture = new PopFuture<PopCommandResponse>(nettyConnectFuture);
            cmd.setCommandFuture(connectFuture);
            commandList.add(cmd);


            // handle connect done
            nettyConnectFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(final Future<? super Void> future) throws Exception {
                    logger.debug("=== channel conneced " + commandList.size(), null);
                    if (commandList.size() > 0) {
                        final PopCommand command = (PopCommand) commandList.toArray()[0];
                        if (command.getType().equals(PopCommand.Type.INVALID_POP_COMMAND_CONNECT)) {
                            // wait for +OK from server to declare command complete
                            //commandList.remove(command);
                        }
                        final PopFuture<PopCommandResponse> currentCommandFuture = command.getCommandFuture();
                        if (null != currentCommandFuture) {
                            logger.debug("+++ connect marking done  " + currentCommandFuture, null);
                            // CONNECT action is not done until we receive the first +OK response from server
                            //currentCommandFuture.done(new PopCommandResponse(command));
                        } else {
                            logger.debug("+++ connect future is null ", null);
                        }
                    }
                }
            });


            // close handling
            nettyConnectFuture.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(final Future<? super Void> future) throws Exception {
                    logger.debug("+++ channel disconneced " + commandList.size(), null);
                    if (commandList.size() > 0) {
                        final PopCommand command = (PopCommand) commandList.toArray()[0];
                        final PopFuture<PopCommandResponse> currentCommandFuture = command.getCommandFuture();
                        if (null != currentCommandFuture) {
                            logger.debug("+++ disc marking done  " + currentCommandFuture, null);
                            currentCommandFuture.done(new PopException(PopException.Type.CHANNEL_DISCONNECTED));
                        } else {
                            logger.debug("+++ disc future is null ", null);
                        }
                    }
                }
            });
            nettyConnectFuture.sync();
            return connectFuture;

        } catch (InterruptedException e) {
            throw new PopException(Type.CONNECT_FAILURE, e);
        }
    }

    /**
     * Send a POP command to the server.
     *
     * @param command the command to send to server
     * @return the future object for this command
     * @throws PopException on failure
     */
    public PopFuture<PopCommandResponse> execute(@Nonnull final PopCommand command) throws PopException {

        if (sessionChannel == null || !sessionChannel.isActive()) {
            throw new PopException(PopException.Type.CHANNEL_NOT_CONNECTED);
        }

        /*
        if (!commandList.isEmpty()) {
            // don't support pipelining
            throw new PopException(PopException.Type.INVALID_STATE);
        }
        */

        final StringBuilder commandToWrite = new StringBuilder();
        commandToWrite.append(command.getCommandLine());

        final Future<Void> writeFuture = sessionChannel.writeAndFlush(commandToWrite.toString());
        final PopFuture<PopCommandResponse> currentCommandFuture = new PopFuture<PopCommandResponse>(writeFuture);

        command.setCommandFuture(currentCommandFuture);
        commandList.add(command);
        return currentCommandFuture;
    }

    /**
     * Disconnect the session, close session and cleanup.
     *
     * @return the future object for disconnect
     * @throws PopException on failure
     */
    public PopFuture<PopCommandResponse> disconnect() throws PopException {
        final State state = stateRef.get();
        if (state != State.CONNECTED) {
            throw new PopException(PopException.Type.INVALID_STATE);
        }
        if (stateRef.compareAndSet(state, State.NULL)) {
            Future f = sessionChannel.disconnect();
            PopFuture<PopCommandResponse> disconnectFuture = new PopFuture<PopCommandResponse>(f);
            sessionChannel = null;
            return disconnectFuture;
        }
        throw new PopException(PopException.Type.INVALID_STATE);
    }

    /**
     * Callback from netty on channel inactivity.
     */
    public void onTimeout() {
        logger.debug("**channel timeout** TH " + Thread.currentThread().getId(), null);

        if (commandList.isEmpty()) {
            return;
        }
        final PopCommand command = (PopCommand) commandList.toArray()[0];
        final PopFuture<PopCommandResponse> currentCommandFuture = command.getCommandFuture();
        if (null == currentCommandFuture) {
            return;
        }
        currentCommandFuture.done(new PopException(PopException.Type.CHANNEL_DISCONNECTED));

    }

    /**
     * Called when response message is being received from the server. Delimiter is \r\n.
     *
     * @param line the response line
     */
    public void onMessage(final String line) {
        if (commandList.isEmpty()) {
            // something went wrong, shutdown and bail out
            try {
                disconnect();
            } catch (PopException e) {
                // ignore
            }
            return;
        }

        final PopCommand command = (PopCommand) commandList.toArray()[0];
        final PopFuture<PopCommandResponse> currentCommandFuture = command.getCommandFuture();
        if (null == currentCommandFuture) {
            // fatal
            // something went wrong, shutdown and bail out
            try {
                disconnect();
            } catch (PopException e) {
                // ignore
            }
            return;
        }

        command.getResponse().parse(line);
        if (command.getResponse().parseComplete()) {
            commandList.remove(command);
            currentCommandFuture.done(command.getResponse());
        }
    }


    /**
     * States of PopSession.
     *
     * @author kraman
     *
     */
    public enum State {
        /** Null session not connected. */
        NULL,
        /** Session is connected, ready to accept commands. */
        CONNECTED;
    }
}
