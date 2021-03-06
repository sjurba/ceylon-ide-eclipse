package com.redhat.ceylon.eclipse.code.resolve;

import static com.redhat.ceylon.eclipse.core.builder.CeylonBuilder.getProjectModelLoader;
import static com.redhat.ceylon.eclipse.core.builder.CeylonBuilder.getProjectTypeChecker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import com.redhat.ceylon.compiler.loader.ModelLoader.DeclarationType;
import com.redhat.ceylon.compiler.typechecker.TypeChecker;
import com.redhat.ceylon.compiler.typechecker.context.PhasedUnit;
import com.redhat.ceylon.compiler.typechecker.context.PhasedUnits;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.Package;
import com.redhat.ceylon.compiler.typechecker.model.Parameter;
import com.redhat.ceylon.compiler.typechecker.model.Setter;
import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.eclipse.code.parse.CeylonParseController;
import com.redhat.ceylon.eclipse.core.model.loader.JDTModelLoader;
import com.redhat.ceylon.eclipse.util.FindDeclarationVisitor;

public class CeylonReferenceResolver {

//    /**
//     * Get the text associated with the given node for use in a link from (or
//     * to) that node
//     */
//    public String getLinkText(Object node) {
//        if (node instanceof Node) {
//            return getNodeDeclarationName((Node) node);
//        } 
//        else {
//            return null;
//        }
//    }
//
//    /**
//     * Get the target for the given source node in the AST produced by the given
//     * Parse Controller.
//     */
//    public Tree.Declaration getLinkTarget(Object node, 
//    		IParseController controller) {
//        if (node instanceof Node) {
//            return getReferencedNode(node, controller);
//        }
//        else {
//            return null;
//        }
//    }

    public static Tree.Declaration getReferencedNode(Node node, 
    		CeylonParseController controller) {
        return getReferencedNode(getReferencedDeclarationOrPackage(node), 
        		controller);
    }

    public static Tree.Declaration getReferencedNode(Declaration dec, 
    		CeylonParseController controller) {
        return getReferencedNode(dec, getCompilationUnit(controller,dec));
    }

    public static Declaration getReferencedDeclarationOrPackage(Node node) {
        Declaration dec;
        if (node instanceof Tree.ImportPath) {
            Package p = ((Tree.ImportPath) node).getPackageModel();
            if (p==null) {
                return null;
            }
            else {
                dec = p.getDirectMember("package", null);
                if (dec==null) {
                    dec = p.getDirectMember("module", null);
                }
            }
        }
        else {
            dec = getReferencedDeclaration((Node) node);
            if (dec instanceof Parameter) {
                Declaration pd = ((Parameter) dec).getDeclaration();
                if (pd instanceof Setter) {
                    dec = pd;
                }
                else if (dec.getName()!=null) {
                    Declaration att = pd.getMemberOrParameter(dec.getUnit(), 
                    		dec.getName(), null);
                    if (att!=null) dec = att;
                }
            }
        }
        return dec;
    }

    /*private String getNodeDeclarationName(Node node) {
        if (node instanceof Tree.MemberOrTypeExpression) {
            return ((Tree.MemberOrTypeExpression) node).getDeclaration().getName();
        } 
        else if (node instanceof Tree.SimpleType) {
            return ((Tree.SimpleType) node).getDeclarationModel().getName();
        } 
        else if (node instanceof Tree.ImportMemberOrType) {
            return ((Tree.ImportMemberOrType) node).getDeclarationModel()
                    .getName();
        }
        if (node instanceof Tree.Declaration) {
            return ((Tree.Declaration) node).getDeclarationModel()
                    .getName();
        } 
        else if (node instanceof Tree.NamedArgument) {
            return ((Tree.NamedArgument) node).getParameter().getName();
        }
        else {
            return null;
        }
    }*/

