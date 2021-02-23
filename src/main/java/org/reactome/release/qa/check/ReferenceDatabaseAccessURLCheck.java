package org.reactome.release.qa.check;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QAReport;


/**
 * This check will look at the ReferenceDatabase objects and then check that their URLs are correct, as per
 * the URLs in identifiers.org - this depends on having a list of identifiers for each ReferenceDatabase for identifiers.org
 * For example: HapMap is identified at identifiers.org as "MIR:00100374".
 * Resources that are not known to identifiers.org cannot be checked.
 * The report will show which databases have an accesURL that does not match the URL in identifiers.org.
 * @author sshorser
 *
 */
@SliceQACheck
public class ReferenceDatabaseAccessURLCheck extends AbstractQACheck
{
	private static final String REACTOME_IDENTIFIER_TOKEN = "###ID###";
	private static final String IDENTIFIERS_DOT_ORG_IDENTIFIER_TOKEN = "{$id}";
	private static final Logger logger = Logger.getLogger(ReferenceDatabaseAccessURLCheck.class);
	private static final String ACCESS_URL_TOKEN = "urlPattern";
	private static final String RESOURCE_IDENTIFIER_ENDPOINT = "https://registry.api.identifiers.org/restApi/resources/search/findByMirId?mirId=";
	
	/**
	 * Queries identifiers.org with a resource identifier ("MIR:########").
	 * (Copied from AddLinks)
	 * @param resourceIdentifier - Resource identifier which will be queried.
	 * @return The URL to access data the the resource identified by resourceIdentifier
	 */
	private String getAccessUrlForResource(String resourceIdentifier)
	{
		String accessUrl = null;
		
		String url = RESOURCE_IDENTIFIER_ENDPOINT + resourceIdentifier;
		
		try
		{
			HttpGet get = new HttpGet(new URI(url));
			try(CloseableHttpClient client = HttpClients.createDefault();
				CloseableHttpResponse response = client.execute(get))
			{
				int statusCode = response.getStatusLine().getStatusCode();
				String responseString = EntityUtils.toString(response.getEntity());
				switch (statusCode)
				{
					case HttpStatus.SC_OK:
						JsonReader reader = Json.createReader(new StringReader(responseString));
						JsonObject responseObject = reader.readObject();
						// Leave the {$id} in the URL and let the caller replace it.
						accessUrl = responseObject.getString(ACCESS_URL_TOKEN).toString().replaceAll("\"", "");
						break;
					case HttpStatus.SC_NOT_FOUND:
						// For a 404, we want to tell the user specifically what happened. 
						// This is the error code for invalid identifier strings.
						// It should also be accompanied by the message: "Required {prefix}:{identifier}"
						logger.error("Got 404 from identifiers.org for the resource identifier request: \"" + url + "\". You might want to verify that the resource identifier you requested is correct.");
						break;
					default:
						logger.error("Unexpected (non-200) status code: " + statusCode + " Response String is: " + responseString);
						break;
				}
			}
			catch (IOException e)
			{
				logger.error(e);
				e.printStackTrace();
			}
		}
		catch (URISyntaxException e)
		{
			logger.error(e);
			e.printStackTrace();
		}
		
		return accessUrl;
	}
	
	@Override
	public QAReport executeQACheck() throws Exception
	{
		QAReport report = new QAReport();
		report.setColumnHeaders("DB_ID", "diplayName", "oldAccessURL", "newAccessURL");
		
		@SuppressWarnings("unchecked")
		Collection<GKInstance> refDatabases = dba.fetchInstancesByClass(ReactomeJavaConstants.ReferenceDatabase);
		
		for (GKInstance refDB : refDatabases)
		{
//			String resourceIdentifier = (String) refDB.getAttributeValue(ReactomeJavaConstants.resourceIdentifier);
			String resourceIdentifier = (String) refDB.getAttributeValue("resourceIdentifier");
			if (resourceIdentifier != null && !resourceIdentifier.trim().isEmpty() )
			{
				String accessURLFromDB = (String) refDB.getAttributeValue(ReactomeJavaConstants.accessUrl);
				String accessURLFromIdentifiersDotOrg = getAccessUrlForResource(resourceIdentifier);
				
				if (accessURLFromIdentifiersDotOrg != null && !accessURLFromIdentifiersDotOrg.trim().isEmpty())
				{
					accessURLFromIdentifiersDotOrg = accessURLFromIdentifiersDotOrg.replace(IDENTIFIERS_DOT_ORG_IDENTIFIER_TOKEN, REACTOME_IDENTIFIER_TOKEN);
					if (!accessURLFromDB.equals(accessURLFromIdentifiersDotOrg))
					{
						report.addLine(refDB.getDBID().toString(), refDB.getDisplayName(), accessURLFromDB, accessURLFromIdentifiersDotOrg);
					}
				}
			}
			else
			{
				logger.warn("Reference Database " + refDB.toString() + " has no resourceIdentifier, so we cannot update its accessURL from identifiers.org");
			}
		}
		return report;
	}

}
