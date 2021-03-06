/*
*    This program is free software; you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation; either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program; if not, write to the Free Software
*    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

package weka.classifiers.meta;

import autoweka.ClassifierResult;
import autoweka.tools.CrossValidateResultUpdater;
import ca.ubc.cs.datastore.CrossValidateResult;
import ca.ubc.cs.datastore.RunResultHistory;
import ca.ubc.cs.datastore.ValidationResultStore;
import weka.attributeSelection.AttributeSelection;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;

import weka.core.AdditionalMeasureProducer;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.converters.ArffSaver;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformation;
import weka.core.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.net.URLDecoder;

import java.security.Permission;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import autoweka.Experiment;
import autoweka.ExperimentConstructor;
import autoweka.Util;
import autoweka.tools.ExperimentRunner;

import autoweka.Configuration;
import autoweka.ConfigurationCollection;

/**
 * Auto-WEKA interface for WEKA.

* * @author Lars Kotthoff
 */
public class AutoWEKAClassifier extends AbstractClassifier implements AdditionalMeasureProducer {

    /** For serialization. */
    private static final long serialVersionUID = 2907034203562786373L;

    /** For logging Auto-WEKA's output. */
    private transient final Logger log = LoggerFactory.getLogger(AutoWEKAClassifier.class);

    /** Default time limit for Auto-WEKA. */
    static final int DEFAULT_TIME_LIMIT = 15;
    /** Default memory limit for classifiers. */
    static final int DEFAULT_MEM_LIMIT = 1024;
    /** Default */
    static final int DEFAULT_N_BEST = 1;

    static final int DEFAULT_FOLD_NO = 10;

    /** The class of the chosen attribute search method. */
    public String getAttributeSearchClass() {
        return attributeSearchClass;
    }

    public void setAttributeSearchClass(String attributeSearchClass) {
        this.attributeSearchClass = attributeSearchClass;
    }

    /** The arguments of the chosen attribute search method. */
    public String[] getAttributeSearchArgs() {
        return attributeSearchArgs;
    }

    public void setAttributeSearchArgs(String[] attributeSearchArgs) {
        this.attributeSearchArgs = attributeSearchArgs;
    }

    /** The class of the chosen attribute evaluation. */
    public String getAttributeEvalClass() {
        return attributeEvalClass;
    }

    public void setAttributeEvalClass(String attributeEvalClass) {
        this.attributeEvalClass = attributeEvalClass;
    }

    /** The arguments of the chosen attribute evaluation method. */
    public String[] getAttributeEvalArgs() {
        return attributeEvalArgs;
    }

    public void setAttributeEvalArgs(String[] attributeEvalArgs) {
        this.attributeEvalArgs = attributeEvalArgs;
    }

    /** Start time of evaluation process. */
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /** Finish time of evaluation process. */
    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public int getFoldNo() {
        return this.foldNo;
    }

    public void setFoldNo(int foldNo) {
        this.foldNo = foldNo;
    }

    /** Internal evaluation method. */
    static enum Resampling {
        CrossValidation,
        MultiLevel,
        RandomSubSampling,
        TerminationHoldout
    }
    /** Default evaluation method. */
    static final Resampling DEFAULT_RESAMPLING = Resampling.CrossValidation;

    /** Available metrics. */
    static enum Metric {
        areaAboveROC,
        areaUnderROC,
        avgCost,
        correct,
        correlationCoefficient,
        errorRate,
        falseNegativeRate,
        falsePositiveRate,
        fMeasure,
        incorrect,
        kappa,
        kBInformation,
        kBMeanInformation,
        kBRelativeInformation,
        meanAbsoluteError,
        pctCorrect,
        pctIncorrect,
        precision,
        relativeAbsoluteError,
        rootMeanSquaredError,
        rootRelativeSquaredError,
        weightedAreaUnderROC,
        weightedFalseNegativeRate,
        weightedFalsePositiveRate,
        weightedFMeasure,
        weightedPrecision,
        weightedRecall,
        weightedTrueNegativeRate,
        weightedTruePositiveRate
    }
    /** Metrics to maximise. */
    static final Metric[] metricsToMax = {
        Metric.areaUnderROC,
        Metric.correct,
        Metric.correlationCoefficient,
        Metric.fMeasure,
        Metric.kappa,
        Metric.kBInformation,
        Metric.kBMeanInformation,
        Metric.kBRelativeInformation,
        Metric.pctCorrect,
        Metric.precision,
        Metric.weightedAreaUnderROC,
        Metric.weightedFMeasure,
        Metric.weightedPrecision,
        Metric.weightedRecall,
        Metric.weightedTrueNegativeRate,
        Metric.weightedTruePositiveRate
    };
    /** Default evaluation metric. */
    static final Metric DEFAULT_METRIC = Metric.errorRate;

