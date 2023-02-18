package com.datasophon.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Generators implements Serializable {
    private String filename;

    private String configFormat;

    private String outputDirectory;

    private List<String> includeParams;

    private String templateName;
    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        Generators generators = (Generators) o;
        if (generators.getFilename().equals(filename)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return filename.hashCode();
    }

}
