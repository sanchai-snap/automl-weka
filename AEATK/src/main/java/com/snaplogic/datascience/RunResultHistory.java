package com.snaplogic.datascience;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RunResultHistory {

    private List<CrossValidateResult> crossValidateResultList;

    private ReadWriteLock readWriteLock;

    RunResultHistory(){
        crossValidateResultList = new ArrayList<>();
        readWriteLock = new ReentrantReadWriteLock();
    }

    public void addData(double matricResult, String crossValidationResult){
        readWriteLock.writeLock().lock();
        try{
            CrossValidateResult result = new CrossValidateResult();
            result.setMatricValue(matricResult);
            result.setCrossValidationString(crossValidationResult);

            crossValidateResultList.add(result);
        }finally {
            readWriteLock.writeLock().unlock();
        }
    }
}
