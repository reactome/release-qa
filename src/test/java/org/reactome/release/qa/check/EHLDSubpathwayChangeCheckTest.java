package org.reactome.release.qa.check;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.Arrays;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class EHLDSubpathwayChangeCheckTest {
	@Mock
	MySQLAdaptor adaptor;

	@Mock
	GKInstance firstPathway;

	@Mock
	GKInstance secondPathway;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetEHLDPathwayIDs() throws Exception {
		setUpMockPathway(firstPathway, 1, "true");
		setUpMockPathway(secondPathway, 2, "false");
		Mockito.when(adaptor.fetchInstancesByClass(ReactomeJavaConstants.Pathway))
			.thenReturn(Arrays.asList(firstPathway, secondPathway));

		EHLDSubpathwayChangeCheck ehldSubpathwayChangeCheck = new EHLDSubpathwayChangeCheck();
		List<Long> pathwayIDs = ehldSubpathwayChangeCheck.getPathwayIDsWithEHLD(adaptor);

		assertThat(pathwayIDs, contains(firstPathway.getDBID())); // Contains firstPathway id only
	}

	private void setUpMockPathway(GKInstance pathway, long dbId, String hasEHLD) throws Exception {
		Mockito.when(pathway.getAttributeValue("hasEHLD")).thenReturn(hasEHLD);
		Mockito.when(pathway.getDBID()).thenReturn(dbId);
	}
}
