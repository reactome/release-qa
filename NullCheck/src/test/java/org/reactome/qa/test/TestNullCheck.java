package org.reactome.qa.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.reactome.qa.nullcheck.FailedReactionChecker;
import org.reactome.qa.nullcheck.PhysicalEntityChecker;
import org.reactome.qa.nullcheck.SimpleEntityChecker;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

@RunWith(org.powermock.modules.junit4.PowerMockRunner.class)
//@PrepareForTest({ SimpleEntityChecker.class })
public class TestNullCheck
{

	private MySQLAdaptor mockAdaptor = PowerMockito.mock(MySQLAdaptor.class);
	
	@Before
	public void setup() throws Exception
	{
		PowerMockito.whenNew(MySQLAdaptor.class).withAnyArguments().thenReturn(mockAdaptor);
	}
	
	@Test
	public void testSimpleEntityChecker() throws Exception
	{

		
		GKInstance species = Mockito.mock(GKInstance.class);
		Mockito.when(species.toString()).thenReturn("Test_Species");
		
		GKInstance author = Mockito.mock(GKInstance.class);
		Mockito.when(author.getDBID()).thenReturn(new Long(123456));
		Mockito.when(author.getDisplayName()).thenReturn("Author1").thenReturn("Author #2");
		//List<GKInstance> authorList = new ArrayList<GKInstance>(Arrays.asList(author, author));

		GKInstance instEdit = Mockito.mock(GKInstance.class);
		Mockito.when(instEdit.getAttributeValue("author")).thenReturn(author);
		Mockito.when(instEdit.getDisplayName()).thenReturn("Modified by Author1").thenReturn("Modified by Author #2");
		
		SchemaClass mockSchemaClass = Mockito.mock(SchemaClass.class);
		Mockito.when(mockSchemaClass.getName()).thenReturn("Some class").thenReturn("Some Other class");
		
		GKInstance mockInst1 = Mockito.mock(GKInstance.class);
		Mockito.when(mockInst1.getDBID()).thenReturn(new Long(12345));
		Mockito.when(mockInst1.getDisplayName()).thenReturn("Mock Instance 1");
		Mockito.when(mockInst1.getSchemClass()).thenReturn(mockSchemaClass);
		Mockito.when(mockInst1.getAttributeValuesList("modified")).thenReturn(Arrays.asList(instEdit));
		Mockito.when(mockInst1.getAttributeValue("species")).thenReturn(species);
		
		GKInstance mockInst2 = Mockito.mock(GKInstance.class);
		Mockito.when(mockInst2.getDBID()).thenReturn(new Long(12345));
		Mockito.when(mockInst2.getDisplayName()).thenReturn("Mock Instance Two");
		Mockito.when(mockInst2.getSchemClass()).thenReturn(mockSchemaClass);
		//Mockito.when(mockInst2.getAttributeValuesList("modified")).thenReturn(authorList);
		Mockito.when(mockInst2.getAttributeValuesList("modified")).thenReturn(Arrays.asList(instEdit));
		Mockito.when(mockInst2.getAttributeValue("species")).thenReturn(species);
		
		List<GKInstance> instances = new ArrayList<GKInstance>(Arrays.asList(mockInst1, mockInst2));
		
		Mockito.when(mockAdaptor.fetchInstanceByAttribute(anyString(), anyString(), anyString(), any())).thenReturn(instances);
		
		SimpleEntityChecker checker = new SimpleEntityChecker();
		checker.setAdaptor(mockAdaptor);
		Report r = checker.executeQACheck();
		
		((DelimitedTextReport)r).print("\t", System.out);
	}
	
