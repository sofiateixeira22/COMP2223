package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.function.BiFunction;

public class AstVisitor extends AJmmVisitor {


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

    private String importDeclarationVisit(JmmNode jmmNode, String s) {
        String ret = "placeholder";
        return ret;
    }

    private String classDeclarationVisit(JmmNode jmmNode, String s) {

        System.out.println("HERE...........");


        String ret = "";
        for ( JmmNode child : jmmNode . getChildren ()){
            ret += visit (child ,"");
            ret += "\n";
        }

        System.out.println(ret);

        return ret;
    }

    private String identifierVisit(JmmNode jmmNode, String s) {
        String ret = "placeholder";
        return ret;
    }

    private String integerVisit(JmmNode jmmNode, String s) {
        String ret = "placeholder";
        return ret;
    }

    private String assignmentVisit(JmmNode jmmNode, String s) {
        String ret = "placeholder";
        return ret;
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
