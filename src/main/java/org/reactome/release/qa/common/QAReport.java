package org.reactome.release.qa.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class QAReport
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
	 * Add lines in bulk.
	 * @param lines
	 */
	public void addLines(List<List<String>> lines)
	{
		for (List<String> line : lines)
		{
			this.addLine(line);
		}
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
	
	/**
	 * Gets an unmodifiable list of headers.
	 * @return an unmodifiable list of headers.
	 */
	public List<String> getHeaders()
	{
		return Collections.unmodifiableList(this.columnHeaders);
	}
	
	/**
	 * Gets an unmodifiable list of report lines.
	 * @return an unmodifiable list of report lines.
	 */
	public List<List<String>> getReportLines()
	{
		return Collections.unmodifiableList(this.reportLines);
	}
	
	public abstract void print() throws IOException;
	protected abstract void printHeader();
}
