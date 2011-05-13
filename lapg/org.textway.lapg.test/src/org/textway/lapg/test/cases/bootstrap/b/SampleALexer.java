package org.textway.lapg.test.cases.bootstrap.b;

import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class SampleALexer {

	public static class LapgSymbol {
		public Object sym;
		public int lexem;
		public int state;
		public int offset;
		public int endoffset;
	}

	public interface Lexems {
		public static final int eoi = 0;
		public static final int identifier = 1;
		public static final int _skip = 2;
		public static final int Lclass = 3;
		public static final int LCURLY = 4;
		public static final int RCURLY = 5;
		public static final int Linterface = 6;
		public static final int Lenum = 7;
		public static final int error = 8;
		public static final int numeric = 9;
		public static final int octal = 10;
		public static final int decimal = 11;
		public static final int eleven = 12;
	}

	public interface ErrorReporter {
		void error(int start, int end, int line, String s);
	}

	public static final int TOKEN_SIZE = 2048;

	private Reader stream;
	final private ErrorReporter reporter;

	final private char[] data = new char[2048];
	private int datalen, l;
	private char chr;

	private int group;

	final private StringBuilder token = new StringBuilder(TOKEN_SIZE);

	private int tokenLine = 1;
	private int currLine = 1;
	private int currOffset = 0;
	

	public SampleALexer(Reader stream, ErrorReporter reporter) throws IOException {
		this.reporter = reporter;
		reset(stream);
	}

	public void reset(Reader stream) throws IOException {
		this.stream = stream;
		this.datalen = stream.read(data);
		this.l = 0;
		this.group = 0;
		chr = l < datalen ? data[l++] : 0;
	}

	public int getState() {
		return group;
	}

	public void setState(int state) {
		this.group = state;
	}

	public int getTokenLine() {
		return tokenLine;
	}

	public int getLine() {
		return currLine;
	}

	public void setLine(int currLine) {
		this.currLine = currLine;
	}

	public int getOffset() {
		return currOffset;
	}

	public void setOffset(int currOffset) {
		this.currOffset = currOffset;
	}

	public String current() {
		return token.toString();
	}

	private static final short lapg_char2no[] = {
		0, 1, 1, 1, 1, 1, 1, 1, 1, 8, 8, 1, 1, 8, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		4, 10, 10, 10, 10, 10, 10, 10, 7, 7, 1, 1, 1, 1, 1, 1,
		1, 9, 9, 9, 9, 9, 9, 6, 6, 6, 6, 6, 6, 6, 6, 6,
		6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 1, 1, 1, 1, 6,
		1, 9, 9, 9, 9, 9, 9, 6, 6, 6, 6, 6, 6, 6, 6, 6,
		6, 6, 6, 6, 6, 6, 6, 6, 5, 6, 6, 2, 1, 3, 1, 1
	};

	private static final short lapg_lexemnum[] = {
		1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12
	};

	private static final short[][] lapg_lexem = new short[][] {
		{ -2, -1, 1, 2, 3, 4, 4, 5, 6, 4, 5},
		{ -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6},
		{ -7, -7, -7, -7, -7, -7, -7, -7, -7, -7, -7},
		{ -1, -1, -1, -1, 7, 8, -1, -1, -1, -1, 7},
		{ -3, -3, -3, -3, 4, 4, 4, 4, -3, 4, 4},
		{ -1, -1, -1, -1, 9, -1, -1, 9, -1, -1, 9},
		{ -4, -4, -4, -4, -4, -4, -4, -4, 6, -4, -4},
		{ -11, -11, -11, -11, 7, -11, -11, -11, -11, -11, 7},
		{ -1, -1, -1, -1, 10, -1, -1, 10, -1, 10, 10},
		{ -12, -12, -12, -12, 9, -12, -12, 9, -12, -12, 9},
		{ -10, -10, -10, -10, 10, -10, -10, 10, -10, 10, 10}
	};

	private static int mapCharacter(int chr) {
		if (chr >= 0 && chr < 128) {
			return lapg_char2no[chr];
		}
		return 1;
	}

	public LapgSymbol next() throws IOException {
		LapgSymbol lapg_n = new LapgSymbol();
		int state;

		do {
			lapg_n.offset = currOffset;
			tokenLine = currLine;
			if (token.length() > TOKEN_SIZE) {
				token.setLength(TOKEN_SIZE);
				token.trimToSize();
			}
			token.setLength(0);
			int tokenStart = l - 1;

			for (state = group; state >= 0;) {
				state = lapg_lexem[state][mapCharacter(chr)];
				if (state == -1 && chr == 0) {
					lapg_n.endoffset = currOffset;
					lapg_n.lexem = 0;
					lapg_n.sym = null;
					reporter.error(lapg_n.offset, lapg_n.endoffset, this.getTokenLine(), "Unexpected end of input reached");
					return lapg_n;
				}
				if (state >= -1 && chr != 0) {
					currOffset++;
					if (chr == '\n') {
						currLine++;
					}
					if (l >= datalen) {
						token.append(data, tokenStart, l - tokenStart);
						datalen = stream.read(data);
						tokenStart = l = 0;
					}
					chr = l < datalen ? data[l++] : 0;
				}
			}
			lapg_n.endoffset = currOffset;

			if (state == -1) {
				if (l - 1 > tokenStart) {
					token.append(data, tokenStart, l - 1 - tokenStart);
				}
				reporter.error(lapg_n.offset, lapg_n.endoffset, this.getTokenLine(), MessageFormat.format("invalid lexem at line {0}: `{1}`, skipped", currLine, current()));
				lapg_n.lexem = -1;
				continue;
			}

			if (state == -2) {
				lapg_n.lexem = 0;
				lapg_n.sym = null;
				return lapg_n;
			}

			if (l - 1 > tokenStart) {
				token.append(data, tokenStart, l - 1 - tokenStart);
			}

			lapg_n.lexem = lapg_lexemnum[-state - 3];
			lapg_n.sym = null;

		} while (lapg_n.lexem == -1 || !createToken(lapg_n, -state - 3));
		return lapg_n;
	}

	protected boolean createToken(LapgSymbol lapg_n, int lexemIndex) {
		switch (lexemIndex) {
			case 0:
				return createIdentifierToken(lapg_n, lexemIndex);
			case 1:
				 return false; 
			case 7:
				return createNumericToken(lapg_n, lexemIndex);
			case 8:
				return createOctalToken(lapg_n, lexemIndex);
			case 9:
				return createDecimalToken(lapg_n, lexemIndex);
		}
		return true;
	}

	private static Map<String,Integer> subTokensOfIdentifier = new HashMap<String,Integer>();
	static {
		subTokensOfIdentifier.put("class", 2);
		subTokensOfIdentifier.put("interface", 5);
		subTokensOfIdentifier.put("enum", 6);
	}

	protected boolean createIdentifierToken(LapgSymbol lapg_n, int lexemIndex) {
		Integer replacement = subTokensOfIdentifier.get(current());
		if(replacement != null) {
			lexemIndex = replacement;
			lapg_n.lexem = lapg_lexemnum[lexemIndex];
		}
		switch(lexemIndex) {
			case 2: // class
				 lapg_n.sym = "class"; break; 
			case 5: // interface
				 lapg_n.sym = "interface"; break; 
			case 6: // enum
				 lapg_n.sym = new Object(); break; 
			case 0: // <default>
				 lapg_n.sym = current(); break; 
		}
		return true;
	}

	protected boolean createNumericToken(LapgSymbol lapg_n, int lexemIndex) {
		return true;
	}

	protected boolean createOctalToken(LapgSymbol lapg_n, int lexemIndex) {
		switch(lexemIndex) {
			case 8: // <default>
				 lapg_n.sym = Integer.parseInt(current(), 8); break; 
		}
		return true;
	}

	private static Map<String,Integer> subTokensOfDecimal = new HashMap<String,Integer>();
	static {
		subTokensOfDecimal.put("11", 10);
	}

	protected boolean createDecimalToken(LapgSymbol lapg_n, int lexemIndex) {
		Integer replacement = subTokensOfDecimal.get(current());
		if(replacement != null) {
			lexemIndex = replacement;
			lapg_n.lexem = lapg_lexemnum[lexemIndex];
		}
		switch(lexemIndex) {
			case 10: // 11
				 lapg_n.sym = 11; break; 
		}
		return true;
	}
}