    /** Default arguments for the different evaluation methods. */
    static final Map<Resampling, String> resamplingArgsMap;
    static {
        resamplingArgsMap = new HashMap<Resampling, String>();
        resamplingArgsMap.put(Resampling.CrossValidation, "numFolds=10");
        resamplingArgsMap.put(Resampling.MultiLevel, "numLevels=2[$]autoweka.instancegenerators.CrossValidation[$]numFolds=10");
        resamplingArgsMap.put(Resampling.RandomSubSampling, "numSamples=10:percent=66");
    }
    /** Arguments for the default evaluation method. */
    static final String DEFAULT_RESAMPLING_ARGS = resamplingArgsMap.get(DEFAULT_RESAMPLING);

    /** Default additional arguments for Auto-WEKA. */
    static final String DEFAULT_EXTRA_ARGS = "initialIncumbent=DEFAULT:acq-func=EI";

    /** The path for the sorted best configurations **/
    public static final String configurationRankingPath = "ConfigurationLogging" + File.separator + "configuration_ranking.xml";
    /** The path for the log with the hashcodes for the configs we have **/
    public static final String configurationHashSetPath = "ConfigurationLogging" + File.separator + "configuration_hashes.txt";
    /** The path for the directory with the configuration data and score **/
    public static final String configurationInfoDirPath = "ConfigurationLogging" + File.separator + "configurations/";


    /** The chosen classifier. */
    protected Classifier classifier;
    /** The chosen attribute selection method. */
    protected AttributeSelection as;

    protected AutoWEKAClassifierExternalModel externalModel;

    protected boolean hasAttributeSelection;

    /** The class of the chosen classifier. */
    protected String classifierClass;
    /** The arguments of the chosen classifier. */
    protected String[] classifierArgs;
    private String attributeSearchClass;
    private String[] attributeSearchArgs;
    private String attributeEvalClass;
    private String[] attributeEvalArgs;

    private Date startTime;
    private Date finishTime;

    private int foldNo;

    /** The paths to the internal Auto-WEKA files.*/
    protected String[] msExperimentPaths;
    /** The internal name of the experiment. */
    protected static String expName = "Auto-WEKA";

    /** The random seed. */
    protected int seed = 123;
    /** The time limit for running Auto-WEKA. */
    protected int timeLimit = DEFAULT_TIME_LIMIT;
    /** The time limit for running Auto-WEKA. */
    protected int wallClockLimit = DEFAULT_TIME_LIMIT;
    /** The memory limit for running classifiers. */
    protected int memLimit = DEFAULT_MEM_LIMIT;

    /** The number of best configurations to return as output. */
    protected int nBestConfigs = DEFAULT_N_BEST;
    /** The best configurations. */
    protected ConfigurationCollection bestConfigsCollection;

    /** The internal evaluation method. */
    protected Resampling resampling = DEFAULT_RESAMPLING;
    /** The arguments to the evaluation method. */
    protected String resamplingArgs = DEFAULT_RESAMPLING_ARGS;
    /** The extra arguments for Auto-WEKA. */
    protected String extraArgs = DEFAULT_EXTRA_ARGS;

    /** The error metric. */
    protected Metric metric = DEFAULT_METRIC;

    /** The estimated metric values of the chosen methods for each parallel run. */
    protected double[] estimatedMetricValues;
    /** The estimated metric value of the method chosen out of the parallel runs. */
    protected double estimatedMetricValue = -1;

    /** The evaluation for the best classifier. */
    protected Evaluation eval;

    /** The default number of parallel threads. */
    protected final int DEFAULT_PARALLEL_RUNS = 1;

    /** The number of parallel threads. */
    protected int parallelRuns = DEFAULT_PARALLEL_RUNS;

    /** The time it took to train the final classifier. */
    protected double finalTrainTime = -1;

    protected boolean skipSearch = false;

    protected int runCountLimit = Integer.MAX_VALUE;

    private transient weka.gui.Logger wLog;

    /* Don't ask. */
    public int totalTried;

    protected RunResultHistory runResultHistory;

    /**
     * Main method for testing this class.
     *
     * @param argv should contain command line options (see setOptions)
     */
    public static void main(String[] argv) {
        System.setSecurityManager(new SecurityManager() {
                                      @Override
                                      public void checkExit(int status) {
                                          new Exception("exit attempt with return code " + status).printStackTrace();
                                      }
                                    @Override
                                    public void checkPermission(Permission perm, Object context) { }

                                    @Override
                                    public void checkPermission(Permission perm) { }
                                  });

        // this always succeeds...
        runClassifier(new AutoWEKAClassifier(), argv);
    }

    public void runMain(String[] args){
        try {
            runClassifier(this, args);
        }catch(Exception e){
            log.error("Result Future: ", e);
        }
    }

