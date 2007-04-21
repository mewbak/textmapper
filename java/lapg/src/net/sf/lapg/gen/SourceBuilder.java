package net.sf.lapg.gen;

import java.io.InputStream;

import net.sf.lapg.lalr.IError;
import net.sf.lapg.lalr.Syntax;
import net.sf.lapg.syntax.SyntaxUtils;

public class SourceBuilder implements IError {
	
	private static final TargetLanguage[] allLanguages = new TargetLanguage[] {
		new TargetLanguage("java", "", "Parser.java")
	};

	private String myLanguage;
	private int debuglev;
	private int input_sym;

	public SourceBuilder(String lang, int debug) {
		this.myLanguage = lang;
		this.debuglev = debug;
	}

	public boolean process(String sourceName, InputStream input) {
		Syntax s = SyntaxUtils.parseSyntax(sourceName, input, this);
		
		return true;
	}

	public void error(int code, String error) {
		if (code <= debuglev)
			System.out.print(error);
	}
}