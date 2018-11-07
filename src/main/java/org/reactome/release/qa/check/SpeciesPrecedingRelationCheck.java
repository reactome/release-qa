package org.reactome.release.qa.check;

import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QAReport;

/**
 * This QA checks species used in two Event instances via preceding relationship:
 * they should share at least one species in either species or relatedSpecies slot.
 * 
 * This check's escape list can include both preceding and following RLEs.
 * 
 * @author wug
 */
@SuppressWarnings("unchecked")
@SliceQACheck
public class SpeciesPrecedingRelationCheck extends AbstractQACheck {

    public SpeciesPrecedingRelationCheck() {
    }

    @Override
    public String getDisplayName() {
        return "Species_In_Preceding_Event";
    }
    
    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        report.setColumnHeaders("Preceding Event DB_ID", "Preceding Event DisplayName", "Preceding Event Species", "Preceding Event Related Species",
                                "Following Event DB_ID", "Following Event DisplayName", "Following Event Species", "Following Event Related Species");
        Collection<GKInstance> rles = dba.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
        dba.loadInstanceAttributeValues(rles, new String[] {ReactomeJavaConstants.precedingEvent,
                                                            ReactomeJavaConstants.species,
                                                            "relatedSpecies"});
        for (GKInstance rle : rles) {
            if (isEscaped(rle)) {
                continue;
            }
            List<GKInstance> precedingEvents = rle.getAttributeValuesList(ReactomeJavaConstants.precedingEvent);
            if (precedingEvents.size() == 0)
                continue;
            for (GKInstance pEvent : precedingEvents) {
                check(rle, pEvent, report);
            }
        }
        return report;
    }
    
    private void check(GKInstance rle, GKInstance preRLE, QAReport report) throws Exception {
        List<GKInstance> rleSpecies = rle.getAttributeValuesList(ReactomeJavaConstants.species);
        List<GKInstance> preSpecies = preRLE.getAttributeValuesList(ReactomeJavaConstants.species);
        List<GKInstance> rleRelatedSpecies = rle.getAttributeValuesList("relatedSpecies");
        List<GKInstance> preRelatedSpecies = preRLE.getAttributeValuesList("relatedSpecies");
        if (rleSpecies.size() == 1 &&
            preSpecies.size() == 1 &&
            (rleSpecies.get(0).equals(preSpecies.get(0)) || 
             preRelatedSpecies.contains(rleSpecies.get(0)) ||
             rleRelatedSpecies.contains(preSpecies.get(0))))
            return; // There is nothing to be report. This is good!
        // Either RLE can be escaped.
        if (isEscaped(rle) || isEscaped(preRLE)) {
            return;
        }
        report.addLine(preRLE.getDBID().toString(),
                       preRLE.getDisplayName(),
                       join(preSpecies),
                       join(preRelatedSpecies),
                       rle.getDBID().toString(),
                       rle.getDisplayName(),
                       join(rleSpecies),
                       join(rleRelatedSpecies));
    }
    
    private String join(List<GKInstance> species) {
        if (species == null || species.size() == 0)
            return "";
        StringBuilder builder = new StringBuilder();
        species.forEach(inst -> builder.append(inst.getDisplayName()).append("|"));
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }
    
}