    /** Constructs a new AutoWEKAClassifier. */
    public AutoWEKAClassifier() {
        classifier = null;
        classifierClass = null;
        classifierArgs = null;
        setAttributeSearchClass(null);
        setAttributeSearchArgs(new String[0]);
        setAttributeEvalClass(null);
        setAttributeEvalArgs(new String[0]);
        wLog = null;

        totalTried = 0;

        // work around broken XML parsers
        Properties props = System.getProperties();
        props.setProperty("org.xml.sax.parser", "com.sun.org.apache.xerces.internal.parsers.SAXParser");
        props.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        props.setProperty("javax.xml.parsers.SAXParserFactory", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
    }


    protected String getTempDirectoryPath() throws IOException {
    	return Files.createTempDirectory("autoweka").toString() + File.separator;
    }
    
    protected void storeResult(String ling) {
    	// Do nothing
    }
    
    /**
    * Find the best classifier, arguments, and attribute selection for the data.
    *
    * @param is the training data to be used for selecting and tuning the
    * classifier.
    * @throws Exception if the classifier could not be built successfully.
    */
    public void buildClassifier(Instances is) throws Exception {
        String experimentKey = UUID.randomUUID().toString();
        try {
            if (this.externalModel != null)
                buildClassifierExternalModel(is, experimentKey);
            if (!this.skipSearch)
                buildClassifierInternal(is, experimentKey);
        } finally {
            this.runResultHistory = ValidationResultStore.getInstance().pollRunResultHistory(experimentKey);
        }
        if (this.runResultHistory != null && this.runResultHistory.size() > 0) {
            CrossValidateResult result = this.runResultHistory.getBestResult();
            Evaluation eval = result.getEvaluation();
            this.log.info(" {}, {} ", Double.valueOf(eval.incorrect()), Double.valueOf(eval.pctIncorrect()));
            this.classifier = (Classifier)result.getClassifier();
            this.as = result.getAttributeSelection();
            this.eval = eval;
            setAttributeEvalClass(result.getAttributeEval());
            setAttributeEvalArgs(result.getAttributeEvalArgs());
            setAttributeSearchClass(result.getAttributeSearch());
            setAttributeSearchArgs(result.getAttributeSearchArgs());
            setStartTime(result.getStartTime());
            setFinishTime(result.getFinishTime());
            setFoldNo(result.getFoldNo());
            setSeed(result.getSeed());
            this.hasAttributeSelection = (this.as != null);
            if (this.as == null) {
                this.as = new AttributeSelection();
                this.log.info("No attribute selection found");
            }
            this.as.SelectAttributes(is);
            long startTime = System.currentTimeMillis();
            if (this.as != null)
                is = this.as.reduceDimensionality(is);
            this.classifier.buildClassifier(is);
            long stopTime = System.currentTimeMillis();
            this.finalTrainTime = (stopTime - startTime) / 1000.0D;
            this.classifierClass = this.classifier.getClass().toString();
            this.classifierArgs = result.getClassifierArgs();
            eval = new Evaluation(is);
            eval.evaluateModel(this.classifier, is, new Object[0]);
            Instances newInstances = new Instances(is);
            Evaluation evalOther = new Evaluation(newInstances);
            for (Instance instance : newInstances)
                evalOther.evaluateModelOnceAndRecordPrediction(this.classifier, instance);
        } else {
            this.runResultHistory = ValidationResultStore.getInstance().getEmptyResult();
        }

    }

    private void buildClassifierInternal(Instances is, String experimentKey) throws Exception {
        getCapabilities().testWithFail(is);
        try {
            this.estimatedMetricValues = new double[this.parallelRuns];
            this.msExperimentPaths = new String[this.parallelRuns];
            for (int i = 0; i < this.parallelRuns; i++) {
                this.estimatedMetricValues[i] = -1.0D;
                this.msExperimentPaths[i] = getTempDirectoryPath();
                Experiment exp = new Experiment();
                exp.name = expName;
                exp.experimentKey = experimentKey;
                exp.runCount = this.runCountLimit;
                exp.resultMetric = this.metric.toString();
                Properties props = Util.parsePropertyString("type=trainTestArff:testArff=__dummy__");
                ArffSaver saver = new ArffSaver();
                saver.setInstances(is);
                File fp = new File(this.msExperimentPaths[i] + expName + File.separator + expName + ".arff");
                saver.setFile(fp);
                saver.writeBatch();
                props.setProperty("trainArff", URLDecoder.decode(fp.getAbsolutePath()));
                props.setProperty("classIndex", String.valueOf(is.classIndex()));
                exp.datasetString = Util.propertiesToString(props);
                exp.instanceGenerator = "autoweka.instancegenerators." + String.valueOf(this.resampling);
                exp.instanceGeneratorArgs = "seed=" + (this.seed + 1) + ":numFolds=" + this.foldNo + ":seed=" + (this.seed + i);
                exp.attributeSelection = true;
                exp.attributeSelectionTimeout = Math.max(this.timeLimit / 60, 1);
                exp.tunerTimeout = this.wallClockLimit;
                exp.trainTimeout = (Math.max(this.timeLimit / 60, 1) * 5);
                exp.memory = this.memLimit + "m";
                exp.extraPropsString = this.extraArgs;
                List<String> args = new LinkedList<>();
                args.add("-experimentpath");
                args.add(this.msExperimentPaths[i]);
                buildExperimentConstructor(exp, args);
                if (this.nBestConfigs > 1) {
                    String temporaryDirPath = this.msExperimentPaths[i] + expName + File.separator;
                    Util.makePath(temporaryDirPath + configurationInfoDirPath);
                    Util.initializeFile(temporaryDirPath + configurationRankingPath);
                    Util.initializeFile(temporaryDirPath + configurationHashSetPath);
                }
            }
            Thread[] workers = new Thread[this.parallelRuns];
            int j;
            for (j = 0; j < this.parallelRuns; j++) {
                final int index = j;
                workers[j] = new Thread(new Runnable() {
                    public void run() {
                        String[] args = new String[2];
                        args[0] = AutoWEKAClassifier.this.msExperimentPaths[index] + AutoWEKAClassifier.expName;
                        args[1] = "" + (AutoWEKAClassifier.this.seed + index);
                        List<String> resultList = ExperimentRunner.runMain(args);
                        Pattern p = Pattern.compile(".*Estimated mean quality of final incumbent config .* on test set: (-?[0-9.]+).*");
                        Pattern pint = Pattern.compile(".*mean quality of.*: (-?[0-9E.]+);.*");
                        for (String line : resultList) {
                            Matcher m = p.matcher(line);
                            if (m.matches()) {
                                AutoWEKAClassifier.this.estimatedMetricValues[index] = Double.parseDouble(m.group(1));
                                if (Arrays.<AutoWEKAClassifier.Metric>asList(AutoWEKAClassifier.metricsToMax).contains(AutoWEKAClassifier.this.metric))
                                    AutoWEKAClassifier.this.estimatedMetricValues[index] = AutoWEKAClassifier.this.estimatedMetricValues[index] * -1.0D;
                            }
                        }
                    }
                });
                workers[j].start();
            }
            try {
                for (j = 0; j < this.parallelRuns; j++)
                    workers[j].join();
            } catch (InterruptedException e) {
                for (int k = 0; k < this.parallelRuns; k++)
                    workers[k].interrupt();
                throw new InterruptedException("Auto-WEKA run interrupted!");
            }
        } catch (Exception e) {
            this.log.error("Result Future: ", e);
        }
    }

    private void buildClassifierExternalModel(Instances is, String experimentKey) throws Exception {
        ClassifierResult result = this.externalModel.buildClassifier(is);
        CrossValidateResult crossValidateResult = new CrossValidateResult();
        CrossValidateResultUpdater.updateValue(crossValidateResult, result);
        ValidationResultStore store = ValidationResultStore.getInstance();
        RunResultHistory runResultHistory = store.getRunResultHistory(experimentKey);
        runResultHistory.addData(crossValidateResult);
    }

    public void setExternalModel(AutoWEKAClassifierExternalModel externalModel) {
        this.externalModel = externalModel;
    }

    protected void buildExperimentConstructor(Experiment exp, List<String> args) {
    	ExperimentConstructor.buildSingle("autoweka.smac.SMACExperimentConstructor", exp, args);
	}

	/**
    * Calculates the class membership for the given test instance.
    *
    * @param i the instance to be classified
    * @return predicted class
    * @throws Exception if instance could not be classified successfully
    */
    public double classifyInstance(Instance i) throws Exception {
        if(classifier == null) {
            throw new Exception("Auto-WEKA has not been run yet to get a model!");
        }
        i = as.reduceDimensionality(i);
        return classifier.classifyInstance(i);
    }

    /**
    * Calculates the class membership probabilities for the given test instance.
    *
    * @param i the instance to be classified
    * @return predicted class probability distribution
    * @throws Exception if instance could not be classified successfully.
    */
    public double[] distributionForInstance(Instance i) throws Exception {
        if(classifier == null) {
            throw new Exception("Auto-WEKA has not been run yet to get a model!");
        }
        i = as.reduceDimensionality(i);
        return classifier.distributionForInstance(i);
    }

    /**
     * Gets an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    @Override
    public Enumeration<Option> listOptions() {
        Vector<Option> result = new Vector<Option>();
        result.addElement(
            new Option("\tThe seed for the random number generator.\n" + "\t(default: " + seed + ")",
                "seed", 1, "-seed <seed>"));
        result.addElement(
            new Option("\tThe time limit for tuning in minutes (approximately).\n" + "\t(default: " + DEFAULT_TIME_LIMIT + ")",
                "timeLimit", 60, "-timeLimit <limit>"));
        result.addElement(
            new Option("\tThe memory limit for runs in MiB.\n" + "\t(default: " + DEFAULT_MEM_LIMIT + ")",
                "memLimit", 1, "-memLimit <limit>"));
        result.addElement(
            new Option("\tThe amount of best configurations to output.\n" + "\t(default: " + DEFAULT_N_BEST + ")",
                "nBestConfigs", 1, "-nBestConfigs <limit>"));
        result.addElement(
            new Option("\tThe metric to optimise.\n" + "\t(default: " + DEFAULT_METRIC + ")",
                "metric", 1, "-metric <metric>"));
        result.addElement(
            new Option("\tThe number of parallel runs. EXPERIMENTAL.\n" + "\t(default: " + DEFAULT_PARALLEL_RUNS + ")",
                "parallelRuns", 1, "-parallelRuns <runs>"));
        result.addElement(
                new Option("\tSkip searching and relay on given parameters\n\t(default: false)",
                        "skipSearch", 1, "-skipSearch"));
        //result.addElement(
        //    new Option("\tThe type of resampling used.\n" + "\t(default: " + String.valueOf(DEFAULT_RESAMPLING) + ")",
        //        "resampling", 1, "-resampling <resampling>"));
        //result.addElement(
        //    new Option("\tResampling arguments.\n" + "\t(default: " + DEFAULT_RESAMPLING_ARGS + ")",
        //        "resamplingArgs", 1, "-resamplingArgs <args>"));
        //result.addElement(
        //    new Option("\tExtra arguments.\n" + "\t(default: " + DEFAULT_EXTRA_ARGS + ")",
        //        "extraArgs", 1, "-extraArgs <args>"));

        Enumeration<Option> enu = super.listOptions();
        while (enu.hasMoreElements()) {
            result.addElement(enu.nextElement());
        }

        return result.elements();
    }

    /**
     * Returns the options of the current setup.
     *
     * @return the current options
     */
    @Override
    public String[] getOptions() {
        Vector<String> result = new Vector<String>();

        result.add("-seed");
        result.add("" + seed);
        result.add("-timeLimit");
        result.add("" + timeLimit);
        result.add("-memLimit");
        result.add("" + memLimit);
        result.add("-nBestConfigs");
        result.add("" + nBestConfigs);
        result.add("-metric");
        result.add("" + metric);
        result.add("-parallelRuns");
        result.add("" + parallelRuns);
        result.add("-skipSearch");
        result.add("" + this.skipSearch);
        result.add("-runCountLimit");
        result.add("" + this.runCountLimit);
        //result.add("-resampling");
        //result.add("" + resampling);
        //result.add("-resamplingArgs");
        //result.add("" + resamplingArgs);
        //result.add("-extraArgs");
        //result.add("" + extraArgs);

        Collections.addAll(result, super.getOptions());
        return result.toArray(new String[result.size()]);
    }

    /**
     * Set the options for the current setup.
     *
     * @param options the new options
     */
    @Override
    public void setOptions(String[] options) throws Exception {
        String tmpStr;
        String[] tmpOptions;

        tmpStr = Utils.getOption("seed", options);
        if (tmpStr.length() != 0) {
            seed = Integer.parseInt(tmpStr);
        }

        tmpStr = Utils.getOption("timeLimit", options);
        if (tmpStr.length() != 0) {
            timeLimit = Integer.parseInt(tmpStr);
        } else {
            timeLimit = DEFAULT_TIME_LIMIT;
        }

        tmpStr = Utils.getOption("wallClockLimit", options);
        if (tmpStr.length() != 0) {
        	wallClockLimit = Integer.parseInt(tmpStr);
        } else {
//        	wallClockLimit = DEFAULT_TIME_LIMIT;
        	wallClockLimit = Math.max(1,(int)(timeLimit*5/6));
        }
        
        tmpStr = Utils.getOption("memLimit", options);
        if (tmpStr.length() != 0) {
            memLimit = Integer.parseInt(tmpStr);
        } else {
            memLimit = DEFAULT_MEM_LIMIT;
        }

        tmpStr = Utils.getOption("nBestConfigs", options);
        if (tmpStr.length() != 0) {
            nBestConfigs = Integer.parseInt(tmpStr);
        } else {
            nBestConfigs = DEFAULT_N_BEST;
        }

        tmpStr = Utils.getOption("metric", options);
        if (tmpStr.length() != 0) {
            metric = Metric.valueOf(tmpStr);
        } else {
            metric = DEFAULT_METRIC;
        }

        tmpStr = Utils.getOption("parallelRuns", options);
        if (tmpStr.length() != 0) {
            parallelRuns = Integer.parseInt(tmpStr);
        } else {
            parallelRuns = DEFAULT_PARALLEL_RUNS;
        }

        tmpStr = Utils.getOption("skipSearch", options);
        if (tmpStr.length() != 0)
            this.skipSearch = Boolean.valueOf(tmpStr).booleanValue();
        tmpStr = Utils.getOption("foldNo", options);
        if (tmpStr.length() != 0) {
            this.foldNo = Integer.parseInt(tmpStr);
        } else {
            this.foldNo = 10;
        }
        tmpStr = Utils.getOption("runCountLimit", options);
        if (tmpStr.length() != 0) {
            this.runCountLimit = Integer.parseInt(tmpStr);
        } else {
            this.runCountLimit = Integer.MAX_VALUE;
        }

        //tmpStr = Utils.getOption("resampling", options);
        //if (tmpStr.length() != 0) {
        //    resampling = Resampling.valueOf(tmpStr);
        //} else {
        //    resampling = DEFAULT_RESAMPLING;
        //}
        //resamplingArgs = resamplingArgsMap.get(resampling);

        //tmpStr = Utils.getOption("resamplingArgs", options);
        //if (tmpStr.length() != 0) {
        //    resamplingArgs = tmpStr;
        //}

        //tmpStr = Utils.getOption("extraArgs", options);
        //if (tmpStr.length() != 0) {
        //    extraArgs = tmpStr;
        //} else {
        //    extraArgs = DEFAULT_EXTRA_ARGS;
        //}

        super.setOptions(options);
        Utils.checkForRemainingOptions(options);
    }

    /**
     * Set the random seed.
     * @param s The random seed.
     */
    public void setSeed(int s) {
        seed = s;
    }

    /**
     * Get the random seed.
     * @return The random seed.
     */
    public int getSeed() {
        return seed;
    }

    /**
     * Returns the tip text for this property.
     * @return tip text for this property
     */
    public String seedTipText() {
        return "the seed for the random number generator (you do not usually need to change this)";
    }

    /**
     * Set the number of parallel runs.
     * @param n The number of parallel runs.
     */
    public void setParallelRuns(int n) {
        parallelRuns = n;
    }

    /**
     * Get the number of runs to do in parallel.
     * @return The number of parallel runs.
     */
    public int getParallelRuns() {
        return parallelRuns;
    }

    /**
     * Returns the tip text for this property.
     * @return tip text for this property
     */
    public String parallelRunsTipText() {
        return "the number of runs to perform in parallel EXPERIMENTAL";
    }

    /**
     * Set the metric.
     * @param m The metric.
     */
    public void setMetric(Metric m) {
        metric = m;
    }

    /**
     * Get the metric.
     * @return The metric.
     */
    public Metric getMetric() {
        return metric;
    }

    /**
     * Returns the tip text for this property.
     * @return tip text for this property
     */
    public String metricTipText() {
        return "the metric to optimise";
    }

    /**
     * Set the time limit.
     * @param tl The time limit in minutes.
     */
    public void setTimeLimit(int tl) {
        timeLimit = tl;
    }

    /**
     * Get the time limit.
     * @return The time limit in minutes.
     */
    public int getTimeLimit() {
        return timeLimit;
    }

    /**
     * Returns the tip text for this property.
     * @return tip text for this property
     */
    public String timeLimitTipText() {
        return "the time limit for tuning (in minutes)";
    }

    /**
     * Set the memory limit.
     * @param ml The memory limit in MiB.
     */
    public void setMemLimit(int ml) {
        memLimit = ml;
    }

    /**
     * Get the memory limit.
     * @return The memory limit in MiB.
     */
    public int getMemLimit() {
        return memLimit;
    }

    /**
     * Returns the tip text for this property.
     * @return tip text for this property
     */
    public String memLimitTipText() {
        return "the memory limit for runs (in MiB)";
    }

    /**
     * Set the amount of configurations that will be given as output
     * @param nbc The amount of best configurations desired by the user
     */
    public void setnBestConfigs(int nbc) {
        nBestConfigs = nbc;
    }

    /**
     * Get the memory limit.
     * @return The amount of best configurations that will be given as output
     */
    public int getnBestConfigs() {
        return nBestConfigs;
    }

    /**
     * Returns the tip text for this property.
     * @return tip text for this property
     */
    public String nBestConfigsTipText() {
        return "How many of the best configurations should be returned as output";
    }

    //public void setResampling(Resampling r) {
    //    resampling = r;
    //    resamplingArgs = resamplingArgsMap.get(r);
    //}

    //public Resampling getResampling() {
    //    return resampling;
    //}

    ///**
    // * Returns the tip text for this property.
    // * @return tip text for this property
    // */
    //public String ResamplingTipText() {
    //    return "the type of resampling";
    //}

    //public void setResamplingArgs(String args) {
    //    resamplingArgs = args;
    //}

    //public String getResamplingArgs() {
    //    return resamplingArgs;
    //}

    ///**
    // * Returns the tip text for this property.
    // * @return tip text for this property
    // */
    //public String resamplingArgsTipText() {
    //    return "resampling arguments";
    //}

    //public void setExtraArgs(String args) {
    //    extraArgs = args;
    //}

    //public String getExtraArgs() {
    //    return extraArgs;
    //}

    ///**
    // * Returns the tip text for this property.
    // * @return tip text for this property
    // */
    //public String extraArgsTipText() {
    //    return "extra arguments";
    //}

    /** Set the WEKA logger.
     * Used for providing feedback during execution.
     *
     * @param log The logger.
     */
    public void setLog(weka.gui.Logger log) {
        this.wLog = log;
    }

    /**
     * Returns default capabilities of the classifier.
     *
     * @return      the capabilities of this classifier
     */
    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.disableAll();

        // attributes
        result.enable(Capability.NOMINAL_ATTRIBUTES);
        result.enable(Capability.NUMERIC_ATTRIBUTES);
        result.enable(Capability.DATE_ATTRIBUTES);
        result.enable(Capability.STRING_ATTRIBUTES);
        result.enable(Capability.RELATIONAL_ATTRIBUTES);
        result.enable(Capability.MISSING_VALUES);

        // class
        result.enable(Capability.NOMINAL_CLASS);
        result.enable(Capability.NUMERIC_CLASS);
        result.enable(Capability.DATE_CLASS);
        result.enable(Capability.MISSING_CLASS_VALUES);

        // instances
        result.setMinimumNumberInstances(1);

        return result;
    }

