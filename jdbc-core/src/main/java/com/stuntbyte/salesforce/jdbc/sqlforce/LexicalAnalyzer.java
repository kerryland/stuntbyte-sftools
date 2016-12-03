package com.stuntbyte.salesforce.jdbc.sqlforce;

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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * This is the lexical analyzer used to bust an expression into tokens.
 *
 * Its major contribution is to isolate the logic for recognizing quoted strings and identifier names.
 *
 * @author snort
 *
 */
public class LexicalAnalyzer {

	private static final char ESCAPE_CHAR = '\\';
	private static final String QUOTE_CHARS = "\"'";
	private static final char EOL = '\n';
	private String prompt = "SQLForce> ";

	private static Set<String> multiCharPunctuation;
	static {
		multiCharPunctuation = new HashSet<String>();
		multiCharPunctuation.add( "!=");
		multiCharPunctuation.add( "<>");
		multiCharPunctuation.add( ">=");
		multiCharPunctuation.add( "<=");

	}


	private Stack<BufferedReader> readerStack = new Stack<BufferedReader>();
	private Stack<Character> pushbackStack = new Stack<Character>();
	private LexicalToken lastToken = null;
	private PrintStream promptStream;

	public LexicalAnalyzer(InputStream inStream, PrintStream promptStream ) {
		BufferedReader reader = new BufferedReader( new InputStreamReader(inStream));
		this.promptStream = promptStream;

		readerStack.push(reader);

	}

	private Character read() {

		if( !pushbackStack.isEmpty()) {
			return pushbackStack.pop().charValue();
		}
		try {
			while( !readerStack.isEmpty()) {

				BufferedReader reader = readerStack.peek();
				if( null!=promptStream
						&& 1 == readerStack.size()
						&& !reader.ready()
						&& null!=prompt ) {
					promptStream.print(prompt);
					promptStream.flush();
				}
				String inline = readerStack.peek().readLine();
				if( null == inline ) {
					readerStack.pop().close();
					continue;
				}

				unread( inline + EOL );
				return read();
			}
		} catch( IOException e ) {
			return null;
		}
		return null;

	}

	private Character peek() {
		Character cc = read();
		if( null != cc ) { unread(cc); }

		return cc;
	}

	private void unread( char value ) {
		pushbackStack.push( new Character(value));
	}

	private void unread(String text ) {
		char cList[] = text.toCharArray();
		for( int n = cList.length; n-- > 0; ) {
			pushbackStack.push(cList[n]);
		}
	}
	/**
	 * Eat all leading whitespace from the input stream.
	 *
	 */
	private void eatWhiteSpace() {
		eatWhiteSpace(false);

	}

	/**
	 * Eat all leading whitespace from the input stream.
	 *
	 * @param eatEOL if true theh consider end of line white space.
	 */
	private void eatWhiteSpace( boolean eatEOL ) {
		Character cc;
		while( null != (cc = read())) {
			if( (cc.equals(EOL) && !eatEOL) || !Character.isWhitespace(cc)) {
				unread(cc);
				break;
			}
		}
	}
	/**
	 * Create a new lexical token AND record the fact that it has been created.
	 *
	 * ALL lexical token creation should go this routine so users of this package
	 * can recall the most recent token scanned.
	 *
	 * @param tokenType create a token of this type.
	 * @param value of the token
	 * @return a new lexical token.
	 */
	private LexicalToken createToken( LexicalToken.Type tokenType, String value ) {
		LexicalToken token = new LexicalToken( tokenType, value );

		lastToken = token;

		return token;
	}
	/**
	 * Recognize a quoted string including escaped characters.
	 *
	 * If we run out of characters before finding the closing quote, pretend it was there.
	 *
	 * @return resultant string (can be zero length).
	 */
	private String parseQuotedString() {

		Character quote = read();
		StringBuffer answer = new StringBuffer();
		boolean lastWasEscape = false;

		while( true ) {
			Character c = read();
			if( null == c ) { break; } // Out of characters

			if( c.equals(quote) && !lastWasEscape ) { break; } // Got the complete string

			answer.append(c);

			boolean isEscape = c.equals(ESCAPE_CHAR );
			lastWasEscape = lastWasEscape?false:isEscape;
		}

		return answer.toString();
	}

	/**
	 * Read a sequence of digits.
	 *
	 * @return sequential digits found.
	 */
	private String parseDigits( ) {
		StringBuffer answer = new StringBuffer();
		while( true ) {
			Character c = read();
			if( null==c ) { break; }
			if( !Character.isDigit(c)) { unread(c); break; }
			answer.append( c );
		}

		return answer.toString();
	}

