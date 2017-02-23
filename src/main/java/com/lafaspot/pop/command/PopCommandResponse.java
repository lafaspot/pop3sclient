/**
 *
 */
package com.lafaspot.pop.command;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Pop comamnd response object.
 * @author kraman
 *
 */
public class PopCommandResponse {

	/** Response type - ERR or OK. */
	private Type  type;
	/** Is parsing complete for response. */
	private boolean parseComplete = false;
	/** This response is for command. */
	private final PopCommand command;
	/** Response lines. */
	private final List<String> lines = new ArrayList<String>();

	/**
	 * Constructor.
	 * @param command this response is for
	 */
	public PopCommandResponse(@Nonnull final PopCommand command) {
		this.command = command;
		this.parseComplete = !command.getType().multiLine();
	}

	/**
	 * Return if parse is complete.
	 * @return parseComplete
	 */
	public boolean parseComplete() {
		return parseComplete;
	}

	/**
	 * Parse one line of response.
	 * @param line response line
	 */
	public void parse(@Nonnull final String line) {

		if (line.trim().startsWith("-ERR")) {
			parseComplete = true;
			type = Type.ERR;
		} else if (line.trim().startsWith("+OK")) {
			type = Type.OK;
		} else if (line.trim().equals(".")) {
			parseComplete = true;
		}
		lines.add(line);
	}

	/**
	 * Is this response OK?
	 * @return was the response OK
	 */
	public boolean isOk() {
		return Type.OK.equals(type);
	}

	/**
	 * Get response lines.
	 * @return response lines
	 */
	public List<String> getLines() {
		return lines;
	}

	/**
	 * Return the command this response is for.
	 * @return command
	 */
	public PopCommand getCommand() {
		return command;
	}

	/**
	 * Return the String equivalent of the response.
	 * 
	 * @return the string value of response
	 */
	@Override
	public String toString() {
		final StringBuffer buf = new StringBuffer();
		buf.append(type);
		if (null != command) {
			buf.append(command.toString());
		}

		if (null != lines && lines.size() > 0) {
			buf.append(lines);
		}
		return buf.toString();
	}

	/**
	 * Response type, OK or ERR.
	 * @author kraman
	 *
	 */
	public enum Type {
		/** Response OK. */
		OK,
		/** Response ERR. */
		ERR
	}

}
