package de.dkfz.b080.co.files;

import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.FileGroup;

import java.util.List;

/**
 * @author michael
 */
public abstract class AnnotatableFileGroup<F extends BaseFile, A extends BaseFile> extends FileGroup<F> {

    protected A annotationFile;

    public AnnotatableFileGroup(List<F> files) {
        super(files);
    }

    public abstract A annotate();

    public boolean hasAnnotationFile() {
        return annotationFile != null;
    }

    public A getAnnotationFile() {
        if(annotationFile == null) {
            annotationFile = annotate();
        }
        return annotationFile;

    }

}
