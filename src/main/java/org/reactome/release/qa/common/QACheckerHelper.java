package org.reactome.release.qa.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

public class QACheckerHelper {
    
    public static final String IS_NOT_NULL = "IS NOT NULL";
    public static final String IS_NULL = "IS NULL";

    // Variables for CoV-specific QA tests, from the CoV-1-to-CoV-2 projection process (August 2020).
    public static final long COV_2_INFECTION_PATHWAY_DB_ID = 9694516L;
    public static final long COV_1_SPECIES_DB_ID = 9678119L;
    public static final long COV_2_SPECIES_DB_ID = 9681683L;
    public static final long COV_1_DISEASE_DB_ID = 9678120L;
    public static final long COV_2_DISEASE_DB_ID = 9683912L;
    
    /**
     * Filter a list of DB ids by the DB ids in skipList.
     * Any object whose DB_ID is in skipList will *not* be in the output.
     *
     * @param skipList the skipList
     * @param dbIds thd DB ids to filter
     * @return
     */
    public static Collection<Long> filterDbIdsBySkipList(Collection<Long> skipList, Collection<Long> dbIds)
    {
        if (skipList == null || skipList.isEmpty()) {
            return dbIds;
        } else {
            return dbIds.parallelStream()
                    .filter(dbId -> !skipList.contains(dbId))
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * Filter a list of GKInstance objects by the DB IDs in skipList.
     * @param skipList - the skipList.
     * @param instances - Objects from the database. Any object whose DB_ID is in skipList will *not* be in the output.
     * @return
     */
    public static Collection<GKInstance> filterBySkipList(Collection<Long> skipList, Collection<GKInstance> instances)
    {
        if (skipList != null && !skipList.isEmpty())
        {
            return instances.parallelStream().filter(inst -> !skipList.contains(inst.getDBID())).collect(Collectors.toList());
        }
        else
        {
            return instances;
        }
    }
    
    public static boolean isChimeric(GKInstance rle) throws Exception {
        if (!rle.getSchemClass().isValidAttribute(ReactomeJavaConstants.isChimeric))
            return false;
        Boolean value = (Boolean) rle.getAttributeValue(ReactomeJavaConstants.isChimeric);
        if (value == null || !value)
            return false;
        return true;
    }
    
    @SuppressWarnings("unchecked")
    public static GKInstance getHuman(MySQLAdaptor dba) throws Exception {
        Collection<GKInstance> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Species,
                ReactomeJavaConstants._displayName,
                "=",
                "Homo sapiens");
        if (c == null || c.size() == 0)
            throw new IllegalStateException("Cannot find species Homo sapiens in the database, " +
                                            dba.getDBName() + "@" + dba.getDBHost());
        return c.iterator().next();
    }
    
    public static List<Long> getSkipList(String filePath) throws IOException
    {
        List<Long> skipList = new ArrayList<Long>();
        if (filePath == null)
            return skipList;
        Files.readAllLines(Paths.get(filePath)).forEach(line -> {
            Long dbId = Long.parseLong(line.split("\t")[0]);
            skipList.add(dbId);
        });
        return skipList;
    }
    
    public static String getLastModificationAuthor(GKInstance instance)
    {
        final String noAuthor = "No modification or creation author";
        
        GKInstance mostRecentMod = null;
        try
        {
            @SuppressWarnings("unchecked")
            List<GKInstance> modificationInstances = (List<GKInstance>) instance.getAttributeValuesList("modified");
            if (modificationInstances.size() > 0)
            {
                List<Long> developers = QACheckProperties.getDeveloperDbIds();
                for (int index = modificationInstances.size() - 1; index >= 0; index--)
                {
                    GKInstance modificationInstance = modificationInstances.get(index);
                    GKInstance author = (GKInstance) modificationInstance.getAttributeValue("author");
                    // Skip modification instance for developers.
                    if (author != null && !developers.contains(author.getDBID()))
                    {
                        mostRecentMod = modificationInstance;
                        break;
                    }
                }
            }
        }
        catch (InvalidAttributeException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        if (mostRecentMod == null)
        {
            GKInstance created = null;
            try
            {
                created = (GKInstance) instance.getAttributeValue("created");
            }
            catch (InvalidAttributeException e)
            {
                e.printStackTrace();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            
            if (created != null)
            {
                return created.getDisplayName();
            }
            else
            {
                return noAuthor;
            }
        }
        
        return mostRecentMod.getDisplayName();    
    }
    
    public static GKInstance getLastModification(GKInstance instance)
    {
        try
        {
            @SuppressWarnings("unchecked")
            List<GKInstance> modificationInstances = (List<GKInstance>) instance.getAttributeValuesList("modified");
            if (modificationInstances.size() > 0)
            {
                for (int index = modificationInstances.size() - 1; index >= 0; index--)
                {
                    GKInstance modificationInstance = modificationInstances.get(index);
                    GKInstance author = (GKInstance) modificationInstance.getAttributeValue("author");
                    // Skip modification instance for Solomon, Joel, or Guanming
                    if (author == null || Arrays.asList("8939149", "1551959", "140537").contains(author.getDBID().toString()))
                    {
                        continue;
                    }
                    return modificationInstance;
                }
            }
        }
        catch (InvalidAttributeException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        GKInstance created = null;
        try
        {
            created = (GKInstance) instance.getAttributeValue("created");
        }
        catch (InvalidAttributeException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        return created == null ? null : created;
    }
    
    public static int componentsHaveSpecies(GKInstance physicalEntity) throws Exception
    {
        Set<GKInstance> speciesSet = QACheckerHelper.grepAllSpeciesInPE(physicalEntity, true);
        //return !speciesSet.isEmpty();
        return !speciesSet.isEmpty() ? speciesSet.size() : 0;
    }
    
    static Set<GKInstance> grepAllSpeciesInPE(GKInstance pe, boolean needRecursion) throws Exception
    {
        Set<GKInstance> speciesSet = new HashSet<>();
        if (pe.getSchemClass().isValidAttribute(ReactomeJavaConstants.species))
        {
            @SuppressWarnings("unchecked")
            List<GKInstance> speciesList = (List<GKInstance>)pe.getAttributeValuesList(ReactomeJavaConstants.species);
            if (speciesList != null && speciesList.size() > 0)
            {
                speciesSet.addAll(speciesList);
            }
        }
        if (speciesSet.size() == 0 && needRecursion)
        {
            QACheckerHelper.grepAllSpeciesInPE(pe, speciesSet);
        }
        return speciesSet;
    }

    static void grepAllSpeciesInPE(GKInstance pe, Set<GKInstance> speciesSet) throws Exception
    {
        Set<GKInstance> wrappedPEs = InstanceUtilities.getContainedInstances(pe,
                ReactomeJavaConstants.hasComponent,
                ReactomeJavaConstants.hasCandidate,
                ReactomeJavaConstants.hasMember,
                ReactomeJavaConstants.repeatedUnit);
        for (GKInstance wrappedPE : wrappedPEs)
        {
            Set<GKInstance> wrappedSpecies = QACheckerHelper.grepAllSpeciesInPE(wrappedPE, true);
            speciesSet.addAll(wrappedSpecies);
        }
    }
    
    public static Collection<GKInstance> getInstancesWithNullAttribute(MySQLAdaptor dba, String schemaClass, String attribute, Collection<Long> skipList)
    {
        return getInstances(dba, schemaClass, attribute, IS_NULL, skipList);
    }
    
    public static Collection<GKInstance> getInstancesWithNonNullAttribute(MySQLAdaptor dba, String schemaClass, String attribute, Collection<Long> skipList)
    {
        return getInstances(dba, schemaClass, attribute, IS_NOT_NULL, skipList);
    }
    
    @SuppressWarnings("unchecked")
    public static Collection<GKInstance> getInstances(MySQLAdaptor dba, String schemaClass, String attribute, String operator, Collection<Long> skipList)
    {
        List<GKInstance> instances = new ArrayList<GKInstance>();
        try
        {
            instances.addAll(dba.fetchInstanceByAttribute(schemaClass, attribute, operator, null));
            return QACheckerHelper.filterBySkipList(skipList, instances);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return instances;
    }
    
    /**
     * A generic method to get the table for an attribute in a specified class.
     * @param clsName
     * @param attributeName
     * @param dba
     * @return
     * @throws Exception
     */
    public static String getAttributeTableName(String clsName,
                                               String attributeName,
                                               MySQLAdaptor dba) throws Exception {
        Schema schema = dba.fetchSchema();
        SchemaClass cls = schema.getClassByName(clsName);
        SchemaAttribute attribute = cls.getAttribute(attributeName);
        SchemaClass originCls = attribute.getOrigin();
        if (attribute.isMultiple())
            return originCls.getName() + "_2_" + attribute.getName();
        else
            return originCls.getName();
    }

    /**
     * Finds all Events in DB that are not used for manual inference, or members of Pathways in skiplist. Finding if
     * manually inferred is done by checking for a null 'inferredFrom' referral.
     * @param dba MySQLAdaptor
     * @param skiplistDbIds List<String> -- List of Pathway DbIds. If an Event being checked is a member of these pathways, they are skipped.
     * @return Set<GKInstance> -- All Events in DB that are not used for manual inference.
     * @throws Exception -- Thrown by MySQLAdaptor
     */
    public static Set<GKInstance> findEventsNotUsedForManualInference(MySQLAdaptor dba, List<String> skiplistDbIds) throws Exception {
        Set<GKInstance> eventsNotUsedForInference = new HashSet<>();
        Collection<GKInstance> events = dba.fetchInstancesByClass(ReactomeJavaConstants.Event);
        for (GKInstance event : events) {
            if (!manuallyInferred(event) && !memberSkipListPathway(event, skiplistDbIds)) {
                eventsNotUsedForInference.add(event);
            }
        }
        return eventsNotUsedForInference;
    }

    /**
     * Checks if incoming Event is manually inferred by checking inferredFrom referral.
     * This method only correctly identifies manual inferences in a curation or slice database.
     * This is due to the fact that it only checks the 'inferredFrom' attribute, which is used
     * in automatic inferences as well.
     * @param event GKInstance -- Event instance being checked for inferredFrom referral.
     * @return boolean -- true if inferredFrom referral exists, false if not.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    private static boolean manuallyInferred(GKInstance event) throws Exception {
        return event.getReferers(ReactomeJavaConstants.inferredFrom) != null;
    }

    /**
     * Finds all Events not used for Inference, and then finds subset that are Human ReactionlikeEvents
     * @param dba MySQLAdaptor
     * @param skiplistDbIds List<String> -- List of Pathway DbIds. If an Event being checked is a member of these pathways, they are skipped.
     * @return Set<GKInstance> -- All Human ReactionlikeEvents that are not used for manual inference.
     * @throws Exception-- Thrown by MySQLAdaptor
     */
    public static Set<GKInstance> findHumanReactionsNotUsedForManualInference(MySQLAdaptor dba, List<String> skiplistDbIds) throws Exception {
        Set<GKInstance> reactionsNotUsedForManualInference = new HashSet<>();
        for (GKInstance event : findEventsNotUsedForManualInference(dba, skiplistDbIds)) {
            // Filter for Human ReactionlikeEvents
            if (event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)
                    && isHumanDatabaseObject(event)) {

                reactionsNotUsedForManualInference.add(event);
            }
        }
        return reactionsNotUsedForManualInference;
    }

    /**
     * This method utilizes the InstanceUtilities method for finding all reaction participants. It also checks for the
     * 'activeUnit' participant in catalysts/regulations that can be flagged by certain QA checks. Generally speaking,
     * the 'activeUnit' instance should be found in the 'physicalEntity' of catalysts/regulations. Sometimes this is not
     * the case, when the wrong activeUnit is mistakenly checked in. This will be flagged by QA tests that call this method.
     * @param reaction GKInstance -- ReactionlikeEvent that will be checked for all participants.
     * @return Set<GKInstance> -- All distinct participants in the incoming ReactionlikeEvent.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    public static Set<GKInstance> getAllReactionParticipantsIncludingActiveUnits(GKInstance reaction) throws Exception {
        Set<GKInstance> reactionPEs = new HashSet<>();
        reactionPEs.addAll(InstanceUtilities.getReactionParticipants(reaction));
        // Retrieve activeUnit PEs from Reaction Catalysts/Regulations, if present.
        reactionPEs.addAll(getContainedActiveUnits(reaction));

        Set<GKInstance> allReactionPEs = new HashSet<>();
        for (GKInstance reactionPE : reactionPEs) {
            allReactionPEs.add(reactionPE);
            allReactionPEs.addAll(getPhysicalEntityContainedInstances(reactionPE));
        }
        return allReactionPEs;
    }

    /**
     * This method returns all instances found in the 'activeUnit' slot of the incoming Reaction's 'catalystActivity'
     * and 'regulatedBy' instances, if they exist.
     * @param reaction GKInstance -- ReactionlikeEvent that will be checked for all activeUnit instances.
     * @return Set<GKInstance> -- All distinct activeUnit participants in the incoming ReactionlikeEvent.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    private static Set<GKInstance> getContainedActiveUnits(GKInstance reaction) throws Exception {
        Set<GKInstance> reactionCatalystsAndRegulations = new HashSet<>();
        reactionCatalystsAndRegulations.addAll(reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity));
        reactionCatalystsAndRegulations.addAll(reaction.getAttributeValuesList(ReactomeJavaConstants.regulatedBy));

        Set<GKInstance> activeUnits = new HashSet<>();
        for (GKInstance instance : reactionCatalystsAndRegulations) {
            activeUnits.addAll(InstanceUtilities.getContainedInstances(
                    instance,
                    ReactomeJavaConstants.activeUnit
            ));
        }
        return activeUnits;
    }

    /**
     * This method is wrapper for the InstanceUtilities getContainedInstances method.
     * @param reactionPE GKInstance -- PhysicalEntity that will be checked for contained instances.
     * @return Set<GKInstance> -- All distinct contained instances in the incoming PhysicalEntity.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    public static Set<GKInstance> getPhysicalEntityContainedInstances(GKInstance reactionPE) throws Exception {
        return InstanceUtilities.getContainedInstances(
                reactionPE,
                ReactomeJavaConstants.hasMember,
                ReactomeJavaConstants.hasCandidate,
                ReactomeJavaConstants.hasComponent,
                ReactomeJavaConstants.repeatedUnit
        );
    }

    /**
     * Checks if incoming DatabaseObject (Event or PhysicalEntity) has single Homo sapiens species.
     * @param databaseObject GKInstance -- Event or PhysicalEntity instance being checked for only Homo sapiens species.
     * @return boolean -- true if databaseObject only has a single, Homo sapiens species instance, false if not.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    public static boolean isHumanDatabaseObject(GKInstance databaseObject) throws Exception {

        List<GKInstance> objectSpecies = databaseObject.getSchemClass().isValidAttribute(ReactomeJavaConstants.species) ?
                databaseObject.getAttributeValuesList(ReactomeJavaConstants.species) : Collections.emptyList();
        return objectSpecies.size() == 1 && hasHumanSpecies(objectSpecies);
    }

    /**
     * Checks if the incoming DatabaseObject (Event or PhysicalEntity) is non-human.
     * @param databaseObject GKInstance -- Event or PhysicalEntity to be checked for non-human species attribute.
     * @return boolean -- true if instance has species, and if none of the species are human, false if not.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    public static boolean hasOnlyNonHumanSpecies(GKInstance databaseObject) throws Exception {
        // Check if species is a valid attribute for physicalEntity.
        return hasSpeciesAttribute(databaseObject)
                && databaseObject.getAttributeValue(ReactomeJavaConstants.species) != null
                && !databaseObject.getAttributeValuesList(ReactomeJavaConstants.species).isEmpty()
                && !hasHumanSpecies(databaseObject.getAttributeValuesList(ReactomeJavaConstants.species));
    }

    /**
     * Checks to see if any of the Species instances in incoming list are Homo sapiens.
     * @param objectSpecies List<GKInstance> -- List of Species instances from a DatabaseObject (PhysicalEntity or ReactionlikeEvent)
     * @return boolean -- true if any of the Species instance display names are equal to Homo sapiens.
     */
    private static boolean hasHumanSpecies(List<GKInstance> objectSpecies) {
        return objectSpecies.stream().anyMatch(species -> species.getDisplayName().equals("Homo sapiens"));
    }

    /**
     * Checks if species is a valid attribute in the incoming PhysicalEntity.
     * @param physicalEntity GKInstance -- PhysicalEntity that is being checked to see if species is a valid attribute.
     * @return boolean -- true if species is a valid attribute, false if not.
     */
    private static boolean hasSpeciesAttribute(GKInstance physicalEntity) {
        return physicalEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.species);
    }

    /**
     * Helper method that finds all unique DbIds in the 'species' and 'relatedSpecies' attributes of the instance.
     * @param inst - GKInstance, an Event or PhysicalEntity.
     * @return - Set<Long>, DbIds from species/relatedSpecies attributes.
     * @throws Exception, thrown by MySQLAdaptor.
     */
    public static Set<Long> getSpeciesAndRelatedSpeciesDbIds(GKInstance inst) throws Exception {
        Set<Long> speciesDbIds = new HashSet<>();
        if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)) {
            for (GKInstance speciesInst : (Collection<GKInstance>) inst.getAttributeValuesList(ReactomeJavaConstants.species)) {
                speciesDbIds.add(speciesInst.getDBID());
            }
        }
        if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.relatedSpecies)) {
            for (GKInstance relatedSpeciesInst : (Collection<GKInstance>) inst.getAttributeValuesList(ReactomeJavaConstants.relatedSpecies)) {
                speciesDbIds.add(relatedSpeciesInst.getDBID());
            }
        }
        return speciesDbIds;
    }