    /**
     * Returns an instance of a TechnicalInformation object, containing
     * detailed information about the technical background of this class,
     * e.g., paper reference or book this class is based on.
     *
     * @return the technical information about this class
     */
    public TechnicalInformation getTechnicalInformation() {
        TechnicalInformation result = new TechnicalInformation(Type.INPROCEEDINGS);
        result.setValue(Field.AUTHOR, "Chris Thornton, Frank Hutter, Holger Hoos, and Kevin Leyton-Brown");
        result.setValue(Field.YEAR, "2013");
        result.setValue(Field.TITLE, "Auto-WEKA: Combined Selection and Hyperparameter Optimization of Classifiaction Algorithms");
        result.setValue(Field.BOOKTITLE, "Proc. of KDD 2013");

        return result;
    }

    /**
     * This will return a string describing the classifier.
     * @return The string.
     */
    public String globalInfo() {
        return "Automatically finds the best model with its best parameter settings for a given dataset.\n\n"
            + "For more information see:\n\n"
            + getTechnicalInformation().toString();
    }

    /**
     * This will return a string describing the classifier.
     * @return The string.
     */
    public String toString() {
        String res = "best classifier: " + classifierClass + "\n" +
            "arguments: " + (classifierArgs != null ? Arrays.toString(classifierArgs) : "[]") + "\n" +
            "attribute search: " + getAttributeSearchClass() + "\n" +
            "attribute search arguments: " + (getAttributeSearchArgs() != null ? Arrays.toString(getAttributeSearchArgs()) : "[]") + "\n" +
            "attribute evaluation: " + getAttributeEvalClass() + "\n" +
            "attribute evaluation arguments: " + (getAttributeEvalArgs() != null ? Arrays.toString(getAttributeEvalArgs()) : "[]") + "\n" +
            "metric: " + metric + "\n" +
            "estimated " + metric + ": " + estimatedMetricValue + "\n" +
            "training time on evaluation dataset: " + finalTrainTime + " seconds\n\n";

        res += "You can use the chosen classifier in your own code as follows:\n\n";
        if(getAttributeSearchClass() != null || getAttributeEvalClass() != null) {
            res += "AttributeSelection as = new AttributeSelection();\n";
            if(getAttributeSearchClass() != null) {
                res += "ASSearch asSearch = ASSearch.forName(\"" + getAttributeSearchClass() + "\", new String[]{";
                if(getAttributeSearchArgs() != null) {
                    String[] args = getAttributeSearchArgs().clone();
                    for(int i = 0; i < args.length; i++) {
                        res += "\"" + args[i] + "\"";
                        if(i < args.length - 1) res += ", ";
                    }
                }
                res += "});\n";
                res += "as.setSearch(asSearch);\n";
            }

            if(getAttributeEvalClass() != null) {
                res += "ASEvaluation asEval = ASEvaluation.forName(\"" + getAttributeEvalClass() + "\", new String[]{";
                if(getAttributeEvalArgs() != null) {
                    String[] args = getAttributeEvalArgs().clone();
                    for(int i = 0; i < args.length; i++) {
                        res += "\"" + args[i] + "\"";
                        if(i < args.length - 1) res += ", ";
                    }
                }
                res += "});\n";
                res += "as.setEvaluator(asEval);\n";
            }
            res += "as.SelectAttributes(instances);\n";
            res += "instances = as.reduceDimensionality(instances);\n";
        }

        res += "Classifier classifier = AbstractClassifier.forName(\"" + classifierClass + "\", new String[]{";
        if(classifierArgs != null) {
            String[] args = classifierArgs.clone();
            for(int i = 0; i < args.length; i++) {
                res += "\"" + args[i] + "\"";
                if(i < args.length - 1) res += ", ";
            }
        }
        res += "});\n";
        res += "classifier.buildClassifier(instances);\n\n";

        try {
            res += eval.toSummaryString();
            res += "\n";
            res += eval.toMatrixString();
            res += "\n";
            res += eval.toClassDetailsString();
        } catch(Exception e) { /*TODO treat*/ }


        if(nBestConfigs > 1) {

            if(bestConfigsCollection==null){
                res += "\n\n------- BEST CONFIGURATIONS -------";
                res+= "\nEither your dataset is so large or the runtime is so short that we couldn't evaluate even a single fold";
                res+= "\nof your dataset within the given time constraints. Please, consider running Auto-WEKA for a longer time.";
            }else{
                List<Configuration> bccAL = bestConfigsCollection.asArrayList();
                int fullyEvaluatedAmt = bestConfigsCollection.getFullyEvaluatedAmt();
                int maxFoldEvaluationAmt = bccAL.get(0).getEvaluationAmount();

                res += "\n\n------- " + fullyEvaluatedAmt + " BEST CONFIGURATIONS -------";
                res += "\n\nThese are the " + fullyEvaluatedAmt + " best configurations, as ranked by SMAC";
                res += "\nPlease note that this list only contains configurations evaluated on at least "+maxFoldEvaluationAmt+" folds,";
                if(maxFoldEvaluationAmt<10){
                    res+= "\nWhich is less than 10 because that was the largest amount of folds we could evaluate for a single configuration";
                    res+= "\nunder the given time constraints. If you want us to evaluate more folds (recommended), or if you need more configurations,";
                    res +="\nplease consider running Auto-WEKA for a longer time.";
                }else{
                    res += "\nIf you need more configurations, please consider running Auto-WEKA for a longer time.";
                }
                for(int i = 0; i < fullyEvaluatedAmt; i++){
                    res += "\n\nConfiguration #" + (i + 1) + ":\nSMAC Score: " + bccAL.get(i).getAverageScore() + "\nArgument String:\n" + bccAL.get(i).getArgStrings();
                }
            }
            res+="\n\n----END OF CONFIGURATION RANKING----\n";
        }

        if(msExperimentPaths != null) {
            res += "\nTemporary run directories:\n";
            for(int i = 0; i < msExperimentPaths.length; i++) {
                res += msExperimentPaths[i] + "\n";
            }
        }

        res += "\n\nFor better performance, try giving Auto-WEKA more time.\n";
        if(totalTried < 1000) {
            res += "Tried " + totalTried + " configurations; to get good results reliably you may need to allow for trying thousands of configurations.\n";
        }
        return res;
    }

