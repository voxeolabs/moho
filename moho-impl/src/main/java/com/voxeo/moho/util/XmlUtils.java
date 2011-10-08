package com.voxeo.moho.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A utility class to cover up the rough bits of xml parsing
 * 
 * @author Kyle Liu
 */
public class XmlUtils {

  private static ThreadLocal<Document> documentThreadLocal = new ThreadLocal<Document>();

  private static ThreadLocal<DocumentBuilder> builderThreadLocal = new ThreadLocal<DocumentBuilder>() {
    @Override
    protected DocumentBuilder initialValue() {
      return getNewDocumentBuilder();
    }
  };

  private static DocumentBuilder getNewDocumentBuilder() {
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setAttribute("http://xml.org/sax/features/namespaces", true);
      factory.setAttribute("http://xml.org/sax/features/validation", false);
      factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
      factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      factory.setNamespaceAware(true);
      factory.setIgnoringElementContentWhitespace(true);
      factory.setValidating(false);
      factory.setIgnoringComments(false);
      final DocumentBuilder builder = factory.newDocumentBuilder();
      return builder;
    }
    catch (final ParserConfigurationException e) {
      throw new RuntimeException("Failed to create DocumentBuilder", e);
    }
  }

  private static DocumentBuilder getOwnerDocumentBuilder() {
    return builderThreadLocal.get();
  }

  public static Document createDocument() {
    return getNewDocumentBuilder().newDocument();
  }

  public static Document getOwnerDocument() {
    Document doc = documentThreadLocal.get();
    if (doc == null) {
      doc = getOwnerDocumentBuilder().newDocument();
      documentThreadLocal.set(doc);
    }
    return doc;
  }

  public static Element parse(final String xmlString) throws IOException {
    return parse(new ByteArrayInputStream(xmlString.getBytes(LanguageUtils.DEFAULT_ENCODING)));
  }

  public static Element parse(final InputStream xmlStream) throws IOException {
    try {
      return getOwnerDocumentBuilder().parse(xmlStream).getDocumentElement();
    }
    catch (final SAXException e) {
      throw new IOException(e.toString());
    }
  }

  public static Element parse(final InputSource source) throws IOException {
    try {
      return getOwnerDocumentBuilder().parse(source).getDocumentElement();
    }
    catch (final SAXException e) {
      throw new IOException(e.toString());
    }
  }

  public static String render(final Node node) {
    try {
      final StringWriter writer = new StringWriter();
      final DOMSource domSource = new DOMSource(node);
      final StreamResult streamResult = new StreamResult(writer);
      final TransformerFactory tf = TransformerFactory.newInstance();
      final Transformer serializer = tf.newTransformer();
      serializer.setOutputProperty(OutputKeys.INDENT, "yes");
      serializer.transform(domSource, streamResult);
      return writer.toString();
    }
    catch (final Exception e) {
      throw new RuntimeException("Error serializing DOM", e);
    }
  }



  public static String normalize(final String s, final boolean canonical) {
    final StringBuffer str = new StringBuffer();

    final int len = s != null ? s.length() : 0;
    for (int i = 0; i < len; i++) {
      final char ch = s.charAt(i);
      switch (ch) {
        case '<': {
          str.append("&lt;");
          break;
        }
        case '>': {
          str.append("&gt;");
          break;
        }
        case '&': {
          str.append("&amp;");
          break;
        }
        case '"': {
          str.append("&quot;");
          break;
        }
        case '\r':
        case '\n': {
          if (canonical) {
            str.append("&#");
            str.append(Integer.toString(ch));
            str.append(';');
            break;
          }
          // else, default append char
        }
        default: {
          str.append(ch);
        }
      }
    }
    return str.toString();
  }

  public static Element createElement(final String localPart) {
    return createElement(new QName(localPart));
  }

  public static Element createElement(final String localPart, final Document doc) {
    return createElement(new QName(localPart), doc);
  }

  public static Element createElement(final QName qname) {
    return createElement(qname.getLocalPart(), qname.getPrefix(), qname.getNamespaceURI(), getOwnerDocument());
  }

  public static Element createElement(final QName qname, final Document doc) {
    return createElement(qname.getLocalPart(), qname.getPrefix(), qname.getNamespaceURI(), doc);
  }

  public static Element createElement(final String localPart, final String prefix, final String uri, final Document doc) {
    if (prefix == null || prefix.length() == 0) {
      return doc.createElementNS(uri, localPart);
    }
    return doc.createElementNS(uri, prefix + ":" + localPart);
  }

  public static Text createTextNode(final String value) {
    final Document doc = getOwnerDocument();
    return doc.createTextNode(value);
  }

  public static Element copyElement(final Element element) {
    return copyElement(element, null);
  }

  public static Element copyElement(final Element element, final Document doc) {
    Element retval = null;
    if (doc == null) {
      retval = createElement(getElementQName(element));
    }
    else {
      retval = createElement(getElementQName(element), doc);
    }
    copyAttributes(retval, element);
    final String text = getTextContent(element);
    if (text != null) {
      retval.setTextContent(text);
    }
    for (final Element child : getChildElements(element)) {
      final Element childCopy = copyElement(child, doc);
      retval.appendChild(childCopy);
    }
    return retval;
  }

  public static Element getParentElement(final Node node) {
    final Node parent = node.getParentNode();
    return parent instanceof Element ? (Element) parent : null;
  }

  public static boolean hasChildElements(final Node node) {
    final NodeList nlist = node.getChildNodes();
    for (int i = 0; i < nlist.getLength(); i++) {
      final Node child = nlist.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        return true;
      }
    }
    return false;
  }

  public static List<Element> getChildElements(final Node node) {
    final ArrayList<Element> list = new ArrayList<Element>();
    final NodeList nlist = node.getChildNodes();
    for (int i = 0; i < nlist.getLength(); i++) {
      final Node child = nlist.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        list.add((Element) child);
      }
    }
    return list;
  }

  public static Element getFirstChildElement(final Node node) {
    return getFirstChildElementIntern(node, null);
  }

  public static Element getFirstChildElement(final Node node, final String nodeName) {
    return getFirstChildElementIntern(node, new QName(nodeName));
  }

  public static Element getFirstChildElement(final Node node, final QName nodeName) {
    return getFirstChildElementIntern(node, nodeName);
  }

  private static Element getFirstChildElementIntern(final Node node, final QName nodeName) {
    Element childElement = null;
    final Iterator it = getChildElementsIntern(node, nodeName, false).iterator();
    if (it.hasNext()) {
      childElement = (Element) it.next();
    }
    return childElement;
  }

  public static List<Element> getChildElements(final Node node, final String nodeName, final boolean recurse) {
    return getChildElementsIntern(node, new QName(nodeName), recurse);
  }

  public static List<Element> getChildElements(final Node node, final boolean recurse, final String... nodeNames) {
    final List<Element> retval = new ArrayList<Element>();
    for (final String nodeName : nodeNames) {
      retval.addAll(getChildElementsIntern(node, new QName(nodeName), recurse));
    }
    return retval;
  }

  private static List<Element> getChildElementsIntern(final Node node, final QName nodeName, final boolean recurse) {
    final ArrayList<Element> list = new ArrayList<Element>();
    if (node == null) {
      return list;
    }
    final NodeList nlist = node.getChildNodes();
    for (int i = 0; i < nlist.getLength(); i++) {
      final Node child = nlist.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        if (nodeName == null) {
          list.add((Element) child);
        }
        else {
          QName qname;
          if (nodeName.getNamespaceURI().length() > 0) {
            qname = new QName(child.getNamespaceURI(), child.getLocalName());
          }
          else {
            qname = new QName(child.getLocalName() == null ? child.getNodeName() : child.getLocalName());
          }
          if (qname.equals(nodeName)) {
            list.add((Element) child);
          }
        }
        if (recurse) {
          list.addAll(getChildElementsIntern(child, nodeName, true));
        }
      }
    }
    return Collections.unmodifiableList(list);
  }

  public static String getTextContent(final Node node) {
    boolean hasTextContent = false;
    final StringBuffer buffer = new StringBuffer();
    final NodeList nlist = node.getChildNodes();
    for (int i = 0; i < nlist.getLength(); i++) {
      final Node child = nlist.item(i);
      if (child.getNodeType() == Node.TEXT_NODE) {
        buffer.append(child.getNodeValue());
        hasTextContent = true;
      }
    }
    return hasTextContent ? buffer.toString() : null;
  }

  public static String getElementContent(final Element element, final boolean trim) {
    final NodeList nl = element.getChildNodes();
    String attributeText = "";
    for (int i = 0; i < nl.getLength(); i++) {
      final Node n = nl.item(i);
      if (n instanceof Text) {
        attributeText += ((Text) n).getData();
      }
    }
    if (trim) {
      attributeText = attributeText.trim();
    }
    return attributeText;
  }

  public static QName getElementQName(final Element el) {
    final String qualifiedName = el.getNodeName();
    return resolveQName(el, qualifiedName);
  }

  public static QName resolveQName(final Element el, final String qualifiedName) {
    QName qname;
    String prefix = "";
    String namespaceURI = "";
    String localPart = qualifiedName;

    final int colIndex = qualifiedName.indexOf(":");
    if (colIndex > 0) {
      prefix = qualifiedName.substring(0, colIndex);
      localPart = qualifiedName.substring(colIndex + 1);

      if ("xmlns".equals(prefix)) {
        namespaceURI = "URI:XML_PREDEFINED_NAMESPACE";
      }
      else {
        Element nsElement = el;
        while (namespaceURI.equals("") && nsElement != null) {
          namespaceURI = nsElement.getAttribute("xmlns:" + prefix);
          if (namespaceURI.equals("")) {
            nsElement = getParentElement(nsElement);
          }
        }
      }
      if (namespaceURI.equals("")) {
        throw new IllegalArgumentException("Cannot find namespace uri for: " + qualifiedName);
      }
    }

    qname = new QName(namespaceURI, localPart, prefix);
    return qname;
  }

  public static String getAttributeValue(final Element el, final String attrName) {
    return getAttributeValue(el, new QName(attrName));
  }

  public static String getAttributeValue(final Element el, final String attrName, final String defaultVal) {
    final String retval = getAttributeValue(el, new QName(attrName));
    return retval == null ? defaultVal : retval;
  }

  public static String getAttributeValue(final Element el, final QName attrName) {
    String attr = null;
    if ("".equals(attrName.getNamespaceURI())) {
      attr = el.getAttribute(attrName.getLocalPart());
    }
    else {
      attr = el.getAttributeNS(attrName.getNamespaceURI(), attrName.getLocalPart());
    }

    if ("".equals(attr)) {
      attr = null;
    }
    return attr;
  }

  public static QName getAttributeValueAsQName(final Element el, final String attrName) {
    return getAttributeValueAsQName(el, new QName(attrName));
  }

  public static QName getAttributeValueAsQName(final Element el, final QName attrName) {
    QName qname = null;
    final String qualifiedName = getAttributeValue(el, attrName);
    if (qualifiedName != null) {
      qname = resolveQName(el, qualifiedName);
    }
    return qname;
  }

  public static boolean getAttributeValueAsBoolean(final Element el, final String attrName) {
    return getAttributeValueAsBoolean(el, new QName(attrName));
  }

  public static boolean getAttributeValueAsBoolean(final Element el, final QName attrName) {
    return StringUtils.toBoolean(getAttributeValue(el, attrName));
  }

  public static boolean getAttributeValueAsBoolean(final Element el, final String attrName, final boolean defaultVal) {
    return getAttributeValueAsBoolean(el, new QName(attrName), defaultVal);
  }

  public static boolean getAttributeValueAsBoolean(final Element el, final QName attrName, final boolean defaultVal) {
    final String ret = getAttributeValue(el, attrName);
    if (ret == null) {
      return defaultVal;
    }
    return StringUtils.toBoolean(ret);
  }

  public static int getAttributeValueAsInteger(final Element el, final String attrName, final int defaultVal) {
    return getAttributeValueAsInteger(el, new QName(attrName), defaultVal);
  }

  public static int getAttributeValueAsInteger(final Element el, final QName attrName, final int defaultVal) {
    final String attrVal = getAttributeValue(el, attrName);
    return attrVal != null ? new Integer(attrVal) : defaultVal;
  }

  public static long getAttributeValueAsLong(final Element el, final String attrName, final long defaultVal) {
    return getAttributeValueAsLong(el, new QName(attrName), defaultVal);
  }

  public static long getAttributeValueAsLong(final Element el, final QName attrName, final long defaultVal) {
    final String attrVal = getAttributeValue(el, attrName);
    return attrVal != null ? new Long(attrVal) : defaultVal;
  }

  public static Map<QName, String> getAttributes(final Element el) {
    final Map<QName, String> attmap = new HashMap<QName, String>();
    final NamedNodeMap attribs = el.getAttributes();
    for (int i = 0; i < attribs.getLength(); i++) {
      final Attr attr = (Attr) attribs.item(i);
      final String name = attr.getName();
      final QName qname = resolveQName(el, name);
      final String value = attr.getNodeValue();
      attmap.put(qname, value);
    }
    return attmap;
  }

  public static Map<String, String> getAttributeMap(final Element el) {
    final Map<String, String> attmap = new HashMap<String, String>();
    final NamedNodeMap attribs = el.getAttributes();
    for (int i = 0; i < attribs.getLength(); i++) {
      final Attr attr = (Attr) attribs.item(i);
      final String name = attr.getName();
      final String value = attr.getNodeValue();
      attmap.put(name, value);
    }
    return attmap;
  }

  public static void copyAttributes(final Element destElement, final Element srcElement) {
    final NamedNodeMap attribs = srcElement.getAttributes();
    for (int i = 0; i < attribs.getLength(); i++) {
      final Attr attr = (Attr) attribs.item(i);
      final String uri = attr.getNamespaceURI();
      final String qname = attr.getName();
      final String value = attr.getNodeValue();
      if (uri == null && qname.startsWith("xmlns")) {
      }
      else {
        destElement.setAttributeNS(uri, qname, value);
      }
    }
  }

  public static void copyElementAttributes(final Element e, final Map<String, String> attributes, String[] filters) {
    final Map<QName, String> map = getAttributes(e);
    if (filters == null) {
      filters = new String[0];
    }
    for (final QName qname : map.keySet()) {
      boolean ignore = false;
      final String name = qname.getLocalPart();
      for (final String filter : filters) {
        if (name.equals(filter)) {
          ignore = true;
          break;
        }
      }
      if (!ignore) {
        attributes.put(name, map.get(qname));
      }
    }
  }

  public static final String CATEGORY = "category";

  public static final String ITEM = "item";

  public static final String NAME = "name";

  public static Element toElement(final Object o, final Document ctx) {
    if (o == null) {
      return null;
    }
    Element retval = null;
    if (o instanceof Collection) {
      retval = XmlUtils.createElement(CATEGORY, ctx);
      for (final Iterator it = ((Collection) o).iterator(); it.hasNext();) {
        retval.appendChild(toElement(it.next(), ctx));
      }
    }
    else if (o instanceof Map) {
      retval = XmlUtils.createElement(CATEGORY, ctx);
      for (final Object entry : ((Map) o).entrySet()) {
        retval.appendChild(toElement(entry, ctx));
      }
    }
    else if (o instanceof Map.Entry) {
      retval = toElement(((Map.Entry) o).getValue(), ctx);
      retval.setAttribute(NAME, StringUtils.toString(((Map.Entry) o).getKey()));
    }
    else {
      retval = XmlUtils.createElement(ITEM, ctx);
      retval.setTextContent(StringUtils.toString(o));
    }
    return retval;
  }

}
