package org.reactome.release.qa.common;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class DelimitedTextReport extends QAReport
{
	private boolean printSeparatorLine = false;
	private String delimiter = "\t" ;
	StringBuilder sb = new StringBuilder();
	private OutputStream outStream ;
	
	
	public void setPrintSeperatorLine(boolean printSep)
	{
		this.printSeparatorLine = printSep;
	}
	
	/**
	 * Prints the header, and an optional seperator line.
	 */
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
		if (printSeparatorLine)
		{
			char[] chars = new char[this.sb.toString().length() ];
			Arrays.fill(chars, '-');
			String text = new String(chars);
			this.sb.append(text).append("\n");
		}
	}

	/**
	 * Prints a report to a specified output stream. Values will be separated with a delimiter character.
	 * @param delim - The delimiter character.
	 * @param outStream - The outputstream. Set this to System.out if you want to print to stdout.
	 * @throws IOException
	 * @throws ReportException
	 */
	public void print(String delim, OutputStream outStream) throws IOException
	{
		this.delimiter = delim;
		this.outStream = outStream;
		this.print();
	}
	
	/**
	 * Print the report.
	 */
	@Override
	public void print() throws IOException
	{
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
}