    public static Declaration getReferencedDeclaration(Node node) {
        //NOTE: this must accept a null node, returning null!
        if (node instanceof Tree.MemberOrTypeExpression) {
            return ((Tree.MemberOrTypeExpression) node).getDeclaration();
        } 
        else if (node instanceof Tree.SimpleType) {
            return ((Tree.SimpleType) node).getDeclarationModel();
        } 
        else if (node instanceof Tree.ImportMemberOrType) {
            return ((Tree.ImportMemberOrType) node).getDeclarationModel();
        } 
        else if (node instanceof Tree.Declaration) {
            return ((Tree.Declaration) node).getDeclarationModel();
        } 
        else if (node instanceof Tree.NamedArgument) {
            return ((Tree.NamedArgument) node).getParameter();
        }
        else {
            return null;
        }
    }

    public static Tree.Declaration getReferencedNode(Declaration dec,
            Tree.CompilationUnit compilationUnit) {
        if (compilationUnit==null || dec==null) {
            return null;
        }
        else {
            FindDeclarationVisitor visitor = new FindDeclarationVisitor(dec);
            compilationUnit.visit(visitor);
            //System.out.println("referenced node: " + visitor.getDeclarationNode());
            return visitor.getDeclarationNode();
        }
    }
    
    public static Tree.CompilationUnit getCompilationUnit(IProject project, 
            Declaration dec) {
        PhasedUnit phasedUnit = getPhasedUnit(project, dec);
        return phasedUnit==null ? null : phasedUnit.getCompilationUnit();
    }

    public static PhasedUnit getPhasedUnit(IProject project, 
            Declaration dec) {
        return getProjectTypeChecker(project)
                        .getPhasedUnitFromRelativePath(getRelativePath(dec));
    }

    public static Tree.CompilationUnit getCompilationUnit(CeylonParseController cpc,
            Declaration dec) {
        if (cpc==null || dec==null) {
            return null;
        }
        else {
            Tree.CompilationUnit root = cpc.getRootNode();            
            if (root!=null && root.getUnit() != null && 
                    root.getUnit().equals(dec.getUnit())) {
                return root;
            }
            else {
                TypeChecker typeChecker = cpc.getTypeChecker();
                String relativePath = getRelativePath(dec);
                PhasedUnit pu = typeChecker==null ? null : 
                    typeChecker.getPhasedUnits().getPhasedUnitFromRelativePath(relativePath);
                if (pu!=null) {
                    return pu.getCompilationUnit();
                }
                
                IProject currentProject = cpc.getProject();
                if (currentProject!=null) {
                    try {
						for (IProject project: currentProject.getReferencedProjects()) {
						    JDTModelLoader requiredProjectLoader = getProjectModelLoader(project);
						    if (requiredProjectLoader==null) {
						        continue;
						    }
						    Declaration originalDecl = requiredProjectLoader
						    		.getDeclaration(dec.getQualifiedNameString(), 
						    		        DeclarationType.TYPE);
						    if (originalDecl!=null) {
						        String fileName = originalDecl.getUnit().getFilename();
						        String packagePath = originalDecl.getUnit().getPackage()
						        		.getQualifiedNameString().replace('.', '/');
						        String fileRelativePath = packagePath + "/" + fileName;

						        TypeChecker requiredProjectTypeChecker = getProjectTypeChecker(project);
						        if (requiredProjectTypeChecker==null) {
						            continue;
						        }
						        PhasedUnit requiredProjectPhasedUnit = requiredProjectTypeChecker
						        		.getPhasedUnitFromRelativePath(fileRelativePath);
						        if (requiredProjectPhasedUnit != null 
						        		&& requiredProjectPhasedUnit.isFullyTyped()) {
						            pu = requiredProjectPhasedUnit;
						            break;
						        }
						    }
						}
					} 
                    catch (CoreException e) {
						e.printStackTrace();
					}
                }
                
                if (pu==null && typeChecker!=null) {
                    for (PhasedUnits dependencies : typeChecker.getPhasedUnitsOfDependencies()) {
                        pu = dependencies.getPhasedUnitFromRelativePath(relativePath);
                        if (pu!=null) {
                            break;
                        }
                    }
                }
                if (pu!=null) {
                    return pu.getCompilationUnit();
                }
                return null;
            }
        }
    }

    private static String getRelativePath(Declaration dec) {
        return dec.getUnit().getPackage()
                .getQualifiedNameString().replace('.', '/')
                + "/" + dec.getUnit().getFilename();
    }

}
