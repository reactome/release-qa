package org.reactome.release.qa.check;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.reactome.release.qa.check.EHLDSubpathwayChangeCheck.EHLDPathway;

public class EHLDSubpathwayChangeCheckTest {
	@Mock
	MySQLAdaptor adaptor;

	@Mock
	SchemaClass pathwaySchemaClass;

	@Mock
	GKInstance firstPathway;

	@Mock
	GKInstance secondPathway;

	private static final long FIRST_PATHWAY_DB_ID = 1L;
	private static final long SECOND_PATHWAY_DB_ID = 2L;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetEHLDPathwayIDs() throws Exception {
		setUpMockPathways();

		EHLDSubpathwayChangeCheck ehldSubpathwayChangeCheck = new EHLDSubpathwayChangeCheck();
		List<Long> pathwayIDs = ehldSubpathwayChangeCheck.getPathwayIDsWithEHLD(adaptor);

		assertThat(pathwayIDs, contains(firstPathway.getDBID())); // Contains firstPathway id only
	}

	@Test
	public void testGetEHLDSubPathways() throws Exception {
		setUpMockPathways();
		Mockito.when(firstPathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent))
			.thenReturn(Collections.singletonList(secondPathway));

		EHLDSubpathwayChangeCheck ehldSubpathwayChangeCheck = new EHLDSubpathwayChangeCheck();
		EHLDPathway ehldPathway = ehldSubpathwayChangeCheck.getEHLDPathways(
			Collections.singletonList(FIRST_PATHWAY_DB_ID), adaptor
		).get(0); // Retrieve first EHLD pathway

		assertThat(ehldPathway.getPathway(), equalTo(firstPathway));
		assertThat(ehldPathway.getSubPathways(), contains(secondPathway));
	}

	private void setUpMockPathways() throws Exception {
		setUpMockPathway(firstPathway, FIRST_PATHWAY_DB_ID, "true");
		setUpMockPathway(secondPathway, SECOND_PATHWAY_DB_ID, "false");

		Mockito.when(adaptor.fetchInstancesByClass(ReactomeJavaConstants.Pathway))
			.thenReturn(Arrays.asList(firstPathway, secondPathway));
		Mockito.when(adaptor.fetchInstance((Collection<Long>) Collections.singletonList(FIRST_PATHWAY_DB_ID)))
			.thenReturn(Collections.singletonList(firstPathway));
	}

	private void setUpMockPathway(GKInstance pathway, long dbId, String hasEHLD) throws Exception {
		Mockito.when(pathway.getSchemClass()).thenReturn(pathwaySchemaClass);
		Mockito.when(pathwaySchemaClass.isa(ReactomeJavaConstants.Pathway)).thenReturn(true);
		Mockito.when(pathway.getAttributeValue("hasEHLD")).thenReturn(hasEHLD);
		Mockito.when(pathway.getDBID()).thenReturn(dbId);
	}
}
