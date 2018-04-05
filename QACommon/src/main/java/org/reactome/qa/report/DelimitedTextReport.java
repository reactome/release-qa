package org.reactome.qa.report;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.reactome.qa.report.exception.ReportException;

public class DelimitedTextReport extends Report
{

	private String delimiter;
	StringBuilder sb = new StringBuilder();
	private OutputStream outStream ;
	
	/**
	 * Print the report as text, with values delimited by a specific character.
	 * @param delim - the delimiter character.
	 * @throws IOException 
	 * @throws ReportException 
	 */
	public void printDelmitedReport(String delim) throws IOException, ReportException
	{
		this.delimiter = delim;
		this.print();
	}

	@Override
	protected void printHeader()
	{
		
		int i = 0;
		for (String header : this.columnHeaders)
		{
			this.sb.append(header);
			if (i < this.columnHeaders.size()-1)
			{
				this.sb.append(this.delimiter);
			}
			i++;
		}
		this.sb.append("\n");
		
		// Append a separator line as wide as the header. - later, this can be configurable.
		char[] chars = new char[this.sb.toString().length() ];
		Arrays.fill(chars, '-');
		String text = new String(chars);
		this.sb.append(text).append("\n");
	}

	@Override
	public void print() throws IOException, ReportException
	{
		if (this.delimiter == null)
		{
			throw new ReportException("Delimiter is null. A delimeter character MUST be specified when printing a delimited text report.");
		}
		this.sb = new StringBuilder();
		this.printHeader();
		// Append data.
		for (List<String> line : this.reportLines)
		{
			int j = 0;
			for (String dataValue : line)
			{
				sb.append(dataValue);
				if (j < line.size() - 1 )
				{
					sb.append(this.delimiter);
				}
				j++;
			}
			
			sb.append("\n");
		}
		outStream.write(sb.toString().getBytes());
	}

	@Override
	public void setOutput(OutputStream outStream)
	{
		this.outStream = outStream;
	}

}
