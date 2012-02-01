package com.voxeo.rayo.client.xmpp.extensions;

import org.dom4j.Element;

import com.rayo.core.xml.XmlProviderManager;
import com.voxeo.rayo.client.xmpp.util.Dom4jParser;

public class ExtensionsManager {

	private static XmlProviderManager manager = XmlProviderManagerFactory.buildXmlProvider();
		
	public static Extension buildExtension(Object object) throws ProviderException {
		
		try {
			return new Extension(manager.toXML(object));
		} catch (ProviderException p) {
			throw p;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}		
	}
	
	public static Object unmarshall(Extension extension) {
		
		return unmarshall(extension, Object.class);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T unmarshall(Extension extension, Class<T> clazz) throws ProviderException {
		
		String xml = extension.getElement().asXML();
		try {
			Element element = Dom4jParser.parseXml(xml);
			return (T)manager.fromXML(element);
		} catch (ProviderException p) {
			throw p;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
