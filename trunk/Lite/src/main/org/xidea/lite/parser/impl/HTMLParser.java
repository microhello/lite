package org.xidea.lite.parser.impl;

import static org.xidea.lite.parser.ResultContext.END_INSTRUCTION;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.operation.TextContains;
import org.xidea.el.operation.TextNullEmpty;
import org.xidea.lite.Template;
import org.xidea.lite.parser.Parser;
import org.xidea.lite.parser.ParseContext;

/**
 * 类似这种特别的编译模块(有扩展,可能导致其他运行环境无法解释)，最好带上双重开关。
 * 
 * @author jindw
 */
public class HTMLParser extends AbstractHTMLParser implements Parser<Element>{
	public static final String AUTO_FORM_FEATRUE_URL = "http://www.xidea.org/ns/lite/autoform";
	public static final String NO_AUTO = "none";
	public static final String AUTO_ANYWAY = "anyway";
	public static final String AUTO_IN_FORM = "form";

	private static final String FORM_TAG = "form";
	private static final String INPUT_TAG = "input";
	private static final String TEXTAREA_TAG = "textarea";
	private static final String SELECT_TAG = "select";
	private static final String OPTION_TAG = "option";

	private static final String ATTRIBUTE_SELECTED = "selected";
	private static final String ATTRIBUTE_CHECKED = "checked";
	private static final String ATTRIBUTE_TYPE = "type";
	private static final String ATTRIBUTE_NAME = "name";
	private static final String ATTRIBUTE_VALUE = "value";

	private static final Pattern TYPE_CHECK_RADIO = Pattern
			.compile("^(?:checkbox|radio)$");
	private static final Pattern TYPE_BUTTON = Pattern
			.compile("^(?:reset|button|submit)$");
	private static final Object KEY_SELECT = new Object();

	public HTMLParser() {
		super();
	}

	protected void parse(Node node, ParseContext context) {
		Element el = (Element) node;
		String localName = el.getLocalName();
		Object status = context.getFeatrue(AUTO_FORM_FEATRUE_URL);
		if (AUTO_ANYWAY.equals(status)) {
			processAutoForm(context, el, localName);
		} else if (AUTO_IN_FORM.equals(status) && FORM_TAG.equals(localName)) {
			context.setFeatrue(AUTO_FORM_FEATRUE_URL, AUTO_ANYWAY);
			parseHTMLElement(el, context, null);
			context.setFeatrue(AUTO_FORM_FEATRUE_URL, AUTO_ANYWAY);
		} else {
			parseHTMLElement(el, context, null);
		}
	}

	private void processAutoForm(ParseContext context, Element el,
			String localName) {
		if (INPUT_TAG.equals(localName)) {
			parseInput(el, context);
		} else if (TEXTAREA_TAG.equals(localName)) {
			parseTextArea(el, context);
		} else if (SELECT_TAG.equals(localName)) {
			parseSelect(el, context);
		} else if (OPTION_TAG.equals(localName)) {
			parseSelectOption(el, context);
		} else {
			parseHTMLElement(el, context, null);
		}
	}

	protected void parseSelect(Element el, ParseContext context) {
		context.setAttribute(KEY_SELECT, el);
		parseHTMLElement(el, context, null);
	}

	protected void parseInput(Element element, ParseContext context) {
		element = (Element) element.cloneNode(true);// options 有值，textarea有值
		String type = element.getAttribute(ATTRIBUTE_TYPE);
		List<Object> attributes = null;
		if (TYPE_CHECK_RADIO.matcher(type).find()) {
			if (!element.hasAttribute(ATTRIBUTE_CHECKED)
					&& element.hasAttribute(ATTRIBUTE_NAME)
					&& element.hasAttribute(ATTRIBUTE_VALUE)) {
				String name = element.getAttribute(ATTRIBUTE_NAME);
				String value = element.getAttribute(ATTRIBUTE_VALUE);
				attributes = buildCheckedAttribute(context, name, value,
						ATTRIBUTE_CHECKED);
			}
		} else if (!TYPE_BUTTON.matcher(type).find()) {
			if (!element.hasAttribute(ATTRIBUTE_VALUE)
					&& element.hasAttribute(ATTRIBUTE_NAME)) {
				element.setAttribute(ATTRIBUTE_VALUE, "${"
						+ element.getAttribute(ATTRIBUTE_NAME) + "}");
			}
		}
		parseHTMLElement(element, context, attributes);
	}

	protected void parseTextArea(Element el, ParseContext context) {
		Document document = el.getOwnerDocument();
		if (el.hasAttribute(ATTRIBUTE_VALUE)) {
			el = (Element) el.cloneNode(false);
			String value = el.getAttribute(ATTRIBUTE_VALUE);
			el.removeAttribute(ATTRIBUTE_VALUE);
			el.appendChild(document.createTextNode(value));
		} else {
			el = (Element) el.cloneNode(true);
			Node child = el.getFirstChild();
			String name = el.getAttribute(ATTRIBUTE_NAME);
			if (child == null) {
				if (name.length() > 0) {
					el.appendChild(document.createTextNode(buildNullEmptyEL(
							context, name)));
				}
			} else if (child.getNextSibling() == null) {
				if (child instanceof Text) {
					String value = ((Text) child).getData().trim();
					if (value.length() == 0 && name.length() > 0) {
						el
								.appendChild(document
										.createTextNode(buildNullEmptyEL(
												context, name)));
					}
				}
			}
		}
		parseHTMLElement(el, context, null);
	}

	protected void parseSelectOption(Element element, ParseContext context) {
		element = (Element) element.cloneNode(true);// options 有值，textarea有值
		Element selectNode = (Element) context.getAttribute(KEY_SELECT);
		List<Object> attributes = null;
		if (!element.hasAttribute(ATTRIBUTE_SELECTED)) {
			if (selectNode.hasAttribute(ATTRIBUTE_NAME)
					&& element.hasAttribute(ATTRIBUTE_VALUE)) {
				String name = selectNode.getAttribute(ATTRIBUTE_NAME);
				String value = element.getAttribute(ATTRIBUTE_VALUE);
				attributes = buildCheckedAttribute(context, name, value,
						ATTRIBUTE_SELECTED);
			}
		}
		parseHTMLElement(element, context, attributes);
	}

	protected List<Object> buildCheckedAttribute(ParseContext context,
			String name, String value, String attributeName) {
		List<Object> attributes = new ArrayList<Object>();
		final String valueEL;
		if (value.startsWith("${") && value.endsWith("}")) {
			value = value.substring(2, value.length() - 1);
			valueEL = value;
		} else {
			valueEL = JSONEncoder.encode(value);
		}
		final String collectionEL = name;
		attributes
				.add(new Object[] {
						Template.IF_TYPE,
						context.parseEL(buildCSEL(context, collectionEL,
								valueEL)) });
		attributes.add(" " + attributeName + "=\""
				+ BOOLEAN_ATTBUTE_MAP.get(attributeName) + "\"");
		attributes.add(END_INSTRUCTION);
		return attributes;
	}

	private String buildCSEL(ParseContext context, final String collectionEL,
			final String valueEL) {
		String id = context.addGlobalObject(TextContains.class, null);
		return id + "(" + collectionEL + "," + valueEL + ")";
	}

	private String buildNullEmptyEL(ParseContext context, final String valueEL) {
		String id = context.addGlobalObject(TextNullEmpty.class, null);
		return "${" + id + "(" + valueEL + ")}";
	}

}
