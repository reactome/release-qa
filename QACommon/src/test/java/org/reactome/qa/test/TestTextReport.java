package org.reactome.qa.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.exception.ReportException;

public class TestTextReport
{

	@Test
	public void testReportOK() throws IOException, ReportException
	{
		DelimitedTextReport testReport = new DelimitedTextReport();
		testReport.setColumnHeaders( new ArrayList<String>(Arrays.asList("Header A","Header 2")) );
		testReport.addHeader("Header_C");
		
		testReport.addLine(Arrays.asList("value1", "value2", "value3"));
		testReport.addLine(Arrays.asList("1234", "5.6778", "930495898983"));
		
		
		testReport.print(",",System.out);
		
		OutputStream outStream = new ByteArrayOutputStream();
		testReport.print(",", outStream);
		String expectedOutput = "Header A,Header 2,Header_C\n" + 
		"---------------------------\n" + 
		"value1,value2,value3\n" + 
		"1234,5.6778,930495898983\n";
		
		assertEquals(expectedOutput, outStream.toString());
	}

	@Test
	public void testReportNotOK() throws IOException
	{
		DelimitedTextReport testReport = new DelimitedTextReport();
		testReport.setColumnHeaders( new ArrayList<String>(Arrays.asList("Header A","Header 2")) );
		testReport.addHeader("Header_C");
		
		testReport.addLine(Arrays.asList("value1", "value2", "value3"));
		testReport.addLine(Arrays.asList("1234", "5.6778", "930495898983"));
		
		
		try
		{
			testReport.print(null, System.out);
		}
		catch (ReportException e)
		{
			e.printStackTrace();
			assertEquals("Delimiter is null. A delimeter character MUST be specified when printing a delimited text report.", e.getMessage());
		}
		
	}
}
