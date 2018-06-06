# ReleaseQA
A collection of tools to perform QA checks during the Release process.

### Notes added by Guanming: 

* Contributors check has been removed. It should be regarded as release report.
* unreleased_instances_with_released_stable_id: This should be regarded as release report. It has not been ported into Java.

### QAs provided in this project, which should be run against the slice database

* ReactionlikeEvent_Species_Chimeric: Check the usage of isChimeric and species assignment
* FailedReaction_NormalReaction_Output: FailedReaction should have normalReaction filled and no output
* Human_Event_Not_In_Hierarchy: Make sure human events are listed in the pathway hierarchy always.
* New_Event_QA: Check newly released Events for several related issues, which should output if any.
* PhysicalEntity_Container_Species: Make sure the usage of species is correct between container PE (e.g. Complex or EntitySet) and its contained PEs.
* ReactionLikeEvent_Multiple_Check: Several checks related to ReactionlikeEvents, including null inputs, outputs, and use of normalReaction slot and disease assignment. This check should be split in the future.
* SimpleEntity_With_Species: Get a list of SimpleEntity having no-null species.
* Species_in_Preceding_Event: Make sure two Events linked by preceding/following relationship have shared species in species or relatedSpecis slot.
* StableIdentifier_Identifier: Make sure StableIdentifiers don't have empty or duplicated identifier values, and a StableIdentifier instance is not used by more than one Instance. 

### QA checks to run against the Release database

* CompareSpeciesbyclasses: This check will compare the current release database to the previous release database (for example: `test_reactome_65` and `test_reactome_64`) and compare the number of instances of each class type. When the percent difference is > 10% (where the actual difference is > 1000), the difference will be highlighted as `*** Difference is N% ***` in the output report.
This QA check requires additional config settings in `auth.properties` to connect tot he previous release database. These properties are: `altDbHost`, `altDbName`, `altDbUser`, `altDbPwd`. 

### Running the code
You can run a specific check like this:

```
java -Xmx8G -jar release-qa-0.1.0-jar-with-dependencies.jar
```

This will run a set of QAs for release. Before running the above, make sure you have edited `resources/auth.properties` to provide required database connection information. All outputs will be generated in the output folder.

To use skip lists for ReactionlikeEvent checks, place skip lists in the resources folder. The file names are expected as following:

* rleCompartmentSkipList.txt
* rleInputSkipList.txt
* rleOutputSkipList.txt

By default, the *Slice* QA checks will run. To run the *Release* QA checks, add this line `testType=ReleaseQATest` to the `auth.properties` file which contains the database connection info.