package autoweka.smac;

import java.util.Queue;

import java.util.Arrays;

import autoweka.Wrapper;
import autoweka.ClassifierResult;
import ca.ubc.cs.datastore.CrossValidateResult;

public class SMACWrapper extends Wrapper
{
    private boolean mRawEval = false;

    public static void main(String[] args)
    {
    	System.out.println("Start SMAC Wrapper : "+Arrays.toString(args));
        SMACWrapper wrapper = new SMACWrapper();
        try {
            wrapper.run(args);
        } catch(Exception e) {
//            System.exit(1);
        }
    }

    @Override
    protected void _processWrapperParameterStart(Queue<String> args)
    {
        //First argument is the instance file - smac adds a path here
        mInstance = args.poll();
        //The instance info... ignore
        args.poll();
        //The timeout
        mTimeout = Float.parseFloat(args.poll());
        //The cutoff length... ignore
        args.poll();
        // some random seed -- ignore
        args.poll();
    }

    @Override
    protected void _processParameter(String arg, Queue<String> args)
    {
        if(arg.equals("-raw")){
            mRawEval = true;
        }
    }

    @Override
    protected CrossValidateResult _processResults(ClassifierResult res)
    {
        //Get the score
        double score = res.getScore();
        if(mRawEval)
        {
            score = res.getRawScore();
        }

        //Did we complete?
        String resultStr = "SAT";
        if(!res.getCompleted())
        {
            resultStr = "TIMEOUT";
        }

        StringBuilder extraResultsSB = new StringBuilder();
        int i = 0;
        while(mProperties.containsKey("extraRun" + i))
        {
            //Run this instance
            ClassifierResult evalRes = mRunner.evaluateClassifierOnTesting(res.getClassifier(), mProperties.getProperty("extraRun" + i), mResultMetric, mTimeout);
            extraResultsSB.append("(");
            extraResultsSB.append(evalRes.getEvaluationTime());
            extraResultsSB.append(" ");
            extraResultsSB.append(evalRes.getRawScore());
            extraResultsSB.append(") ");
            i++;
        }
        //We need to add the norm penalty
        if(mRawEval)
        {
            extraResultsSB.append("[");
            extraResultsSB.append(res.getNormalizationPenalty());
            extraResultsSB.append("] ");
        }
        if(res.getMemOut()){
            extraResultsSB.append("MEMOUT ");
        }

        extraResultsSB.append(res.getPercentEvaluated());

        //Print the result string
        String resultString = "Result for ParamILS: " + resultStr + ", " + res.getTime() + ", 0, " + score + ", " + mExperimentSeed + ", EXTRA " + extraResultsSB.toString();
//        System.out.println(resultString);

        CrossValidateResult crossValidateResult = new CrossValidateResult();
        crossValidateResult.setMatricValue(res.getRawScore());
        crossValidateResult.setComplete(res.getCompleted());
        crossValidateResult.setCrossValidationString( res.getClassifier().toString());
        crossValidateResult.setResultString(resultString);
        crossValidateResult.setEvaluation(res.getEvaluation());
        crossValidateResult.setClassifier(res.getClassifier());
        crossValidateResult.setClassifierArgs(res.getClassiferArgsArray());
        crossValidateResult.setAttributeSelection(res.getAttributeSelection());
        crossValidateResult.setCrossValidationString(res.getModelString());

        return crossValidateResult;
    }
}
