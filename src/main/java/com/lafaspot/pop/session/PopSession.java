/**
 *
 */
package com.lafaspot.pop.session;

import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

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
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * The PopSession object, used to conenct and send commands to the server.
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
        logger.debug(" +++ connect to  " + server, null);

        if (!stateRef.compareAndSet(State.NULL, State.CONNECTED)) {
            throw new PopException(Type.INVALID_STATE);
        }

        final PopSession thisSession = this;
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                p.addLast(SSL_HANDLER, sslContext.newHandler(ch.alloc(), server, port));
                p.addLast(INACTIVITY_HANDLER, new PopInactivityHandler(thisSession, inactivityTimeout, logger));
                p.addLast(DELIMITER, new DelimiterBasedFrameDecoder(MAX_LINE_LENGTH, Delimiters.lineDelimiter()));
                p.addLast(DECODER, new StringDecoder());
                p.addLast(ENCODER, new StringEncoder());
                p.addLast(POP_HANDLER, new PopMessageDecoder(thisSession, logger));
            }

        });

        final PopCommand cmd = new PopCommand(PopCommand.Type.INVALID);
        ChannelFuture future;
        try {
            future = bootstrap.connect(server, port).sync();
        } catch (InterruptedException e) {
            throw new PopException(Type.CONNECT_FAILURE, e);
        }

        sessionChannel = future.channel();
        PopFuture<PopCommandResponse> connectFuture = new PopFuture<PopCommandResponse>(future);
        cmd.setCommandFuture(connectFuture);
        future.addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(final Future<? super Void> future) throws Exception {
                if (future.isSuccess()) {
					// connectFuture.done(new PopCommandResponse(cmd));
                	/*
                    if (!stateRef.compareAndSet(State.CONNECT_SENT, State.WAIT_FOR_OK)) {
                        logger.error("Connect success in invalid state " + stateRef.get().name(), null);
                        return;
                    }
                    */
                }
            }
        });

		commandList.add(cmd);
		return connectFuture;
    }

    /**
     * Send a POP command to the server.
     * @param command the command to send to server
     * @return the future object for this command
     * @throws PopException on failure
     */
    public PopFuture<PopCommandResponse> execute(@Nonnull final PopCommand command) throws PopException {

        final StringBuilder commandToWrite = new StringBuilder();
        commandToWrite.append(command.getCommandLine());
        final Future f = sessionChannel.writeAndFlush(commandToWrite.toString());
        final PopFuture<PopCommandResponse> currentCommandFuture = new PopFuture<PopCommandResponse>(f);
        command.setCommandFuture(currentCommandFuture);
        commandList.add(command);
        return currentCommandFuture;
    }

    /**
     * Disconnect the session, close session and cleanup.
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

    }

    /**
     * Called when response message is being received from the server. Delimiter is \r\n.
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
