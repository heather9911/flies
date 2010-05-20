package org.fedorahosted.flies.model;

import java.util.Date;
import java.util.List;

import org.dbunit.operation.DatabaseOperation;
import org.fedorahosted.flies.FliesDbunitJpaTest;
import org.fedorahosted.flies.common.ContentType;
import org.fedorahosted.flies.common.LocaleId;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.CoreMatchers.*;

public class HDocumentHistoryTest extends FliesDbunitJpaTest {

	protected void prepareDBUnitOperations() {
		beforeTestOperations.add(new DataSetOperation(
				"META-INF/testdata/ProjectsData.dbunit.xml",
				DatabaseOperation.CLEAN_INSERT));
	}

	@Test
	public void ensureHistoryIsRecorded() {
		Session session = getSession();
		HDocument d = new HDocument("/path/to/document.txt",
				ContentType.TextPlain, LocaleId.EN);
		d.setProjectIteration((HProjectIteration) session.load(
				HProjectIteration.class, 1L));
		session.save(d);
		session.flush();

		Date lastChanged = d.getLastChanged();

		d.incrementRevision();
		d.setContentType(ContentType.PO);
		session.update(d);
		session.flush();

		List<HDocumentHistory> historyElems = loadHistory(d);

		assertThat(historyElems.size(), is(1));
		HDocumentHistory history = historyElems.get(0);
		assertThat(history.getDocId(), is(d.getDocId()));
		assertThat(history.getContentType(), is(ContentType.TextPlain));
		assertThat(history.getLastChanged(), is(lastChanged));
		assertThat(history.getLastModifiedBy(), nullValue());
		assertThat(history.getLocale(), is(LocaleId.EN));
		assertThat(history.getName(), is(d.getName()));
		assertThat(history.getPath(), is(d.getPath()));
		assertThat(history.getRevision(), is(d.getRevision() - 1));
		
		d.incrementRevision();
		d.setName("name2");
		session.update(d);
		session.flush();
		
		historyElems = loadHistory(d);
		assertThat(historyElems.size(), is(2));
		
		
		
	}

	private List<HDocumentHistory> loadHistory(HDocument d) {
		return getSession().createCriteria(HDocumentHistory.class).add(
				Restrictions.eq("document", d)).list();
	}

}
