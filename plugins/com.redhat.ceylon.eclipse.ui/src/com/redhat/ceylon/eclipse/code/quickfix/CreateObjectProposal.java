package com.redhat.ceylon.eclipse.code.quickfix;

import static com.redhat.ceylon.eclipse.code.outline.CeylonLabelProvider.CORRECTION;
import static com.redhat.ceylon.eclipse.code.quickfix.CeylonQuickFixAssistant.getIndent;
import static com.redhat.ceylon.eclipse.code.quickfix.CeylonQuickFixAssistant.importType;
import static com.redhat.ceylon.eclipse.code.quickfix.CreateSubtypeProposal.subtypeDeclaration;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;

import com.redhat.ceylon.compiler.typechecker.model.ClassOrInterface;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.IntersectionType;
import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Statement;
import com.redhat.ceylon.eclipse.code.editor.Util;
import com.redhat.ceylon.eclipse.code.quickfix.CreateSubtypeProposal.CreateSubtype;
import com.redhat.ceylon.eclipse.util.FindStatementVisitor;

class CreateObjectProposal extends ChangeCorrectionProposal {
    
    final int offset;
    final int length;
    final IFile file;
    
    CreateObjectProposal(ProducedType type, int offset, int length, 
            IFile file, TextChange change) {
        super("Create instance of '" + type.getProducedTypeName() + "'", 
                change, 10, CORRECTION);
        this.offset=offset;
        this.length=length;
        this.file=file;
    }
    
    @Override
    public void apply(IDocument document) {
        super.apply(document);
        Util.gotoLocation(file, offset, length);
    }

    static void addCreateObjectProposal(IDocument doc, Tree.CompilationUnit cu,
            Collection<ICompletionProposal> proposals, IFile file,
            Node node) {
        FindStatementVisitor fsv = new FindStatementVisitor(node, false);
        fsv.visit(cu);
        Statement s = fsv.getStatement();
        if (s!=null) {
            ProducedType type = CreateSubtypeProposal.getType(cu, node);
            if (type!=null && 
                    (type.getDeclaration() instanceof ClassOrInterface &&
                            type.getDeclaration().isExtendable() ||
                     type.getDeclaration() instanceof IntersectionType)) {
                TextChange change = new DocumentChange("Create Object", doc);
                change.setEdit(new MultiTextEdit());
                Integer offset = s.getStartIndex();
                String name = type.getDeclaration().getName().replace("&", "")
                		.replace("<", "").replace(">", "");
                CreateSubtype cs = subtypeDeclaration(type, 
                		cu.getUnit().getPackage(), true);
                int shift=0;
            	HashSet<Declaration> already = new HashSet<Declaration>();
                for (ProducedType pt: cs.getImportedTypes()) {
                	shift+=importType(change, pt, cu, already);
                }
				String dec = cs.getDefinition().replace("$className", "my" + name) + "\n";
                dec = dec.replaceAll("\n", "\n" + getIndent(node, doc));
                change.addEdit(new InsertEdit(offset,dec));
                proposals.add(new CreateObjectProposal(type, 
                        offset+7+shift, name.length()+2, file, change));
            }
        }
    }
    
}