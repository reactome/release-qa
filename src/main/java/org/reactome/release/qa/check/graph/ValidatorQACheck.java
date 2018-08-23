package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.function.Consumer;

import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.check.graph.Validator.Invalid;
import org.reactome.release.qa.common.QAReport;

public class ValidatorQACheck extends AbstractQACheck {

    private Validator validator;
    
    public ValidatorQACheck(Validator validator) {
        this.validator = validator;
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        Consumer<Invalid> consumer = new Consumer<Validator.Invalid>() {
            
            @Override
            public void accept(Invalid invalid) {
                addReportLine(report, invalid);
            }

        };
        this.validator.validate(dba, consumer);
        
        return report;
    }

    @Override
    public String getDisplayName() {
        return validator.getName();
    }

    private void addReportLine(QAReport report, Invalid invalid) {
        report.addLine(
                Arrays.asList(invalid.instance.getDBID().toString(), 
                        invalid.instance.getDisplayName(), 
                        invalid.instance.getSchemClass().getName(), 
                        invalid.issue,  
                        QACheckerHelper.getLastModificationAuthor(invalid.instance)));
    }

}