    /**
     * Checks if the incoming databaseObject has a popalated disease attribute.
     * @param databaseObject GKInstance -- Instance to be checked for populated disease attribute.
     * @return boolean -- true if has filled disease attribute, false if not.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    public static boolean hasDisease(GKInstance databaseObject) throws Exception {
        return databaseObject.getSchemClass().isValidAttribute(ReactomeJavaConstants.disease)
                && databaseObject.getAttributeValue(ReactomeJavaConstants.disease) != null;
    }

    /**
     * Finds all parent DbIds of the incoming Event, and then checks if any of them are in the skiplist of Pathway DbIds.
     * @param event GKInstance -- Event that is being checked for membership in a skiplist Pathway.
     * @param skiplistDbIds List<String> -- List of Pathway DbIds. If an Event being checked is a member of these pathways, they are skipped.
     * @return boolean -- true if member of skiplist Pathway, false if not.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    public static boolean memberSkipListPathway(GKInstance event, List<String> skiplistDbIds) throws Exception {
        // Finds all parent Event DbIds.
        Set<String> hierarchyDbIds = findEventHierarchyDbIds(event);
        // Check if any returned Event DbIds (including original Events) are in skiplist.
        return skiplistDbIds.stream().anyMatch(dbId -> hierarchyDbIds.contains(dbId));
    }

    /**
     * Finds parent DbIds of incoming Event through hasEvent referrers. Recurses until there are no more referrers.
     * @param event GKInstance -- Event that is being checked for referrers. Its DbId is added to the Set being built.
     * @return Set<String> -- Once TopLevelPathway has been found, returns all DbIds, inclusive, between TopLevelPathway and original Event.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    private static Set<String> findEventHierarchyDbIds(GKInstance event) throws Exception {
        Set<String> dbIds = new HashSet<>();
        dbIds.add(event.getDBID().toString());
        Collection<GKInstance> hasEventReferrals = event.getReferers(ReactomeJavaConstants.hasEvent);
        if (hasEventReferrals != null) {
            for (GKInstance hasEventReferral : hasEventReferrals) {
                dbIds.addAll(findEventHierarchyDbIds(hasEventReferral));
            }
        }
        return dbIds;
    }

    /**
     * Reads skiplist file that contains Pathway DbIds that should not be included in QA check.
     * @return List<String> -- List of DbIds.
     */
    public static List<String> getNonHumanPathwaySkipList() throws IOException {
        return Files.readAllLines(Paths.get("resources/manually_curated_nonhuman_pathways_skip_list.txt"));
    }

