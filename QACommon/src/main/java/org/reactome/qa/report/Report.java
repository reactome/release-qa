package org.reactome.qa.report;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.reactome.qa.report.exception.ReportException;

public abstract class Report
{
	protected List<List<String>> reportLines = new ArrayList<List<String>>();
	protected List<String> columnHeaders = new ArrayList<String>();

	
	/**
	 * Add a line to the report.
	 * @param line - a list of values that makes up the line.
	 */
	public void addLine(List<String> line)
	{
		this.reportLines.add(line);
	}
	
	/**
	 * Sets the headers for the report.
	 * @param headers - a list of headers.
	 */
	public void setColumnHeaders(List<String> headers)
	{
		this.columnHeaders = headers;
	}
	
	/**
	 * Adds a header to the headers.
	 * @param header - the header to add.
	 */
	public void addHeader(String header)
	{
		this.columnHeaders.add(header);
	}
	
	public abstract void print() throws IOException, ReportException;
	protected abstract void printHeader();
	public abstract void setOutput(OutputStream outStream);
}
