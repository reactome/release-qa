package org.reactome.qa.nullCheck;

import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;

public class NullCheck
{

	private  static GKInstance getSomeAttribute(GKInstance inst, String attrName) throws InvalidAttributeException, Exception
	{
		return ((GKInstance) inst.getAttributeValue(attrName));
	}
	
	public static void main(String[] args)
	{
		try
		{
			MySQLAdaptor currentDBA = new MySQLAdaptor("localhost", "test_reactome_64", "root", "root", 3306);
			Collection<GKInstance> simpleEntities = currentDBA.fetchInstanceByAttribute("SimpleEntity", "species", "IS NOT NULL", null);
			if (simpleEntities.size() > 0)
			{
				//TODO: Switch to better logging framework than stdout.
				System.out.println("There are "+simpleEntities.size()+" SimpleEntities with a non-null species. Details are:");
				for (GKInstance se : simpleEntities)
				{
					int modSize = se.getAttributeValuesList("modified").size();
					GKInstance mostRecentMod = (GKInstance) se.getAttributeValuesList("modified").get(modSize-1);
					System.out.println("DB_ID: " + se.getDBID() + "_displayName: " + se.getDisplayName()
									+ " Last modified: " + mostRecentMod.getDisplayName()
									+ " Species: " + getSomeAttribute(se,"species").getDBID() + "/" + getSomeAttribute(se,"species").getDisplayName());
				}
			}
			else
			{
				System.out.print("SimpleEntities with non-null species: there are none! :)");
			}
			
			Collection<GKInstance> physicalEntities = currentDBA.fetchInstanceByAttribute("PhysicalEntity", "compartment", "IS NULL", null);
			if (physicalEntities.size() > 0)
			{
				System.out.println("There are "+physicalEntities.size()+" PhysicalEntities with a null compartment. Details are:");
				for (GKInstance pe : physicalEntities)
				{
					int modSize = pe.getAttributeValuesList("modified").size();
					GKInstance mostRecentMod = (GKInstance) pe.getAttributeValuesList("modified").get(modSize-1);
					System.out.println("DB_ID: " + pe.getDBID() + "_displayName: " + pe.getDisplayName()
									+ " Last modified: " + mostRecentMod.getDisplayName());
				}
			}
			else
			{
				System.out.print("PhysicalEntities with non compartments: there are none! :)");
			}
			
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
