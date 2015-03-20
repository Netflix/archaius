package com.netflix.archaius.persisted2.loader;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;

public class FileStreamLoader implements Callable<InputStream>{

    private String filename;

    public FileStreamLoader(String filename) {
        this.filename = filename;
    }
    
    @Override
    public InputStream call() throws Exception {
        return new FileInputStream(filename);
    }

}
