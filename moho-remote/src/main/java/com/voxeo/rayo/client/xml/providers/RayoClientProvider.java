package com.voxeo.rayo.client.xml.providers;

import java.net.URISyntaxException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.rayo.core.xml.providers.BaseProvider;
import com.voxeo.rayo.client.verb.ClientPauseCommand;
import com.voxeo.rayo.client.verb.ClientResumeCommand;
import com.voxeo.rayo.client.verb.RefEvent;

public class RayoClientProvider extends BaseProvider {
	
    private static final Namespace OUTPUT_NAMESPACE = new Namespace("", "urn:xmpp:rayo:output:1");

    
	@Override
	protected Object processElement(Element element) throws Exception {

        String elementName = element.getName();
        
        if (elementName.equals("ref")) {
        	return buildRef(element);
        }
        
        return null;
	}

    @Override
    protected void generateDocument(Object object, Document document) throws Exception {

        if (object instanceof RefEvent) {
            createRef(object, document);
        } else if (object instanceof ClientPauseCommand) {
            createPauseCommand(object, document);
        } if (object instanceof ClientResumeCommand) {
            createResumeCommand(object, document);
        } 
    }
    
    private void createPauseCommand(Object command, Document document) throws Exception {
        document.addElement(new QName("pause", OUTPUT_NAMESPACE));
    }

    private void createResumeCommand(Object command, Document document) throws Exception {
        document.addElement(new QName("resume", OUTPUT_NAMESPACE));
    }

	private Object buildRef(org.dom4j.Element element) throws URISyntaxException {
		
		RefEvent ref = new RefEvent();
		ref.setJid(element.attributeValue("id"));
		return ref;		
	}	
	
	private Document createRef(Object object, Document document) throws Exception {
		
		RefEvent ref = (RefEvent)object;
		Element root = document.addElement(new QName("ref", new Namespace("","urn:xmpp:rayo:1")));
		root.addAttribute("id", ref.getJid());
		return document;
	}
	
	@Override
	public boolean handles(Class<?> clazz) {

		return clazz == RefEvent.class ||
			   clazz == ClientPauseCommand.class ||
			   clazz == ClientResumeCommand.class;
	}
}
