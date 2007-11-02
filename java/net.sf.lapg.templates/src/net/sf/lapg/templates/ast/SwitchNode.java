package net.sf.lapg.templates.ast;

import java.util.ArrayList;

import net.sf.lapg.templates.api.EvaluationException;
import net.sf.lapg.templates.api.IEvaluationEnvironment;

public class SwitchNode extends Node {

	ArrayList<CaseNode> cases;
	ExpressionNode expression;
	
	public SwitchNode(ExpressionNode expression, ArrayList<CaseNode> cases) {
		this.expression = expression;
		this.cases = cases;
	}

	protected void emit(StringBuffer sb, Object context, IEvaluationEnvironment env) {
		try {
			Object value = env.evaluate(expression, context, false);
			for( CaseNode n : cases ) {
				Object caseValue = env.evaluate(n.getExpression(), context, false);
				if( ConditionalNode.safeEqual(value, caseValue)) {
					n.emit(sb, context, env);
					return;
				}
			}
		} catch( EvaluationException ex ) {
			/* ignore, statement will be skipped */
		}
	}
}