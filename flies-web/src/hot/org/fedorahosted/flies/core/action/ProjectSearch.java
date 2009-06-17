package org.fedorahosted.flies.core.action;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Name;

import org.fedorahosted.flies.core.model.Project;

@Name("projectSearch")
@Scope(ScopeType.EVENT)
@AutoCreate
public class ProjectSearch {
    
    int pageSize = 15;
    int currentPage = 0;
    boolean hasMore = false;
    
    private String searchQuery;

    private List<Project> searchResults;
	
    @In
	EntityManager entityManager;
    
    public String getSearchQuery() {
        return searchQuery;
    }
    
    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }
    
    public List<Project> getSearchResults() {
        return searchResults;
    }
    
    public void setSearchResults(List<Project> projects) {
        this.searchResults = projects;
    }
    
    public void doSearch() {
        currentPage = 0;
        updateResults();
    }
    
    public void nextPage() {
        if (!isLastPage()) {
            currentPage++;
            updateResults();
        }
    }

    public void prevPage() {
        if (!isFirstPage()) {
            currentPage--;
            updateResults();
        }
    }

    public boolean isLastPage() {
        return ( searchResults != null ) && !hasMore;
    }

    public boolean isFirstPage() {
        return ( searchResults != null ) && ( currentPage == 0 );
    }

    private void updateResults() {
        FullTextQuery query;
        try {
            query = searchQuery(searchQuery);
        } catch (ParseException pe) { 
            return; 
        }
      
        List<Project> items = query
            .setMaxResults(pageSize + 1)
            .setFirstResult(pageSize * currentPage)
            .getResultList();
        
        if (items.size() > pageSize) {
            searchResults = new ArrayList(items.subList(0, pageSize));
            hasMore = true;
        } else {
            searchResults = items;
            hasMore = false;
        }

    }

    private FullTextQuery searchQuery(String searchQuery) throws ParseException
    {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
    
        List<Project> projects = entityManager.createQuery("select project from Project as project").getResultList();
	    for (Project project : projects) {
    		fullTextEntityManager.index(project);
	    } 
        
        String[] projectFields = {"name", "description"};
        QueryParser parser = new MultiFieldQueryParser(projectFields, new StandardAnalyzer());
        parser.setAllowLeadingWildcard(true);
        Query luceneQuery = parser.parse(searchQuery);
        return ( (FullTextEntityManager) entityManager ).createFullTextQuery(luceneQuery, Project.class);
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

}
