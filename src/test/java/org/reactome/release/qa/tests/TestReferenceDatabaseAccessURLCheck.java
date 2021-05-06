package org.reactome.release.qa.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.reactome.release.qa.check.ReferenceDatabaseAccessURLCheck;
import org.reactome.release.qa.common.QAReport;

public class TestReferenceDatabaseAccessURLCheck
{
	private static final String NEW_URL = "http://www.some.resource/newurl/###ID###";

	private static final String OLD_URL = "http://www.some.resource/###ID###";

	private static final String MESSAGE_CONTENT = "{ \"urlPattern\":\"http://www.some.resource/newurl/{$id}\" }";
	
	@Mock
	private URI mockUri;
	
	@Mock
	private CloseableHttpClient mockClient;
	
	@Mock
	private CloseableHttpResponse mockResponse;

	@Mock
	private StatusLine mockStatusLine;
	
	private HttpEntity entity = new ByteArrayEntity(MESSAGE_CONTENT.getBytes());
	
	@Before
	public void setup()
	{
		MockitoAnnotations.openMocks(this);
	}
	
	/**
	 * Test that a mismatch in URLs will produce a report line.
	 * @throws InvalidAttributeException
	 * @throws Exception
	 */
	@Test
	public void testURLMisMatch() throws InvalidAttributeException, Exception
	{
		// Mock the Database objects
		GKInstance mockRefDB = setupMockRefDB(OLD_URL);
		Collection<GKInstance> mockRefDBs = new ArrayList<>();
		mockRefDBs.add(mockRefDB);
		
		try( MockedStatic<HttpClients> mockedStatic = Mockito.mockStatic(HttpClients.class); )
		{
			ReferenceDatabaseAccessURLCheck check = new ReferenceDatabaseAccessURLCheck();
			
			MySQLAdaptor mockAdaptor = Mockito.mock(MySQLAdaptor.class);
			Mockito.when(mockAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceDatabase)).thenReturn(mockRefDBs);
			check.setMySQLAdaptor(mockAdaptor);

			setupHttpResponse(HttpStatus.SC_OK);
			
			QAReport report = check.executeQACheck();
			printReport(report);
			assertEquals(1, report.getReportLines().size());
		}
	}

	/**
	 * Test that if the URLs match, there will be nothing in the report.
	 * @throws InvalidAttributeException
	 * @throws Exception
	 */
	@Test
	public void testURLMatch() throws InvalidAttributeException, Exception
	{
		// Mock the Database objects
		GKInstance mockRefDB = setupMockRefDB(NEW_URL);
		Collection<GKInstance> mockRefDBs = new ArrayList<>();
		mockRefDBs.add(mockRefDB);
		
		try( MockedStatic<HttpClients> mockedStatic = Mockito.mockStatic(HttpClients.class); )
		{
			ReferenceDatabaseAccessURLCheck check = new ReferenceDatabaseAccessURLCheck();
			
			MySQLAdaptor mockAdaptor = Mockito.mock(MySQLAdaptor.class);
			Mockito.when(mockAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceDatabase)).thenReturn(mockRefDBs);
			check.setMySQLAdaptor(mockAdaptor);

			setupHttpResponse(HttpStatus.SC_OK);
			
			QAReport report = check.executeQACheck();
			printReport(report);
			assertEquals(0, report.getReportLines().size());
		}
	}

	/**
	 * Test that if identifiers.org returns a 404, there will be nothing in the report.
	 * @throws InvalidAttributeException
	 * @throws Exception
	 */
	@Test
	public void testHttpResponse404() throws InvalidAttributeException, Exception
	{
		// Mock the Database objects
		GKInstance mockRefDB = setupMockRefDB(NEW_URL);
		Collection<GKInstance> mockRefDBs = new ArrayList<>();
		mockRefDBs.add(mockRefDB);
		
		try( MockedStatic<HttpClients> mockedStatic = Mockito.mockStatic(HttpClients.class); )
		{
			ReferenceDatabaseAccessURLCheck check = new ReferenceDatabaseAccessURLCheck();
			
			MySQLAdaptor mockAdaptor = Mockito.mock(MySQLAdaptor.class);
			Mockito.when(mockAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceDatabase)).thenReturn(mockRefDBs);
			check.setMySQLAdaptor(mockAdaptor);

			setupHttpResponse(HttpStatus.SC_NOT_FOUND);
			
			QAReport report = check.executeQACheck();
			printReport(report);
			assertEquals(0, report.getReportLines().size());
		}
	}
	
	/**
	 * Test that other (not 200, not 404) HTTP responses will return nothing in the report.
	 * @throws InvalidAttributeException
	 * @throws Exception
	 */
	@Test
	public void testOtherHttpResponse() throws InvalidAttributeException, Exception
	{
		// Mock the Database objects
		GKInstance mockRefDB = setupMockRefDB(NEW_URL);
		Collection<GKInstance> mockRefDBs = new ArrayList<>();
		mockRefDBs.add(mockRefDB);
		
		try( MockedStatic<HttpClients> mockedStatic = Mockito.mockStatic(HttpClients.class); )
		{
			ReferenceDatabaseAccessURLCheck check = new ReferenceDatabaseAccessURLCheck();
			
			MySQLAdaptor mockAdaptor = Mockito.mock(MySQLAdaptor.class);
			Mockito.when(mockAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceDatabase)).thenReturn(mockRefDBs);
			check.setMySQLAdaptor(mockAdaptor);

			setupHttpResponse(501);
			
			QAReport report = check.executeQACheck();
			printReport(report);
			assertEquals(0, report.getReportLines().size());
		}
	}
	
	/**
	 * Test that if the ReferenceDatabse objects has no resourceIdentifier attribute, 
	 * there will nothing in the report.
	 * @throws InvalidAttributeException
	 * @throws Exception
	 */
	@Test
	public void testNoResourceIdentifier() throws InvalidAttributeException, Exception
	{
		GKInstance mockRefDB = Mockito.mock(GKInstance.class);
		Mockito.when(mockRefDB.getAttributeValue(ReactomeJavaConstants.accessUrl)).thenReturn(NEW_URL);
		Mockito.when(mockRefDB.getAttributeValue("resourceIdentifier")).thenReturn(null);
		Mockito.when(mockRefDB.getDisplayName()).thenReturn("Some Resource");
		Mockito.when(mockRefDB.getDBID()).thenReturn(1234L);
		Collection<GKInstance> mockRefDBs = new ArrayList<>();
		mockRefDBs.add(mockRefDB);
		
		try( MockedStatic<HttpClients> mockedStatic = Mockito.mockStatic(HttpClients.class); )
		{
			ReferenceDatabaseAccessURLCheck check = new ReferenceDatabaseAccessURLCheck();
			
			MySQLAdaptor mockAdaptor = Mockito.mock(MySQLAdaptor.class);
			Mockito.when(mockAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceDatabase)).thenReturn(mockRefDBs);
			check.setMySQLAdaptor(mockAdaptor);

			setupHttpResponse(HttpStatus.SC_OK);
			// No report lines will be generated. Check the log, there should be a WARN message.
			QAReport report = check.executeQACheck();
			printReport(report);
			assertEquals(0, report.getReportLines().size());
		}
	}
	
	private GKInstance setupMockRefDB(String accessUrl) throws InvalidAttributeException, Exception
	{
		GKInstance mockInstance = Mockito.mock(GKInstance.class);
		
		Mockito.when(mockInstance.getAttributeValue(ReactomeJavaConstants.accessUrl)).thenReturn(accessUrl);
		Mockito.when(mockInstance.getAttributeValue("resourceIdentifier")).thenReturn("MIR:123456");
		Mockito.when(mockInstance.getDisplayName()).thenReturn("Some Resource");
		Mockito.when(mockInstance.getDBID()).thenReturn(1234L);
		return mockInstance;
	}

	private void setupHttpResponse(int responseCode) throws IOException, ClientProtocolException
	{
		Mockito.when(mockStatusLine.getStatusCode()).thenReturn(responseCode);
		Mockito.when(mockResponse.getEntity()).thenReturn(entity);
		Mockito.when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
		Mockito.when(mockClient.execute(Mockito.any(HttpUriRequest.class))).thenReturn(mockResponse);
		Mockito.when(mockClient.execute(Mockito.any(HttpUriRequest.class), (HttpContext) Mockito.any(HttpContext.class))).thenReturn(mockResponse);
		
		Mockito.when(HttpClients.createDefault()).thenReturn(mockClient);
	}

	private void printReport(QAReport report)
	{
		for (List<String> line : report.getReportLines())
		{
			for (String s: line)
			{
				System.out.print(s + "\t");
			}
			System.out.println("\n");
		}
	}
}
