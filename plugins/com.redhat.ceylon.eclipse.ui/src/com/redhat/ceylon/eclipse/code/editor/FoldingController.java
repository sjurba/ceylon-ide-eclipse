package com.redhat.ceylon.eclipse.code.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;

import com.redhat.ceylon.eclipse.code.parse.CeylonParseController;
import com.redhat.ceylon.eclipse.code.parse.IModelListener;

public class FoldingController implements IModelListener {
	
    private final ProjectionAnnotationModel fAnnotationModel;
    private final CeylonFoldingUpdater fFoldingUpdater;

    public FoldingController(ProjectionAnnotationModel annotationModel) {
        super();
        this.fAnnotationModel= annotationModel;
        this.fFoldingUpdater= new CeylonFoldingUpdater();
    }

    public AnalysisRequired getAnalysisRequired() {
        return AnalysisRequired.SYNTACTIC_ANALYSIS;
    }

    public void update(CeylonParseController parseController, IProgressMonitor monitor) {
        if (fAnnotationModel != null) { // can be null if file is outside workspace
            try {
                fFoldingUpdater.updateFoldingStructure((CeylonParseController)parseController, 
                		fAnnotationModel);
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}