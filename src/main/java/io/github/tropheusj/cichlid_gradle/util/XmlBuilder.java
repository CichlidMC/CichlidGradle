package io.github.tropheusj.cichlid_gradle.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public record XmlBuilder(List<XmlElement> elements) {
	public static XmlBuilder create() {
		return new XmlBuilder(new ArrayList<>());
	}

	public XmlBuilder add(XmlElement element) {
		this.elements.add(element);
		return this;
	}

	public void write(OutputStream stream) throws IOException {
		try {
			DOMSource source = new DOMSource(this.toDocument());
			TransformerFactory factory = TransformerFactory.newInstance();
			factory.setAttribute("indent-number", 4);
			Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(source, new StreamResult(stream));
		} catch (ParserConfigurationException | TransformerException e) {
			throw new RuntimeException(e);
		}
	}

	private Document toDocument() throws ParserConfigurationException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document xml = builder.newDocument();
		for (XmlElement element : this.elements) {
			element.addTo(xml, xml);
		}
		return xml;
	}

	public record XmlElement(String name, @Nullable String content, List<XmlElement> children) {
		public XmlElement(String name) {
			this(name, List.of());
		}

		public XmlElement(String name, String content) {
			this(name, content, List.of());
		}

		public XmlElement(String name, List<XmlElement> children) {
			this(name, null, children);
		}

		private void addTo(Document root, Node parent) {
			Element element = root.createElement(this.name);
			if (this.content != null)
				element.setTextContent(this.content);

			for (XmlElement child : this.children) {
				child.addTo(root, element);
			}

			parent.appendChild(element);
		}
	}
}
