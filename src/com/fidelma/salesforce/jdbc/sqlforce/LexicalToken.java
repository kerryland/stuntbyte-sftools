package com.fidelma.salesforce.jdbc.sqlforce;

/*******************************************************************************
 * Copyright (c) 2010 Gregory Smith (gsmithfarmer@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gregory Smith (gsmithfarmer@gmail.com) - initial API and implementation
 ******************************************************************************/
/**
 *
 */

/**
 * Representation of a lexical token.
 *
 *
 * @author snort
 *
 */
public class LexicalToken {

	/**
	 * All types of tokens recognized by the lexical analyzer.
	 *
	 */
	public enum Type {
		STRING,
		NUMBER,
		IDENTIFIER,
		PUNCTUATION,
		END_OF_LINE,
		DATE,
		DATETIME
	}

	private Type tokenType;
	private String value;

	/**
	 * Create a new lexical token.
	 *
	 * @param type type of token
	 * @param value from the expression (for strings, quotes should be removed).
	 */
	public LexicalToken( Type type, String value ) {
		this.tokenType = type;
		this.value = value;
	}

	/**
	 * Return the token type.
	 *
	 * @return token type.
	 */
	public Type getType() { return tokenType; }

	/**
	 * Return the value of the token.
	 *
	 * @return token value (from the expression).
	 */
	public String getValue() { return value; }


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return tokenType.toString() + "[" + value + "]";
	}


}