	private String preZeroPad( String value, int nDigits ) {
		int n2Pad = nDigits - value.length();
		StringBuffer b = new StringBuffer();
		while( n2Pad > 0 ) {
			b.append("0");
			n2Pad--;
		}

		return b.toString() + value;
	}
	/**
	 * Scan for a date.
	 *
	 * The parser does not guarantee that a valid Salesforce date is return but based on the
	 * lexical context, we can be pretty sure that is what the user meant. When the corresponding
	 * SOQL statement is executed the truth will be know.
	 *
	 * A bit of sloppiness in the date is allowed (and fixed up) by the scanner. See the code.
	 * @param year The year part of the date (already scanned).
	 *
	 * @return a saleforce date.
	 */
	private String parseDate( String year ) {
		read(); // The first dash.

		String mm = preZeroPad(parseDigits(), 2 );
		read(); // Another Dash;
		String day = preZeroPad(parseDigits(), 2);

		return year + "-" + mm + "-" + day;
	}

	/**
	 * Scan for a dateTime value.
	 *
	 * The parser does not guarantee that a valid Salesforce dateTime is return but based on the
	 * lexical context, we can be pretty sure that is what the user meant. When the corresponding
	 * SOQL statement is executed the truth will be know.
	 *
	 * A bit of sloppiness in the date is allowed (and fixed up) by the scanner. See the code.
	 *
	 * @param yearPlusT the year plus a T (e.g. 2010-05-17T)
	 * @return a salesforce dateTime.
	 */
	private String parseDateTime( String yearPlusT ) {
		String hour = preZeroPad(parseDigits(),2);
		read(); //  :
		String minutes = preZeroPad(parseDigits(), 2);
		read(); //  :
		String seconds = preZeroPad(parseDigits(), 2);

		String baseDT = yearPlusT + hour + ":" + minutes + ":" + seconds;
		Character c = peek();
		if( null == c ) {
			return baseDT + "Z";
		}

		if( c.equals('.')) {
			baseDT += read() + parseDigits();
			c = peek();
		}

		if( c.equals('-') || c.equals('+')) {
			String dt = baseDT + read()
						+ preZeroPad(parseDigits(),2)
						+ read() + preZeroPad(parseDigits(), 2);
			return dt;
		}


		if( c.equals('Z') || c.equals('z')) {
			read();
			return baseDT + "Z";
		} else {
			return baseDT + "Z";
		}
	}
	/**
	 * Scan for a java style identifier name.
	 *
	 * When called, we are guarenteed that the first character on the stack is legal
	 * java identifier.
	 * <p>
	 * An identifier can is defined as one of the following:
	 * <ul>
	 * <li>javaIdentifier
	 * <li>javaIdentifier.javaIdentifier
	 * </ul>
	 *
	 * @return identifier name.
	 */
	private String parseIdentifier() {
		StringBuffer answer = new StringBuffer();
		Character c = read();

		answer.append(c);
		while( null != (c = read())) {

			if( c!='.' && !Character.isJavaIdentifierPart(c)) {
				unread(c);
				break;

			}

			answer.append(c);

		}

		return answer.toString();
	}

	/**
	 * Read all tokens up to the next whitespace character.
	 *
	 * When called, I am guarenteed that the first character I will
	 * read is a non-blank character.
	 *
	 * @return a non-null sequence of characters.
	 */
	private String parseNonBlank() {
		StringBuffer answer = new StringBuffer();
		Character c;

		answer.append(read());
		while( null != (c = read())) {

			if( Character.isWhitespace(c)) {
				unread(c);
				break;

			}

			answer.append(c);

		}

		return answer.toString();
	}
	/**
	 * Recognize punctuation character(s).
	 *
	 * When called, we are guaranteed that the first character on the stack is legal
	 * punctuation.
	 *
	 * @return punctuation character (s).
	 */
	private String parsePunctuation() {
		StringBuffer answer = new StringBuffer();
		Character c = read();

		answer.append(c);
		while( null != (c = peek())) {
			if( !multiCharPunctuation.contains(answer.toString() + c)) {
				break;
			}
			answer.append( read());
		}

		return answer.toString();
	}

	/**
	 * Recognize a number.
	 *
	 * When called, we are guaranteed that the first character on the stack is a digit OR the first two
	 * characters are .Digit
	 *
	 * @return parsed number.
	 */
	private String parseNumber() {
		StringBuffer answer = new StringBuffer();
		Character c = read();
		boolean havePeriod = false;

		answer.append(c);
		if( '.' == c ) {
			havePeriod = true;
		}

		while( null != (c=read())) {

			if( Character.isDigit(c)) {
				answer.append(c);
				continue;
			}

			if( !havePeriod && '.' == c ) {
				answer.append(c);
				havePeriod = true;
				continue;
			}

			unread(c);
			break;
		}
		return answer.toString();
	}


