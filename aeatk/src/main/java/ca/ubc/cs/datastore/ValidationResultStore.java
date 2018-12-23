package ca.ubc.cs.datastore;

import java.util.concurrent.ConcurrentHashMap;

public class ValidationResultStore {

    private static class ValidationResultStoreLoader {
        private static final ValidationResultStore INSTANCE = new ValidationResultStore();
    }

    public static ValidationResultStore getInstance(){
        return ValidationResultStoreLoader.INSTANCE;
    }

    ValidationResultStore(){
        runResultMap = new ConcurrentHashMap<>();
    }

    private ConcurrentHashMap<String, RunResultHistory> runResultMap;

    public RunResultHistory getRunResultHistory(String runKey){
        RunResultHistory runResultHistory = runResultMap.get(runKey);
        if(runResultHistory == null){
            runResultHistory = new RunResultHistory();
            runResultMap.put(runKey, runResultHistory);
        }

         return runResultHistory;
    }

    public RunResultHistory pollRunResultHistory(String runKey){
        return runResultMap.remove(runKey);
    }

    public RunResultHistory getEmptyResult(){
        return new RunResultHistory();
    }
}
