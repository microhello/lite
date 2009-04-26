package org.xidea.lite.plugin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.ParseContext;

public class RhinoInstructionParserProxy implements InstructionParser {

	private static final Log log = LogFactory
			.getLog(RhinoInstructionParserProxy.class);
	private Scriptable base;
	private Function findStart;
	private Function parse;

	public RhinoInstructionParserProxy(Context context, Scriptable base,
			Function findStart, Function parse) {
		this.base = base;
		this.parse = parse;
		this.findStart = findStart;
	}

	@Override
	public int findStart(ParseContext context, String text, int start) {
		try {
			Object[] args = new Object[] { context, text, start };
			Number value = (Number) RhinoContext.call(base, findStart, args);
			return value.intValue();
		} catch (RuntimeException e) {
			if (log.isDebugEnabled()) {
				log.debug("error in:"
						+ RhinoContext.call(findStart, (Function) RhinoContext
								.getProperty(findStart, "toString"),
								new Object[0]), e);
			}
			return -1;
		}
	}

	@Override
	public int parse(ParseContext context, String text, int start) {
		try {
			Object[] args = new Object[] { context, text, start };
			Number value = (Number) RhinoContext.call(base, parse, args);
			return value.intValue();
		} catch (RuntimeException e) {
			if (log.isDebugEnabled()) {
				log.debug("error in:"
						+ RhinoContext.call(parse, (Function) RhinoContext
								.getProperty(parse, "toString"),
								new Object[0]), e);
			}
			return -1;
		}
	}

}