	public LexicalToken getToken() { return getToken( false ); }

	/**
	 * Return the next lexical token in the stream -- null if there are no more tokens.
	 *
	 * @return next token or null if there is no more data.
	 */
	public LexicalToken getToken( boolean ignoreEOL ) {
        ignoreEOL = true;

		eatWhiteSpace();
		Character firstC = peek();
		if( null == firstC ) { return null; } // Out of data.

		//
		// Look for a quoted string.
		//

		if( QUOTE_CHARS.contains(firstC.toString())) {

			return createToken( LexicalToken.Type.STRING, parseQuotedString());
		}

		//
		// Look for an identifier.
		//
		//
		if( Character.isJavaIdentifierStart(firstC)) {

			return createToken( LexicalToken.Type.IDENTIFIER, parseIdentifier());
		}

		//
		// Look for a number using java built in parser to recognized a variety of number formats.
		//
		if( Character.isDigit(firstC)) {
			String maybeNumber = parseNumber();
			Character c = peek();
			if( 4!=maybeNumber.length() || c!='-') {
				return createToken( LexicalToken.Type.NUMBER, maybeNumber);
			}

			String date = parseDate( maybeNumber );

			if( 'T' != peek() && 't' != peek()) {
				return createToken( LexicalToken.Type.DATE, date );
			}
			read();
			String dateTime = parseDateTime( date + "T");
			return createToken( LexicalToken.Type.DATETIME, dateTime );

		} else if( '.'== firstC  ){
			Character period = read();
			Character digit = peek();
			unread(period);

			if( Character.isDigit(digit)) {
				return createToken( LexicalToken.Type.NUMBER,  parseNumber());
			}
		}
		//
		// If found something that must be punctuation.
		//
		// Recognize these here.
		//

		firstC = read();


		if( firstC.equals(ESCAPE_CHAR ) ) {

			if( peek().equals(EOL)) {
				read();
				return getToken(ignoreEOL);
			}
		}

		if( firstC.equals( EOL )) {
			return ignoreEOL?getToken(ignoreEOL):createToken(LexicalToken.Type.END_OF_LINE, "\n");
		} else {
			unread(firstC );
			return createToken(LexicalToken.Type.PUNCTUATION, parsePunctuation());
		}

	}

	/**
	 * Read the next token up to the next whitespace character.
	 *
	 * @return found token or null if out of data.
	 * @throws Exception a read fails or non blank token not found.
	 */
	public LexicalToken getNonBlankToken( ) throws Exception {
		eatWhiteSpace();
		Character firstC = peek();
		if( null == firstC ) { return null; } // Out of data.
		if( firstC.equals(EOL)) {
			read();
			return null;
		}

		return createToken( LexicalToken.Type.STRING, parseNonBlank());
	}

	/**
	 * Read a token that is composed entirely of letters and digits (a salesforce id).
	 *
	 * @param ignoreEOL
	 * @return
	 * @throws Exception
	 */
	public LexicalToken getLetterDigitToken(boolean ignoreEOL) throws Exception {
		eatWhiteSpace(ignoreEOL);
		Character firstC = peek();
		StringBuffer value = new StringBuffer();
		while( null!=firstC && Character.isLetterOrDigit(firstC) ) {
			value.append( read());
			firstC = peek();
		}

		if( 0==value.length()) {
			throw new Exception("Expected LetterDigit token");
		}
		return createToken( LexicalToken.Type.STRING, value.toString());

	}
	public LexicalToken getToken( boolean ignoreEOL, LexicalToken.Type expectedTokenType ) throws Exception {
		LexicalToken token = getToken(ignoreEOL);

		if( null == token ) {
			throw new Exception("Expected " + expectedTokenType + " but found end of file");
		}

		if( token.getType() != expectedTokenType ) {
			throw new Exception("Expected " + expectedTokenType + " but found " + token.getType());
		}

		return token;
	}

	public LexicalToken getToken( LexicalToken.Type expectedTokenType ) throws Exception {
		return getToken(false,  expectedTokenType );

	}
	public LexicalToken getToken( boolean ignoreEOL, String expectedValue ) throws Exception {
		LexicalToken token = getToken(ignoreEOL);

		if( null == token ) {
			throw new Exception("Expected '" + expectedValue + "' but found end of file");
		}

		if( !token.getValue().equalsIgnoreCase(expectedValue) ) {
			throw new Exception("Expected '" + expectedValue + "' but found '" + token.getValue() + "'");
		}

		return token;
	}

	public LexicalToken getToken( String expectedValue ) throws Exception {
		return getToken(false, expectedValue);


	}

