package com.redhat.ceylon.eclipse.imp.refactoring;

import static com.redhat.ceylon.eclipse.imp.quickfix.CeylonQuickFixAssistant.getIndent;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.texteditor.ITextEditor;

import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.eclipse.util.FindStatementVisitor;

public class ExtractValueRefactoring extends AbstractRefactoring {
	private String newName;
	private boolean explicitType;
	private boolean getter;

	public ExtractValueRefactoring(ITextEditor editor) {
	    super(editor);
	    newName = guessName();
	}
	
	/*public ExtractValueRefactoring(IQuickFixInvocationContext context) {
        super(context);
        newName = guessName();
    }*/
	
	@Override
	boolean isEnabled() {
	    return node instanceof Tree.Term;
	}

    public String getName() {
		return "Extract Value";
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// Check parameters retrieved from editor context
		return new RefactoringStatus();
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return new RefactoringStatus();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		TextChange tfc = searchInEditor() ?
		        new DocumentChange("Extract Value", document) :
		        new TextFileChange("Extract Value", sourceFile);
		extractInFile(tfc);
		return tfc;
	}

    private void extractInFile(TextChange tfc) throws CoreException {
        tfc.setEdit(new MultiTextEdit());
		IDocument doc = tfc.getCurrentDocument(null);
		
		Tree.Term term = (Tree.Term) node;
		Integer start = node.getStartIndex();
		int length = node.getStopIndex()-start+1;
		String exp = toString(term);
		FindStatementVisitor fsv = new FindStatementVisitor(term, false);
		rootNode.visit(fsv);
		Tree.Statement statNode = fsv.getStatement();
		/*if (statNode instanceof Tree.Declaration) {
			Tree.AnnotationList anns = ((Tree.Declaration) statNode).getAnnotationList();
			if (anns!=null && !anns.getAnnotations().isEmpty()) {
				statNode = anns.getAnnotations().get(0);
			}
		}*/
		tfc.addEdit(new InsertEdit(statNode.getStartIndex(),
				( explicitType ? term.getTypeModel().getProducedTypeName() : "value") + " " + 
				newName + (getter ? " { return " + exp  + "; } " : " = " + exp + ";") + 
				"\n" + getIndent(statNode, doc)));
		tfc.addEdit(new ReplaceEdit(start, length, newName));
    }

	public void setNewName(String text) {
		newName = text;
	}
	
	public String getNewName() {
		return newName;
	}
	
	public void setExplicitType() {
		this.explicitType = !explicitType;
	}
	
	public void setGetter() {
		this.getter = !getter;
	}
	
}