    /**
     * Returns the metric value estimated during Auto-WEKA's internal evaluation.
     * @return The estimated metric value.
     */
    public double measureEstimatedMetricValue() {
        return estimatedMetricValue;
    }

    /**
    * Returns an enumeration of the additional measure names
    * @return an enumeration of the measure names
    */
    public Enumeration enumerateMeasures() {
        Vector newVector = new Vector(1);
        newVector.addElement("measureEstimatedMetricValue");
        return newVector.elements();
    }

    /**
    * Returns the value of the named measure
    * @param additionalMeasureName the name of the measure to query for its value
    * @return the value of the named measure
    * @throws IllegalArgumentException if the named measure is not supported
    */
    public double getMeasure(String additionalMeasureName) {
        if (additionalMeasureName.compareToIgnoreCase("measureEstimatedMetricValue") == 0) {
            return measureEstimatedMetricValue();
        } else {
            throw new IllegalArgumentException(additionalMeasureName
                    + " not supported (Auto-WEKA)");
        }
    }
    
    public Classifier getResultClassifier() {
    	return classifier;
    }
    
    public String[] getResultClassifierAttributes() {
        if(classifierArgs == null)
            return new String[0];
    	return classifierArgs.clone();
    }
    
    public Evaluation getEvaluation() {
    	return eval;
    }

    public AttributeSelection getAttributeSelection() { return as; }

    public boolean hasAttributeSelection() { return hasAttributeSelection; }

    public RunResultHistory getRunResultHistory(){
        return runResultHistory;
    }

}
