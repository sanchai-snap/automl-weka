package ca.ubc.cs.datastore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RunResultHistory {

    private static final int MAX_NUMBER_OF_RESULT = 30;

    private List<CrossValidateResult> crossValidateResultList;

    private long totalModel = 0L;

    private ReadWriteLock readWriteLock;

    RunResultHistory(){
        this.crossValidateResultList = new LinkedList<>();
        readWriteLock = new ReentrantReadWriteLock();
    }

    public void addData(CrossValidateResult result){

        // ignore incomplete data
        if(!result.isComplete())
            return;

        readWriteLock.writeLock().lock();
        try{
            crossValidateResultList.add(result);
            Collections.sort(this.crossValidateResultList);

            if(crossValidateResultList.size() > MAX_NUMBER_OF_RESULT){
                this.crossValidateResultList.remove(MAX_NUMBER_OF_RESULT);
                this.totalModel++;
            }
        }finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public int size(){
        return crossValidateResultList.size();
    }

    public CrossValidateResult getBestResult(){
        if (this.crossValidateResultList.size() > 0)
            return this.crossValidateResultList.get(0);
        return null;
    }

    public List<CrossValidateResult> getResultList(){
        return new ArrayList<>(crossValidateResultList);
    }

    public long getTotalModel() {
        return this.totalModel;
    }

    public void setTotalModel(long totalModel) {
        this.totalModel = totalModel;
    }
}
