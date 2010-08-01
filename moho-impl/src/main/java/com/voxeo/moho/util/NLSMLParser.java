/**
 * Copyright 2010 Voxeo Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.voxeo.moho.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NLSMLParser {

  private static final List<Map<String, String>> EMPTY_RESULT = new ArrayList<Map<String, String>>(0);

  public static List<Map<String, String>> parse(final String xml) throws Exception {

    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);

    InputStream is = null;
    Document d = null;
    try {
      is = new ByteArrayInputStream(xml.getBytes());
      d = factory.newDocumentBuilder().parse(is);
    }
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (final IOException e) {
        }
      }
    }

    final NodeList results = d.getElementsByTagName("result");
    if (results.getLength() == 0) {
      return EMPTY_RESULT;
    }

    final List<Map<String, String>> retval = new ArrayList<Map<String, String>>(results.getLength());

    for (int i = 0; i < results.getLength(); i++) {
      final Element result = (Element) results.item(i);
      final String fullGrammarID = result.getAttribute("grammar");
      final List<Element> interpretations = getChildElementsList(result, "interpretation");

      for (final Element interpretation : interpretations) {
        final Map<String, String> reco = new HashMap<String, String>();
        retval.add(reco);

        final String localFullGrammarID = interpretation.getAttribute("grammar");
        String gramid;
        String gram;
        if (localFullGrammarID != null && !localFullGrammarID.equals("")) {
          gramid = parseGrammarID(localFullGrammarID, false);
          gram = parseGrammarID(localFullGrammarID, true);
        }
        else {
          gramid = parseGrammarID(fullGrammarID, false);
          gram = parseGrammarID(fullGrammarID, true);
        }
        reco.put("_grammar_id", gramid);
        reco.put("_grammar", gram);

        final String conf = interpretation.getAttributes().getNamedItem("confidence").getNodeValue();
        final float confF = Float.parseFloat(conf) / 100F;
        reco.put("_confidence", Float.toString(confF));
        reco.put("_interpretationconfidence", Float.toString(confF));

        String tag = getTextContent(interpretation);
        if (tag != null) {
          tag = tag.trim();
          if (tag.length() > 0) {
            tag = tag.replaceAll("\\s*", "");
            reco.put("_tag", tag);
          }
        }

        final Element voxeoresult = getFirstChildElement(interpretation, "voxeoresult");
        if (voxeoresult != null) {
          final Element concept = getFirstChildElement(voxeoresult, "concept");
          if (concept != null) {
            String conceptText = getTextContent(concept);
            if (conceptText != null) {
              conceptText = conceptText.trim();
              if (conceptText.length() > 0) {
                reco.put("_concept", conceptText);
              }
            }
          }
          final Element interp = getFirstChildElement(voxeoresult, "interpretation");
          if (interp != null) {
            String interpText = getTextContent(interp);
            if (interpText != null) {
              interpText = interpText.trim();
              if (interpText.length() > 0) {
                reco.put("_interpretation", interpText);
              }
            }
          }
        }
      }
    }

    return retval;
  }

  private static Element getFirstChildElement(final Node node, final String nodeName) {
    return getFirstChildElementIntern(node, new QName(nodeName));
  }

  private static Element getFirstChildElementIntern(final Node node, final QName nodeName) {
    Element childElement = null;
    final Iterator it = getChildElementsIntern(node, nodeName).iterator();
    if (it.hasNext()) {
      childElement = (Element) it.next();
    }
    return childElement;
  }

  private static List<Element> getChildElementsList(final Node node, final String nodeName) {
    return getChildElementsIntern(node, new QName(nodeName));
  }

  private static List<Element> getChildElementsIntern(final Node node, final QName nodeName) {
    final ArrayList<Element> list = new ArrayList<Element>();
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
      }
    }
    return Collections.unmodifiableList(list);
  }

  private static String getTextContent(final Node node) {
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

  private static String parseGrammarID(final String fullGrammarID, final boolean suffix) {
    // session:0@vomoto.com
    final int start = fullGrammarID.indexOf(":") + 1;
    final int end = fullGrammarID.indexOf("@");

    if (start == -1 || end == -1) {
      return null;
    }
    else {
      String foo = null;
      if (start == end) {
        foo = "" + fullGrammarID.charAt(end);
      }
      else {
        if (suffix) {
          foo = fullGrammarID.substring(start);
        }
        else {
          foo = fullGrammarID.substring(start, end);
        }
      }
      return foo;
    }
  }

}
