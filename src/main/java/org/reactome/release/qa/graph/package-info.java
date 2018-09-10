
/**
 * Test classes in this package are ported from Graph QA checks and have been consolidated 
 * into several group checks as following:
 * 
 * MultipleAttributesMissingCheck: T002_PersonWithoutProperName,  
 *                                 T010_EntitySetWithoutMemberOrCandidate
 *                                 T024_ReferenceDatabaseWithoutUrls
 *                                 
 * Mandatory attribute check (Not provided in this package): T009_ComplexWithoutComponents, 
 *                                                           T006_EwasWithoutReferenceEntity,
 *                                                           T011_PolymerWithoutRepeatedUnit
 *                                                           T012_SimpleEntityWithoutReferenceEntity,
 *                                                           T013_ReferenceEntityWithoutIdentifier
 *                                                           T015_CatalystActivityWithoutPhysicalEntity
 *                                                           T017_NOT_FailedReactionsWithoutOutputs (BlackBoxEvent check has been registered for SingleAttributeMissingCheck)
 *                                                           T019_PublicationsWithoutAuthor
 *                                                           T021_RegulationsWithoutRegulatedEntityOrRegulator (regulatedEntity should not be checked since regulatedBy is used)
 *                                                           T022_PhysicalEntityWithoutCompartment
 *                                                           T023_DatabaseIdentifierWithoutReferenceDatabase
 *                                                           T029_ReactionsLikeEventWithoutInput
 *                                                           
 * Required Attribute check (Not provided in this package): T005_PathwaysWithoutEvents, 
 *                                                          T016_CatalystActivityWithoutActivity
 *                                                          T020_OpenSetsWithoutReferenceEntity
 *                                                          
 * SingleAttributeMissingCheck: T007_EntitiesWithoutStId
 *                              T014_InstanceEditWithoutAuthor
 *                              
 * InstanceDuplicationCheck: T050_DuplicatedLiteratureReferences
 *                           T059_DuplicatedCandidateSets (Compartment is checked in graph qa, but not here)
 *                           T062_DuplicatedReferenceEntities
 *                           T064_DuplicatedEntitySets (Compartment checked in graph qa, but not here)
 * 
 * MultipleAttributesCrossClassesMissingCheck (This is a less stringent check: as long as there is a value in a referred instance, it is fine.
 * e.g. a Reaction doesn't have inferredFrom and literatureReference filled, but it has two summation instances. However, only one of
 * them has literatureReference filled. It will be treated as good.)
 *                                          T091_ReactionsWithoutLiteratureReference
 * 
 * SingleAttriuteDuplicationCheck:  T034_ModifiedRelationshipDuplication
 *                                  T065_EntitySetsWithRepeatedMembers
 * 
 * SingleAttributeSoleValueCheck: T057_ComplexesWithOnlyOneComponent
 *                                T061_EntitySetsWithOnlyOneMember
 *                                
 * TwoAttributesReferToSameCheck: T028_HasMemberAndHasCandidatePointToSameEntry
 *                                T032_PrecedingEventOrReverseReactionOrHasEventPointToSameEntry (slot reverseReaction is not checked here)
 * 
 * InferredFromInOtherAttributeCheck: T025_EntriesWithCyclicInferredToRelations (switch to check duplicated use in inferredFrom and other slots)
 * OtherRelationsThatPointToTheSameEntryTest: T033_OtherRelationsThatPointToTheSameEntryTest
 *                                
 * DatabaseObjectsWithoutCreated: T018_DatabaseObjectsWithoutCreated
 * DatabaseObjectsWithSelfLoops: T001_DatabaseObjectsWithSelfLoops
 * PhysicalEntitiesWithMoreThanOneCompartment: T030_PhysicalEntitiesWithMoreThanOneCompartment (inferred filtering is not applied here)
 * CatalystActivityWherePhysicalEntityAndActiveUnitPointToComplex: T031_CatalystActivityWherePhysicalEntityAndActiveUnitPointToComplex (inferred filtering is not applied in this implementation)
 * PrecedingEventOutputsNotUsedInReaction: T053_PrecedingEventOutputsNotUsedInReaction (inferred filtering is not applied)
 * OrphanEvents: T047_OrphanEvents
 * ReactionsWithOnlyOneInputAndOutputWhereSchemaClassDoNotMatchTest: T071_ReactionsWithOnlyOneInputAndOutputWhereSchemaClassDoNotMatch
 *                                                                   T092_PotentialTranslocationReactionChangesParticipantsSchemaClass (cases here are covered by T071. Therefore not used any more)
 *                                                                      
 * OneHopCircularReferenceCheck: T026_EventsWithCyclicPrecedingEvents (Not implemented explicitly. Included case)
 *                               T027_EntriesWithOtherCyclicRelations
 * 
 * Not ported: T090_CatalystActivityCompartmentDoesNotMatchReactionCompartment (Should be handled by ReactionlikeEvent compartment check)
 *             T058_ComplexesWhereCompartmentDoesNotMatchWithAnyOfTheParticipants (Should be handled by Complex's compartment check)
 *             T092_PotentialTranslocationReactionChangesParticipantsSchemaClass (covered by T071)
 * 
 * @author wug
 *
 */
package org.reactome.release.qa.graph;