    /**
     *  Helper method checks that checks if the attribute exists in the incoming instance. Checks for either
     *  created or species values. Returns displayName or, if attribute instance doesn't exist,  null.
     * @param instance GKInstance -- Instance being checked for an attribute.
     * @param attribute String -- Attribute value that will be checked in the incoming instance.
     * @return String -- Either the attribute instance's displayName, or null.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    public static String getInstanceAttributeNameForOutputReport(GKInstance instance, String attribute) throws Exception {
        GKInstance attributeInstance = null;
        if (instance.getSchemClass().isValidAttribute(attribute)) {
            if (attribute.equals(ReactomeJavaConstants.relatedSpecies) || attribute.equals(ReactomeJavaConstants.species)) {
                List<String> relatedSpeciesNames = new ArrayList<>();
                for (GKInstance relatedSpeciesInst : (Collection<GKInstance>) instance.getAttributeValuesList(attribute)) {
                    relatedSpeciesNames.add(relatedSpeciesInst.getDisplayName());
                }
                return relatedSpeciesNames.size() > 0 ? String.join("|",relatedSpeciesNames) : "N/A";
            }
            attributeInstance = (GKInstance) instance.getAttributeValue(attribute);
        }
        return attributeInstance != null ? attributeInstance.getDisplayName() : "N/A";
    }
}
