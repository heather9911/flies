package org.fedorahosted.flies.webtrans.client;

import java.util.ArrayList;

import org.fedorahosted.flies.gwt.model.Person;
import org.fedorahosted.flies.webtrans.client.ui.HasTreeNodes;
import org.fedorahosted.flies.webtrans.client.ui.TreeNode;
import org.fedorahosted.flies.webtrans.client.ui.TreeNodeMapper;

public class PersonTreeNodeMapper implements TreeNodeMapper<Person> {

	@Override
	public void addToTree(HasTreeNodes<Person> tree,
			ArrayList<Person> elements, boolean openFolderNodes) {
		for (Person person : elements) {
			TreeNode<Person> node = tree.addItem(person.getName());
			node.setObject(person);
		}
	}

	@Override
	public boolean passFilter(Person element, String filter) {
		return element.getName().toLowerCase().contains(filter.toLowerCase());
	}

}