	@Test
	public void testPhysicalEntityChecker() throws Exception
	{
		GKInstance species = Mockito.mock(GKInstance.class);
		Mockito.when(species.toString()).thenReturn("Test_Species");
		
		GKInstance author = Mockito.mock(GKInstance.class);
		Mockito.when(author.getDBID()).thenReturn(new Long(123456));
		Mockito.when(author.getDisplayName()).thenReturn("Author1").thenReturn("Author #2");
		//List<GKInstance> authorList = new ArrayList<GKInstance>(Arrays.asList(author, author));

		GKInstance instEdit = Mockito.mock(GKInstance.class);
		Mockito.when(instEdit.getAttributeValue("author")).thenReturn(author);
		Mockito.when(instEdit.getDisplayName()).thenReturn("Modified by Author1").thenReturn("Modified by Author #2");
		
		SchemaClass mockSchemaClass = Mockito.mock(SchemaClass.class);
		Mockito.when(mockSchemaClass.getName()).thenReturn("Complex").thenReturn("EntitySet").thenReturn("Polymer");
		Mockito.when(mockSchemaClass.isValidAttribute("species")).thenReturn(true);
		
		GKInstance mockInst1 = Mockito.mock(GKInstance.class);
		Mockito.when(mockInst1.getDBID()).thenReturn(new Long(12345));
		Mockito.when(mockInst1.getDisplayName()).thenReturn("Mock Instance 1");
		Mockito.when(mockInst1.getSchemClass()).thenReturn(mockSchemaClass);
		Mockito.when(mockInst1.getAttributeValuesList("modified")).thenReturn(Arrays.asList(instEdit));
		Mockito.when(mockInst1.getAttributeValuesList("created")).thenReturn(Arrays.asList(instEdit));
		Mockito.when(mockInst1.getAttributeValuesList("species")).thenReturn(Arrays.asList(species));
		
		GKInstance mockInst2 = Mockito.mock(GKInstance.class);
		Mockito.when(mockInst2.getDBID()).thenReturn(new Long(12345));
		Mockito.when(mockInst2.getDisplayName()).thenReturn("Mock Instance Two");
		Mockito.when(mockInst2.getSchemClass()).thenReturn(mockSchemaClass);
		Mockito.when(mockInst2.getAttributeValuesList("modified")).thenReturn(Arrays.asList(instEdit));
		Mockito.when(mockInst2.getAttributeValuesList("created")).thenReturn(Arrays.asList(instEdit));
		Mockito.when(mockInst2.getAttributeValuesList("species")).thenReturn(Arrays.asList(species));
		
		List<GKInstance> instances = new ArrayList<GKInstance>(Arrays.asList(mockInst1, mockInst2));
		
		Mockito.when(mockAdaptor.fetchInstanceByAttribute(anyString(), anyString(), anyString(), any())).thenReturn(instances);
		
		PhysicalEntityChecker checker = new PhysicalEntityChecker();
		checker.setAdaptor(mockAdaptor);
		Report r = checker.executeQACheck();
		
		((DelimitedTextReport)r).print("\t", System.out);
	}

	@Test
	public void testFailedReactionChecker() throws Exception
	{
		GKInstance species = Mockito.mock(GKInstance.class);
		Mockito.when(species.toString()).thenReturn("Test_Species");
		
		GKInstance author = Mockito.mock(GKInstance.class);
		Mockito.when(author.getDBID()).thenReturn(new Long(123456));
		Mockito.when(author.getDisplayName()).thenReturn("Author1").thenReturn("Author #2");

		GKInstance instEdit = Mockito.mock(GKInstance.class);
		Mockito.when(instEdit.getAttributeValue("author")).thenReturn(author);
		Mockito.when(instEdit.getDisplayName()).thenReturn("Modified by Author1").thenReturn("Modified by Author #2");
		
		SchemaClass mockSchemaClass = Mockito.mock(SchemaClass.class);
		Mockito.when(mockSchemaClass.getName()).thenReturn("Some class").thenReturn("Some Other class");
		
		GKInstance mockInst1 = Mockito.mock(GKInstance.class);
		Mockito.when(mockInst1.getDBID()).thenReturn(new Long(12345));
		Mockito.when(mockInst1.getDisplayName()).thenReturn("Mock Instance 1");
		Mockito.when(mockInst1.getSchemClass()).thenReturn(mockSchemaClass);
		Mockito.when(mockInst1.getAttributeValuesList("created")).thenReturn(Arrays.asList(instEdit));
		Mockito.when(mockInst1.getAttributeValue("species")).thenReturn(species);
		
		GKInstance mockInst2 = Mockito.mock(GKInstance.class);
		Mockito.when(mockInst2.getDBID()).thenReturn(new Long(12345));
		Mockito.when(mockInst2.getDisplayName()).thenReturn("Mock Instance Two");
		Mockito.when(mockInst2.getSchemClass()).thenReturn(mockSchemaClass);

		Mockito.when(mockInst2.getAttributeValuesList("modified")).thenReturn(Arrays.asList(instEdit));
		Mockito.when(mockInst2.getAttributeValue("species")).thenReturn(species);
		
		List<GKInstance> instances = new ArrayList<GKInstance>(Arrays.asList(mockInst1, mockInst2));
		
		Mockito.when(mockAdaptor.fetchInstanceByAttribute(anyString(), anyString(), anyString(), any())).thenReturn(instances);
		
		FailedReactionChecker checker = new FailedReactionChecker();
		checker.setAdaptor(mockAdaptor);
		Report r = checker.executeQACheck();
		
		((DelimitedTextReport)r).print("\t", System.out);
	}
	
}
