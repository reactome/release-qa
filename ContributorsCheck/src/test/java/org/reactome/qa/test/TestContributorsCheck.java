package org.reactome.qa.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.SchemaClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.reactome.qa.contributorscheck.ContributorsCheck;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

@RunWith(org.powermock.modules.junit4.PowerMockRunner.class)
@PrepareForTest( {ContributorsCheck.class} )
public class TestContributorsCheck
{
	private MySQLAdaptor mockAdaptor = PowerMockito.mock(MySQLAdaptor.class);
	
	@Before
	public void setup() throws Exception
	{
		PowerMockito.whenNew(MySQLAdaptor.class).withAnyArguments().thenReturn(mockAdaptor);
	}
	
	@Test
	public void testCheckContributors() throws Exception
	{
		long pathwayDbId = 9018678;
		GKInstance mockInst1 = Mockito.mock(GKInstance.class);
		Mockito.when(mockInst1.getDBID()).thenReturn(new Long(pathwayDbId));
		Mockito.when(mockInst1.getDisplayName()).thenReturn("test GKInst object");
		
		GKInstance child = Mockito.mock(GKInstance.class);
		SchemaClass mockSchema = Mockito.mock(SchemaClass.class);
		Mockito.when(mockSchema.getName()).thenReturn("Reaction");
		Mockito.when(child.getSchemClass()).thenReturn(mockSchema);
		Mockito.when(child.getDBID()).thenReturn(new Long(123456789));
		Mockito.when(child.getDisplayName()).thenReturn("test GKInst object - child");

		GKInstance mockIE = Mockito.mock(GKInstance.class);
		List<GKInstance> instEditList = new ArrayList<GKInstance>(Arrays.asList(mockIE));
		GKInstance mockPerson = Mockito.mock(GKInstance.class);
		Mockito.when(mockPerson.getDisplayName()).thenReturn("Mock Person");
		Mockito.when(mockIE.getAttributeValuesList("author")).thenReturn(Arrays.asList(mockPerson));
		
		Mockito.when(child.getAttributeValuesList("authored")).thenReturn(instEditList);
		Mockito.when(child.getAttributeValuesList("revised")).thenReturn(instEditList);
		Mockito.when(child.getAttributeValuesList("reviewed")).thenReturn(instEditList);
		
		Mockito.when(child.toString()).thenReturn("Pathway - child");
		Mockito.when(mockInst1.toString()).thenReturn("Pathway - parent");
		
		
		List<GKInstance> children = new ArrayList<GKInstance>();
		children.add(child);
		
		Mockito.when(mockInst1.getAttributeValuesList("hasEvent")).thenReturn(children);
		
		Mockito.when(mockAdaptor.fetchInstance(pathwayDbId)).thenReturn(mockInst1);
		
		GKSchemaAttribute mockAttrib = Mockito.mock(GKSchemaAttribute.class);
		Mockito.when(mockAttrib.getName()).thenReturn("authored").thenReturn("reviewed").thenReturn("revised");
		
		
		List<GKSchemaAttribute> attribs = new ArrayList<GKSchemaAttribute>();
		attribs.add(mockAttrib);
		Mockito.when(child.getSchemaAttributes()).thenReturn(attribs);
		
		
		Report report = ContributorsCheck.checkNewContributors("src/test/resources/contributors-check-input.txt");
		
		
		assertNotNull(report);
		
		((DelimitedTextReport)report).print("\t", System.out);
		
		assertTrue(true);
	}
}
