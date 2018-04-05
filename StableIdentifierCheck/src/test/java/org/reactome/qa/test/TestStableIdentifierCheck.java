package org.reactome.qa.test;

import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.reactome.qa.stableIdentifier.StableIdentifierCheck;

@RunWith(org.powermock.modules.junit4.PowerMockRunner.class)
//@PrepareForTest({ StableIdentifierCheck.class })
public class TestStableIdentifierCheck
{

	private static final String QUERY_STRING = "select identifier, count(*) as identifier_count\n" + 
			"from StableIdentifier\n" + 
			"group by identifier\n" + 
			"having count(*) <> 1\n" + 
			"order by identifier_count desc;";
	
	private MySQLAdaptor mockAdaptor = PowerMockito.mock(MySQLAdaptor.class);
	
	@Test
	public void testStableIdentifierCheck() throws SQLException
	{
		GKInstance inst1 =  PowerMockito.mock(GKInstance.class);
		PowerMockito.when(inst1.getDisplayName()).thenReturn("Inst 1");
		PowerMockito.when(inst1.getDBID()).thenReturn(new Long(12345));
		GKInstance inst2 = PowerMockito.mock(GKInstance.class);
		PowerMockito.when(inst2.getDisplayName()).thenReturn("Inst 2");
		PowerMockito.when(inst2.getDBID()).thenReturn(new Long(67890));
	
		Collection<GKInstance> testResults = Arrays.asList(inst1, inst2);
		ResultSet mockResultSet = PowerMockito.mock(ResultSet.class);
		PowerMockito.when(mockResultSet.next()).thenReturn(true).thenReturn(false);
		PowerMockito.when(mockResultSet.getString("identifier")).thenReturn("Some Identifier");
		PowerMockito.when(mockResultSet.getInt("identifier_count")).thenReturn(123);
		try
		{
			
			PowerMockito.whenNew(MySQLAdaptor.class).withAnyArguments().thenReturn(mockAdaptor);
			Mockito.when(mockAdaptor.fetchInstanceByAttribute("DatabaseIdentifier", "identifier", "IS NULL", null)).thenReturn(testResults);
			Mockito.when(mockAdaptor.executeQuery(QUERY_STRING, null)).thenReturn(mockResultSet);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		//StableIdentifierCheck checker = new StableIdentifierCheck();
		StableIdentifierCheck.executeStableIdentifierCheck("src/test/resources/auth.properties");
		assertTrue(true);
	}

}
