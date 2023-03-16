package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class AstVisitor extends AJmmVisitor {

    List<String> imports;
    String className;
    String superString;
    List<Symbol> fields;
    List<String> methods;
    Type returnType;
    List<Symbol> parameters;
    List<Symbol> localVariables;


    public AstVisitor(){
        super();
    }

    @Override
    protected void buildVisitor() {

        BiFunction<JmmNode, String, String> programVisit = this::programVisit;
        addVisit("Program", programVisit);

        BiFunction<JmmNode, String, String> assignmentVisit = this::assignmentVisit;
        addVisit ("Assignment", assignmentVisit );

        BiFunction<JmmNode, String, String> integerVisit = this::integerVisit;
        addVisit ("Integer", integerVisit );

        BiFunction<JmmNode, String, String> identifierVisit = this::identifierVisit;
        addVisit ("Identifier", identifierVisit );

        BiFunction<JmmNode, String, String> classDeclarationVisit = this::classDeclarationVisit;
        addVisit ("ClassDeclaration", classDeclarationVisit );

        BiFunction<JmmNode, String, String> importDeclarationVisit = this::importDeclarationVisit;
        addVisit ("ImportDeclaration", importDeclarationVisit );
    }


    public List<String> getImports(){return this.imports;}

    public String getClassName(){return this.className;}

    public String getSuperString(){return this.superString;}

    public List<Symbol> getFields(){return this.fields;}

    public List<String> getMethods(){return this.methods;}

    public Type getReturnType(){return this.returnType;}

    public List<Symbol> getParameters(){return this.parameters;}

    public List<Symbol> getLocalVariables(){return this.localVariables;}

    private String importDeclarationVisit(JmmNode jmmNode, String s) {


        return "";
    }
    private String classDeclarationVisit(JmmNode jmmNode, String s) {

        return "";
    }

    private String identifierVisit(JmmNode jmmNode, String s) {
        return "";
    }

    private String integerVisit(JmmNode jmmNode, String s) {
        return "";
    }

    private String assignmentVisit(JmmNode jmmNode, String s) {
        return "";
    }

    private String programVisit(JmmNode jmmNode, String s) {
        s = (s!= null ?s:"");
        String ret = s+" public class {\n";
        String s2 = s+"\t";
        ret += s2+" public static void main ( String [] args ) {\n";

        for ( JmmNode child : jmmNode . getChildren ()){
            ret += visit (child ,s2 + "\t");
            ret += "\n";
        }
        ret += s2 + "}\n";
        ret += s + "}\n";
        return ret ;
    }

    @Override
    public Object visit(JmmNode jmmNode) {
        return super.visit(jmmNode);
    }

    @Override
    public void addVisit(String kind, BiFunction method) {
        super.addVisit(kind, method);
    }
}