	public LexicalToken getToken( boolean ignoreEOL, String[] expectedValues ) throws Exception {
		LexicalToken token = getToken(ignoreEOL);
		StringBuffer expectedValueString = new StringBuffer();
		Set<String> expectedValueSet = new HashSet<String>();

		for( String v : expectedValues ) {
			expectedValueString.append( (expectedValueSet.isEmpty()?"[":",") + v);
			expectedValueSet.add( v.toUpperCase());
		}

		if( null == token ) {
			throw new Exception("Expected '" + expectedValueString + "' but found end of file");
		}

		if( !expectedValueSet.contains(token.getValue().toUpperCase()) ) {
			throw new Exception("Expected '" + expectedValueString + "' but found '" + token.getValue() + "'");
		}

		return token;
	}

	public LexicalToken getToken( String[] expectedValues ) throws Exception {
		return getToken( false, expectedValues );
	}

	public LexicalToken getKeyword( boolean ignoreEOL, String expectedKeyword ) throws Exception {
		LexicalToken token = getToken( ignoreEOL);

		if( null == token ) {
			throw new Exception("Expected Keyword " + expectedKeyword + " but found end of file");
		}

		if( token.getType() != LexicalToken.Type.IDENTIFIER ) {
			throw new Exception("Expected Keyword '" + expectedKeyword + "' but found '" + token.getValue() + "'");
		}
		if( !expectedKeyword.equalsIgnoreCase(token.getValue())) {
			throw new Exception("Expected keyword '" + expectedKeyword + "' but found '" + token.getValue() + "'");
		}

		return token;
	}

	public LexicalToken getKeyword( String expectedKeyword ) throws Exception {
		return getKeyword( false, expectedKeyword );

	}
	/**
	 * Return all remaining data on the line up to the end of line character (do not
	 * include the EOL character.
	 *
	 * @return
	 */
	public LexicalToken readLine() {
		StringBuffer answer = new StringBuffer();
		boolean readOne = false;
		Character cc;
		while( null != (cc=read())) {
			readOne = true;
			if( cc.equals(ESCAPE_CHAR ) ) {

				if( peek().equals(EOL)) {
					cc = read();
					answer.append(cc);
					continue;
				}
			}

			if( cc.equals(EOL)) { break; }
			answer.append(cc);
		}

		if( !readOne ) { return null; }
		return createToken( LexicalToken.Type.STRING, answer.toString());
	}

	/**
	 * Return the most recent token returned by the lexical analyzer.
	 *
	 * @return most recent token found.
	 */
	public LexicalToken getLastToken() {
		return lastToken;
	}

	/**
	 * Consume tokens until a specific token value is found.
	 * This is useful for consuming a line if an error is found mid-stream while parsing.
	 *
	 * @param value consume tokens until this value is found. Consume nothing if the most recent token was this value.
	 */
	public void consume( String value ) {
		if( null!=lastToken && value.equals(lastToken.getValue())) { return; } // already consumed.

		for( LexicalToken var = getToken(); null!=var; var = getToken()) {
			if( value.equals( var.getValue())) {
				break;
			}
		}
	}

	/**
	 * Push the current input stream and start reading from another stream (until EOF).
	 *
	 * @param inStream start reading from this stream.
	 */
	public void include( InputStream inStream ) {
		BufferedReader reader = new BufferedReader( new InputStreamReader(inStream));
		readerStack.push(reader);
	}

	/**
	 * Push a command line onto the reader stack.
	 *
	 * @param command push this command.
	 */
	public void include( String command ) {
		ByteArrayInputStream bIn = new ByteArrayInputStream( command.getBytes());
		include( bIn );
	}
	public void setPrompt(String prompt ) {
		this.prompt = prompt;
		if( null != prompt && 0==prompt.trim().length()) { this.prompt=null; }
	}

	/**
	 * Return the current prompt string used by the lexical analyzer.
	 *
	 * @return prompt string (may be null).
	 */
	public String getPrompt() {
		return prompt;
	}
	/**
	 * Return a value with any quotes removed (does nothing if no quotes).
	 *
	 * @param src remove quotes from this string.
	 * @return src with quotes removed.
	 */
	public String stripQuotes( String src ) {

		if( null == src ) { return src; }
		if( src.length() < 2 ) { return src; }

		String quote = src.substring( 0, 1 );
		if( QUOTE_CHARS.contains(quote) && src.endsWith(quote)) {
			return src.substring( 1, src.length() - 1);
		}
		return src;
	}

    public String unparsedString() {
        StringBuilder sb = new StringBuilder();
        Character ch = read();
        while (ch != null) {
            sb.append(ch);
            ch = read();
        }

        return sb.toString();
    }
}

