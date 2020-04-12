package autoweka.tools;

import autoweka.ClassifierResult;
import ca.ubc.cs.datastore.CrossValidateResult;

public class CrossValidateResultUpdater {
  public static void updateValue(CrossValidateResult crossValidateResult, ClassifierResult res) {
    crossValidateResult.setMatricValue(res.getRawScore());
    crossValidateResult.setComplete(res.getCompleted());
    crossValidateResult.setEvaluation(res.getEvaluation());
    crossValidateResult.setClassifier(res.getClassifier());
    crossValidateResult.setClassifierArgs(res.getClassiferArgsArray());
    crossValidateResult.setAttributeSelection(res.getAttributeSelection());
    crossValidateResult.setAttributeEval(res.getAttributeEvalClassName());
    crossValidateResult.setAttributeEvalArgs(res.getAttributeEvalArgs());
    crossValidateResult.setAttributeSearch(res.getAttributeSearchClassName());
    crossValidateResult.setAttributeSearchArgs(res.getAttributeSearchArgs());
    crossValidateResult.setStartTime(res.getStartTime());
    crossValidateResult.setFinishTime(res.getFinishTime());
    crossValidateResult.setFoldNo(res.getFoldNo());
    crossValidateResult.setSeed(res.getSeed());
    crossValidateResult.setCrossValidationString(res.getModelString());
  }
}
