package org.textmapper.tool.bootstrap.set;

import java.io.IOException;
import java.text.MessageFormat;
import org.textmapper.tool.bootstrap.set.SetLexer.ErrorReporter;
import org.textmapper.tool.bootstrap.set.SetLexer.Span;
import org.textmapper.tool.bootstrap.set.SetLexer.Tokens;

public class SetParser {

	public static class ParseException extends Exception {
		private static final long serialVersionUID = 1L;

		public ParseException() {
		}
	}

	private final ErrorReporter reporter;

	public SetParser(ErrorReporter reporter) {
		this.reporter = reporter;
	}

	private static final boolean DEBUG_SYNTAX = false;
	private static final int[] tmAction = SetLexer.unpack_int(28,
		"\uffff\uffff\5\0\6\0\7\0\ufffd\uffff\1\0\uffff\uffff\0\0\10\0\11\0\12\0\13\0\14\0" +
		"\15\0\uffff\uffff\4\0\uffff\uffff\21\0\22\0\uffff\uffff\3\0\23\0\24\0\25\0\26\0\27" +
		"\0\uffff\uffff\ufffe\uffff");

	private static final short[] tmLalr = SetLexer.unpack_short(10,
		"\1\uffff\3\uffff\5\uffff\0\2\uffff\ufffe");

	private static final short[] lapg_sym_goto = SetLexer.unpack_short(23,
		"\0\1\5\10\13\14\17\20\20\22\24\25\26\30\31\33\34\34\35\36\37\37\37");

	private static final short[] lapg_sym_from = SetLexer.unpack_short(31,
		"\32\0\4\6\23\6\20\23\0\4\6\6\0\4\6\6\16\23\20\23\0\0\0\4\16\0\4\6\20\23\20");

	private static final short[] lapg_sym_to = SetLexer.unpack_short(31,
		"\33\1\1\10\25\11\21\26\2\2\12\13\3\3\14\15\17\27\22\30\4\32\5\7\20\6\6\16\23\31\24");

	private static final short[] tmRuleLen = SetLexer.unpack_short(27,
		"\2\1\1\4\1\1\1\1\1\1\1\1\1\1\2\2\2\1\1\1\1\1\1\2\2\1\4");

	private static final short[] tmRuleSymbol = SetLexer.unpack_short(27,
		"\12\12\13\14\15\16\16\16\17\17\17\17\17\17\20\20\20\21\21\22\22\22\22\23\24\24\25");

	protected static final String[] tmSymbolNames = new String[] {
		"eoi",
		"'a'",
		"'b'",
		"'c'",
		"'d'",
		"'e'",
		"'f'",
		"'g'",
		"'h'",
		"'i'",
		"abcdef_list",
		"input",
		"abcdef",
		"setof_'h'",
		"setof_first_pair",
		"setof_pair",
		"pair",
		"setof_first_recursive",
		"setof_recursive",
		"test2",
		"recursive",
		"helper",
	};

	public interface Nonterminals extends Tokens {
		// non-terminals
		int abcdef_list = 10;
		int input = 11;
		int abcdef = 12;
		int setof_ApostrophehApostrophe = 13;
		int setof_first_pair = 14;
		int setof_pair = 15;
		int pair = 16;
		int setof_first_recursive = 17;
		int setof_recursive = 18;
		int test2 = 19;
		int recursive = 20;
		int helper = 21;
	}

	/**
	 * -3-n   Lookahead (state id)
	 * -2     Error
	 * -1     Shift
	 * 0..n   Reduce (rule index)
	 */
	protected static int tmAction(int state, int symbol) {
		int p;
		if (tmAction[state] < -2) {
			for (p = -tmAction[state] - 3; tmLalr[p] >= 0; p += 2) {
				if (tmLalr[p] == symbol) {
					break;
				}
			}
			return tmLalr[p + 1];
		}
		return tmAction[state];
	}

	protected static int tmGoto(int state, int symbol) {
		int min = lapg_sym_goto[symbol], max = lapg_sym_goto[symbol + 1] - 1;
		int i, e;

		while (min <= max) {
			e = (min + max) >> 1;
			i = lapg_sym_from[e];
			if (i == state) {
				return lapg_sym_to[e];
			} else if (i < state) {
				min = e + 1;
			} else {
				max = e - 1;
			}
		}
		return -1;
	}

	protected int tmHead;
	protected Span[] tmStack;
	protected Span tmNext;
	protected SetLexer tmLexer;

	public Object parse(SetLexer lexer) throws IOException, ParseException {

		tmLexer = lexer;
		tmStack = new Span[1024];
		tmHead = 0;

		tmStack[0] = new Span();
		tmStack[0].state = 0;
		tmNext = tmLexer.next();

		while (tmStack[tmHead].state != 27) {
			int action = tmAction(tmStack[tmHead].state, tmNext.symbol);

			if (action >= 0) {
				reduce(action);
			} else if (action == -1) {
				shift();
			}

			if (action == -2 || tmStack[tmHead].state == -1) {
				break;
			}
		}

		if (tmStack[tmHead].state != 27) {
			reporter.error(MessageFormat.format("syntax error before line {0}",
								tmLexer.getTokenLine()), tmNext.line, tmNext.offset, tmNext.endoffset);
			throw new ParseException();
		}
		return tmStack[tmHead - 1].value;
	}

	protected void shift() throws IOException {
		tmStack[++tmHead] = tmNext;
		tmStack[tmHead].state = tmGoto(tmStack[tmHead - 1].state, tmNext.symbol);
		if (DEBUG_SYNTAX) {
			System.out.println(MessageFormat.format("shift: {0} ({1})", tmSymbolNames[tmNext.symbol], tmLexer.tokenText()));
		}
		if (tmStack[tmHead].state != -1 && tmNext.symbol != 0) {
			tmNext = tmLexer.next();
		}
	}

	protected void reduce(int rule) {
		Span left = new Span();
		left.value = (tmRuleLen[rule] != 0) ? tmStack[tmHead + 1 - tmRuleLen[rule]].value : null;
		left.symbol = tmRuleSymbol[rule];
		left.state = 0;
		if (DEBUG_SYNTAX) {
			System.out.println("reduce to " + tmSymbolNames[tmRuleSymbol[rule]]);
		}
		Span startsym = (tmRuleLen[rule] != 0) ? tmStack[tmHead + 1 - tmRuleLen[rule]] : tmNext;
		left.line = startsym.line;
		left.offset = startsym.offset;
		left.endoffset = (tmRuleLen[rule] != 0) ? tmStack[tmHead].endoffset : tmNext.offset;
		applyRule(left, rule, tmRuleLen[rule]);
		for (int e = tmRuleLen[rule]; e > 0; e--) {
			tmStack[tmHead--] = null;
		}
		tmStack[++tmHead] = left;
		tmStack[tmHead].state = tmGoto(tmStack[tmHead - 1].state, left.symbol);
	}

	@SuppressWarnings("unchecked")
	protected void applyRule(Span tmLeft, int ruleIndex, int ruleLength) {
	}
}