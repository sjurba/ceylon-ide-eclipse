package com.redhat.ceylon.eclipse.imp.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

public class Util {
    
    public static IProject getProject(IEditorInput editor) {
        if (editor instanceof IFileEditorInput) {
            return ((IFileEditorInput) editor).getFile().getProject();
        }
        else {
            return null;
        }
    }

    public static IFile getFile(IEditorInput editor) {
        if (editor instanceof IFileEditorInput) {
            return ((IFileEditorInput) editor).getFile();
        }
        else {
            return null;
        }
    }

    public static ITextSelection getSelection(ITextEditor textEditor) {
        return (ITextSelection) textEditor.getSelectionProvider().getSelection();
    }
    
    public static String getSelectionText(ITextEditor textEditor) {
        ITextSelection sel = getSelection(textEditor);
        IFileEditorInput fileEditorInput= (IFileEditorInput) textEditor.getEditorInput();
        IDocument document = textEditor.getDocumentProvider().getDocument(fileEditorInput);
        try {
            return document.get(sel.getOffset(), sel.getLength());
        } 
        catch (BadLocationException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void gotoLocation(final IResource file, final int offset) {
        gotoLocation(file, offset, 0);
    }
    
    public static void gotoLocation(final IResource file, final int offset, int length) {
        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(IMarker.CHAR_START, offset);
        map.put(IMarker.CHAR_END, offset+length);
        map.put(IDE.EDITOR_ID_ATTR, "org.eclipse.imp.runtime.impEditor");
        try {
            IMarker marker = file.createMarker(IMarker.TEXT);
            marker.setAttributes(map);
            IDE.openEditor(page, marker);
            marker.delete();
        }
        catch (CoreException ce) {} //deliberately swallow it
        /*try {
            IEditorPart editor = EditorUtility.isOpenInEditor(path);
            if (editor == null) {
                editor = EditorUtility.openInEditor(path);
            }
            EditorUtility.revealInEditor(editor, targetOffset, 0);
        } catch (PartInitException e) {
            RuntimePlugin.getInstance().logException("Unable to open declaration", e);
        }*/
    }
    
    public static IEditorPart getCurrentEditor() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage()
                .getActiveEditor();
    }
    
}