package org.fedorahosted.flies.rest.dto.v1.ext;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.fedorahosted.flies.rest.dto.SimpleComment;
import org.fedorahosted.flies.rest.dto.po.HeaderEntry;
import org.fedorahosted.flies.rest.dto.v1.Extension;

@XmlType(name="poHeaderExtension", namespace=PoHeader.NAMESPACE, propOrder={"comment", "entries"})
public class PoHeader extends Extension {
	
	public static final String NAMESPACE = "http://flies.fedorahosted.org/api/gettext/header";

	private SimpleComment comment;
	private List<HeaderEntry> entries;
	
	public PoHeader() {
		super("gettext-po-header");
	}
	
	@XmlElement(name="comment", namespace=NAMESPACE, required=true)
	public SimpleComment getComment() {
		if(comment == null)
			comment = new SimpleComment();
		return comment;
	}

	public void setComment(SimpleComment comment) {
		this.comment = comment;
	}
	
	public void setComment(String comment){
		getComment().setValue(comment);
	}
	
	@XmlElementWrapper(name="entries", namespace=NAMESPACE, required=true)
	@XmlElement(name="entry", namespace=NAMESPACE)
	public List<HeaderEntry> getEntries() {
		if(entries == null)
			entries = new ArrayList<HeaderEntry>();
		return entries;
	}	
	

}
