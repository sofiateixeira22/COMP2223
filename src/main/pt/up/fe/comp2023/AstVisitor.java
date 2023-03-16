package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.*;
import java.util.function.BiFunction;

public class AstVisitor extends AJmmVisitor {

    List<String> imports = new ArrayList<>();
    String className;
    String superString;
    List<Symbol> fields = new ArrayList<>();
    List<String> methods = new ArrayList<>();
    Type returnType;
    Map<String, Type> methodTypes = new HashMap<>();
    List<Symbol> parameters = new ArrayList<>();
    Map<String, List<Symbol>> methodParameters = new HashMap<>();
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

        BiFunction<JmmNode, String, String> classDeclarationVisit = this::classDeclarationVisit;
        addVisit ("ClassDeclaration", classDeclarationVisit );

        BiFunction<JmmNode, String, String> importDeclarationVisit = this::importDeclarationVisit;
        addVisit ("ImportDeclaration", importDeclarationVisit );

        BiFunction<JmmNode, String, String> methodDeclarationVisit = this::methodDeclarationVisit;
        addVisit ("MethodDeclaration", methodDeclarationVisit );

        BiFunction<JmmNode, String, String> varDeclarationVisit = this::varDeclarationVisit;
        addVisit ("VarDeclaration", varDeclarationVisit );

    }

    public List<String> getImports(){return this.imports;}

    public String getClassName(){return this.className;}

    public String getSuperString(){return this.superString;}

    public List<Symbol> getFields(){return this.fields;}

    public List<String> getMethods(){return this.methods;}

    public Map<String, Type> getMethodTypes(){return this.methodTypes;}

    public Map<String, List<Symbol>> getMethodParameters(){return this.methodParameters;}

    public List<Symbol> getLocalVariables(){return this.localVariables;}


    private String classDeclarationVisit(JmmNode jmmNode, String s) {

        boolean extendedClass = jmmNode.hasAttribute("extend");

        for (JmmNode child : jmmNode.getChildren()) {
            if (Objects.equals(child.getKind(), "Identifier")) {

                if (extendedClass) {
                    switch (child.getIndexOfSelf()) {
                        case 0:
                            this.className = child.get("value");
                            break;
                        case 1:
                            this.superString = child.get("value");
                    }
                } else {
                    this.className = child.get("value");
                }
            } else {
                visit(child);
            }
        }

//        System.out.println(jmmNode.getAttributes());
//          - imprime atributos, no caso do teste Parameters não tem mas podes ver no teste ClassAndSupper
//        System.out.println(list);
//          - imprime o que está dentro do ClassDeclaration
//        System.out.println(jmmNode.getJmmChild(1));
//          - imprime o segundo filho do ClassDeclaration neste caso o MethodDeclaration
//        System.out.println(jmmNode.getNumChildren());
//          - estava a pensar em fazer um for para percorrer os filhos e encontrar o MethodDeclaration caso tivesse muitos, e isso retorna o número de filhos do ClassDeclaration
//        System.out.println(list.contains("MethodDeclaration"));
//          - dá false e não sei porquê

        return "";
    }

    private String methodDeclarationVisit(JmmNode jmmNode, String s) {

        this.parameters.clear();

        String currentMethod = "";

        for (int i=0; i<jmmNode.getChildren().size(); i++){
            JmmNode child = jmmNode.getChildren().get(i);
            boolean validParameter = Objects.equals(child.getKind(), "Identifier") || Objects.equals(child.getKind(), "Type");

            if (!validParameter || i==jmmNode.getNumChildren()-1){
                continue;
            }

            JmmNode nextChild = jmmNode.getChildren().get(i+1);

            if (i == 0){
                this.returnType = new Type(child.get("t"), false);
            }
            else if (i == 1) {
                this.methods.add(child.get("value"));
                methodTypes.put(child.get("value"), this.returnType);
                currentMethod = child.get("value");
            } else {
                if (i % 2 == 0) {
                    String typeName = child.get("t");
                    String symbolName = nextChild.get("value");

                    Type type = new Type(typeName, false);
                    Symbol symbol = new Symbol(type, symbolName);

                    this.parameters.add(symbol);
                }
            }

        }

        this.methodParameters.put(currentMethod, this.parameters);

        return "";
    }

    private String importDeclarationVisit(JmmNode jmmNode, String s) {
        var list = jmmNode.getChildren();
        for (var l:list) {
            imports.add(l.get("value"));
        }

        return "";
    }

    private String varDeclarationVisit(JmmNode jmmNode, String s) {

        String symbolName = "";
        String typeName = "";


        for (JmmNode child : jmmNode.getChildren()){
            switch (child.getIndexOfSelf()){
                case 0:
                    typeName = child.get("t");
                    break;
                case 1:
                    symbolName = child.get("value");
                    break;
            }

        }

        Type type = new Type(typeName, false);

        Symbol newSymbol = new Symbol(type, symbolName);

        this.fields.add(newSymbol);

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
