package ca.ubc.cs.datastore;

import weka.attributeSelection.AttributeSelection;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;

public class CrossValidateResult implements Comparable<CrossValidateResult>{
    private double matricValue;
    private String crossValidationString;
    private String resultString;
    private boolean isComplete;
    private AbstractClassifier classifier;
    private String[] classifierArgs;
    private Evaluation evaluation;
    private AttributeSelection attributeSelection;

    public double getMatricValue() {
        return matricValue;
    }

    public void setMatricValue(double matricValue) {
        this.matricValue = matricValue;
    }

    public String getCrossValidationString() {
        return crossValidationString;
    }

    public void setCrossValidationString(String crossValidationString) {
        this.crossValidationString = crossValidationString;
    }

    @Override
    public int compareTo(CrossValidateResult o) {
        if(o == null)
            return 0;
        return Double.compare(matricValue, o.getMatricValue());
    }

    public String getResultString() {
        return resultString;
    }

    public void setResultString(String resultString) {
        this.resultString = resultString;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public AbstractClassifier getClassifier() {
        return classifier;
    }

    public void setClassifier(AbstractClassifier classifier) {
        this.classifier = classifier;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public AttributeSelection getAttributeSelection() {
        return attributeSelection;
    }

    public void setAttributeSelection(AttributeSelection attributeSelection) {
        this.attributeSelection = attributeSelection;
    }

    public String[] getClassifierArgs() {
        return classifierArgs;
    }

    public void setClassifierArgs(String[] classifierArgs) {
        this.classifierArgs = classifierArgs;
    }
}