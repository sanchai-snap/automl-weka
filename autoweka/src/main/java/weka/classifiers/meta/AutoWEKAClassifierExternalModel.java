package weka.classifiers.meta;

import autoweka.ClassifierResult;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

public class AutoWEKAClassifierExternalModel implements Serializable {
  private String classifierClass;
  
  private String[] classifierArgs = new String[0];
  
  private AbstractClassifier classifier;
  
  private AttributeSelection attributeSelection;
  
  private String attributeSearchClass;
  
  private String[] attributeSearchArgs = new String[0];
  
  private String attributeEvalClass;
  
  private String[] attributeEvalArgs = new String[0];
  
  private int seed;
  
  private int foldNo;
  
  private String resultMetric;
  
  public String[] getClassifierArgs() {
    return this.classifierArgs;
  }
  
  public void setClassifierArgs(String[] classifierArgs) {
    this.classifierArgs = classifierArgs;
  }
  
  public String getAttributeSearchClass() {
    return this.attributeSearchClass;
  }
  
  public void setAttributeSearchClass(String attributeSearchClass) {
    this.attributeSearchClass = attributeSearchClass;
  }
  
  public String[] getAttributeSearchArgs() {
    return this.attributeSearchArgs;
  }
  
  public void setAttributeSearchArgs(String[] attributeSearchArgs) {
    this.attributeSearchArgs = attributeSearchArgs;
  }
  
  public String getAttributeEvalClass() {
    return this.attributeEvalClass;
  }
  
  public void setAttributeEvalClass(String attributeEvalClass) {
    this.attributeEvalClass = attributeEvalClass;
  }
  
  public String[] getAttributeEvalArgs() {
    return this.attributeEvalArgs;
  }
  
  public void setAttributeEvalArgs(String[] attributeEvalArgs) {
    this.attributeEvalArgs = attributeEvalArgs;
  }
  
  public String getClassifierClass() {
    return this.classifierClass;
  }
  
  public void setClassifierClass(String classifierClass) {
    this.classifierClass = classifierClass;
  }
  
  public Classifier getClassifier() {
    return (Classifier)this.classifier;
  }
  
  public AttributeSelection getAttributeSelection() {
    return this.attributeSelection;
  }
  
  public void buildClassifier() throws Exception {
    if (this.classifierClass == null)
      throw new IllegalArgumentException("Classifier class is required for building algorithm"); 
    this.classifier = (AbstractClassifier)AbstractClassifier.forName(this.classifierClass, (String[])this.classifierArgs.clone());
    ASEvaluation asEval = null;
    ASSearch asSearch = null;
    if (this.attributeEvalClass != null)
      asEval = ASEvaluation.forName(this.attributeEvalClass, (String[])this.attributeEvalArgs.clone()); 
    if (this.attributeSearchClass != null)
      asSearch = ASSearch.forName(this.attributeSearchClass, (String[])this.attributeSearchArgs.clone()); 
    if (asEval != null || asSearch != null) {
      this.attributeSelection = new AttributeSelection();
      this.attributeSelection.setEvaluator(asEval);
      this.attributeSelection.setSearch(asSearch);
    } 
  }
  
  public ClassifierResult buildClassifier(Instances is) throws Exception {
    buildClassifier();
    ClassifierResult res = new ClassifierResult(this.resultMetric);
    res.setClassifier(this.classifier);
    res.setClassiferArgsArray(this.classifierArgs);
    res.setAttributeSelection(this.attributeSelection);
    res.setAttributeSearchClassName(this.attributeSearchClass);
    res.setAttributeSearchArgs(this.attributeSearchArgs);
    res.setAttributeEvalClassName(this.attributeEvalClass);
    res.setAttributeEvalArgs(this.attributeEvalArgs);
    res.setStartTime(new Date());
    res.setFoldNo(this.foldNo);
    res.setSeed(this.seed);
    String modelString = this.classifierClass + " seed = " + this.seed + ", fold = " + this.foldNo + " " + Arrays.toString((Object[])this.classifierArgs);
    if (this.attributeSearchClass != null)
      modelString = modelString + ", attribute search = " + this.attributeSearchClass + " " + Arrays.toString((Object[])this.attributeSearchArgs); 
    if (this.attributeEvalClass != null)
      modelString = modelString + ", attribute eval = " + this.attributeEvalClass + " " + Arrays.toString((Object[])this.attributeEvalArgs); 
    res.setModelString(modelString);
    Evaluation eval = null;
    try {
      eval = new Evaluation(is);
      eval.crossValidateModel((Classifier)this.classifier, is, this.foldNo, new Random(this.seed));
      res.setCompleted(true);
      res.setPercentEvaluated((100.0F * (float)(1.0D - eval.unclassified() / is.numInstances())));
      res.setScoreFromEval(eval, is);
    } catch (Exception e) {
      res.setCompleted(false);
    } 
    res.setFinishTime(new Date());
    return res;
  }
  
  public int getSeed() {
    return this.seed;
  }
  
  public void setSeed(int seed) {
    this.seed = seed;
  }
  
  public int getFoldNo() {
    return this.foldNo;
  }
  
  public void setFoldNo(int foldNo) {
    this.foldNo = foldNo;
  }
  
  public String getResultMetric() {
    return this.resultMetric;
  }
  
  public void setResultMetric(String resultMetric) {
    this.resultMetric = resultMetric;
  }
}
