package info.reflets.app.parsing;

import info.reflets.app.model.Article;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ArticleParser extends DefaultHandler{

	private final static String ITEM 		= "item";
	private final static String TITLE 		= "title";
	private final static String LINK 		= "link";
	private final static String PUBDATE		= "pubdate";
	private final static String DESCRIPTION = "description";
	private final static String CREATOR 	= "creator";
	private final static String CONTENT 	= "encoded";
	
	private ArrayList<Article> entries;
	
	private Article currentEntry;
	
	private boolean insideItem = false;

	private StringBuffer buffer;
	
	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		super.processingInstruction(target, data);
	}
	
	public ArticleParser(){
		super();
	}
	
	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		entries = new ArrayList<Article>();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		
		buffer = new StringBuffer(); 

		if (localName.equalsIgnoreCase(ITEM)){
			this.currentEntry = new Article();
			insideItem = true;
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		
		if (localName.equalsIgnoreCase(TITLE) && insideItem){
			this.currentEntry.setTitle(buffer.toString());
			buffer = null;
		}
		else if (localName.equalsIgnoreCase(LINK) && insideItem){
			this.currentEntry.setLink(buffer.toString());
			buffer = null;
		}
		else if (localName.equalsIgnoreCase(PUBDATE) && insideItem){
			this.currentEntry.setDate(buffer.toString());
			buffer = null;
		}
		else if (localName.equalsIgnoreCase(DESCRIPTION) && insideItem){
			this.currentEntry.setDescription(buffer.toString());
			buffer = null;
		}
		else if (localName.equalsIgnoreCase(CREATOR) && insideItem){
			this.currentEntry.setAuthor(buffer.toString());
			buffer = null;
		}
		else if (localName.equalsIgnoreCase(CONTENT) && insideItem){
			this.currentEntry.setContent(buffer.toString());
			buffer = null;
		}
		else if (localName.equalsIgnoreCase(ITEM)){
			entries.add(currentEntry);
			insideItem = false;
		}
		
	}
	
	public ArrayList<Article> getArticles() {
		return entries;
	}
	
	public void characters(char[] ch,int start, int length) throws SAXException{
		String lecture = new String(ch,start,length);
		if(buffer != null) buffer.append(lecture);
	}
}
