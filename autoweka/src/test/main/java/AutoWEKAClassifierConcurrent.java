import org.junit.Test;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.meta.AutoWEKAClassifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AutoWEKAClassifierConcurrent {

    @Test
    public void concurrentCall() throws InterruptedException, ExecutionException {
        final String[] args = {"-t","/Users/jumpai/Downloads/iris.arff", "-seed", "123", "-timeLimit", "60", "-no-cv"};
        final String[] args2 = {"-t","/Users/jumpai/Downloads/iris.arff", "-seed", "124", "-timeLimit", "60", "-no-cv"};
        Callable<AutoWEKAClassifier> callable = new Callable<AutoWEKAClassifier>() {
            @Override
            public AutoWEKAClassifier call() throws Exception {
                AutoWEKAClassifier classifier = new AutoWEKAClassifier();
                classifier.runMain(args);
//                System.out.println("Running 1");
//                AutoWEKAClassifier.main(args);
                return classifier;
//                return null;
            }
        };

        Callable<AutoWEKAClassifier> callable2 = new Callable<AutoWEKAClassifier>() {
            @Override
            public AutoWEKAClassifier call() throws Exception {
                AutoWEKAClassifier classifier = new AutoWEKAClassifier();
//                Thread.sleep(200);
                classifier.runMain(args2);
//                System.out.println("Running 2");
                return classifier;
            }
        };

        List<Callable<AutoWEKAClassifier>> callableList = new ArrayList<>();
        callableList.add(callable);
        callableList.add(callable2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<AutoWEKAClassifier>> futures = executor.invokeAll(callableList);

//        executor.awaitTermination(10, TimeUnit.MINUTES);
        for( Future<AutoWEKAClassifier> future : futures){
            AutoWEKAClassifier classifier =  future.get();
            System.out.println("Result Future:" + classifier);
        }
    }

}
