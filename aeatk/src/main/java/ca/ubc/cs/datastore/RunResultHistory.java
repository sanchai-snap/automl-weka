package ca.ubc.cs.datastore;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RunResultHistory {

    private static final int MAX_NUMBER_OF_RESULT = 30;

    private SortedSet<CrossValidateResult> crossValidateResultList;

    private ReadWriteLock readWriteLock;

    RunResultHistory(){
        crossValidateResultList = new TreeSet<>();
        readWriteLock = new ReentrantReadWriteLock();
    }

    public void addData(CrossValidateResult result){

        // ignore incomplete data
        if(!result.isComplete())
            return;

        readWriteLock.writeLock().lock();
        try{
            crossValidateResultList.add(result);

            if(crossValidateResultList.size() > MAX_NUMBER_OF_RESULT){
                crossValidateResultList.remove(crossValidateResultList.last());
            }
        }finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public int size(){
        return crossValidateResultList.size();
    }

    public CrossValidateResult getBestResult(){
        return crossValidateResultList.first();
    }

    public List<CrossValidateResult> getResultList(){
        return new ArrayList<>(crossValidateResultList);
    }
}
