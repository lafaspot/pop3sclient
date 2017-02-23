/**
 *
 */
package com.lafaspot.pop.command;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.lafaspot.pop.session.PopFuture;

/**
 * Pop command.
 * @author kraman
 *
 */
public class PopCommand {
	/** Command type. */
	private final Type type;
	/** Arguments. */
	private final List<String> args = new ArrayList<String>();

	/** Response. */
	private final PopCommandResponse response;

	/** future for this command. */
	private PopFuture<PopCommandResponse> commandFuture;

	/** The command type as string. */
	private final String typeStr;

	/**
	 * Constructor.
	 *
	 * @param type
	 *            command type
	 */
	public PopCommand(@Nonnull final Type type) {
		this.type = type;
		this.typeStr = type.name();
		this.response = new PopCommandResponse(this);
	}

	/**
	 * Generic command constructor.
	 *
	 * @param typeStr
	 *            command as string
	 */
	public PopCommand(@Nonnull final String typeStr) {
		this.type = Type.GENERIC_STRING;
		this.typeStr = typeStr;
		this.response = new PopCommandResponse(this);
	}

	/**
	 * @return the commandFuture
	 */
	public PopFuture<PopCommandResponse> getCommandFuture() {
		return commandFuture;
	}



	/**
	 * @param commandFuture
	 *            the commandFuture to set
	 */
	public void setCommandFuture(@Nonnull final PopFuture<PopCommandResponse> commandFuture) {
		this.commandFuture = commandFuture;
	}



	/**
	 * Add optional arguments.
	 *
	 * @param arg
	 *            arguments
	 * @return this command for chaining
	 */
	public PopCommand addArgs(@Nonnull final String arg) {
		args.add(arg);
		return this;
	}

	/**
	 * Get the command line for this command - the line to be sent over wire.
	 *
	 * @return command line
	 */
	public String getCommandLine() {
		final StringBuffer buf = new StringBuffer();
		buf.append(typeStr);
		if (args.size() > 0) {
			for (final String arg : args) {
				buf.append(" ");
				buf.append(arg);
			}
		}
		buf.append("\r\n");
		return buf.toString();
	}

	/**
	 * Get the command type.
	 *
	 * @return command type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Get command response.
	 *
	 * @return command response
	 */
	public PopCommandResponse getResponse() {
		return response;
	}

	/**
	 * Return the String equivalent of this command.
	 *
	 * @return the string value
	 */
	@Override
	public String toString() {
		final StringBuffer buf = new StringBuffer();
		buf.append(typeStr);

		if (null != args && !args.isEmpty()) {
			buf.append(args);
		}

		return buf.toString();
	}

	/**
	 * Pop command type enum.
	 *
	 * @author kraman
	 *
	 */
	public enum Type {
		/** Invalid command. */
		INVALID(0, false),
		/** User command. */
		USER(1, false),
		/** Pass command. */
		PASS(2, false),
		/** Stat command. */
		STAT(3, false),
		/** List command. */
		LIST(4, true),
		/** Retr command. */
		RETR(5, true),
		/** Dele command. */
		DELE(6, false),
		/** Noop command. */
		NOOP(7, false),
		/** Rset command. */
		RSET(8, false),
		/** Top comamnd. */
		TOP(9, true),
		/** Uidl command. */
		UIDL(10, true),
		/** Uidl command. */
		QUIT(11, false),
		/** Capa command. */
		CAPA(12, true),
		/** Auth command. */
		AUTH(13, false),
		/** Last command. */
		LAST(14, false),
		/** Generic command defined by the string. */
		GENERIC_STRING(15, false);


		/** Is this command multiline. */
		private final boolean multiLine;
		/** Command type. */
		private final int type;

		/**
		 * Constructor.
		 *
		 * @param type
		 *            command type
		 * @param multiLine
		 *            is this multiline command
		 */
		Type(final int type, final boolean multiLine) {
			this.type = type;
			this.multiLine = multiLine;
		}

		/**
		 * Get if this is a multiline command.
		 *
		 * @return is this multiline command
		 */
		public boolean multiLine() {
			return multiLine;
		}
	}

}
