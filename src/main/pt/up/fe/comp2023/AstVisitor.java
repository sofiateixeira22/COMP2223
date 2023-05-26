package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.*;
import java.util.function.BiFunction;

public class AstVisitor extends AJmmVisitor {

    String methodName;
    List<String> imports = new ArrayList<>();
    String className;
    String superString;
    List<Symbol> fields = new ArrayList<>();
    List<String> methods = new ArrayList<>();
    Map<String, Type> methodTypes = new HashMap<>();
    Map<String, List<Symbol>> methodParameters = new HashMap<>();
    Map<String, List<Symbol>> localVariables = new HashMap<>();
    List<Symbol> auxLocalVariables = new ArrayList<>();


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

    public Map<String, List<Symbol>> getLocalVariables(){return this.localVariables;}

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

        return "";
    }

    private String methodDeclarationVisit(JmmNode jmmNode, String s) {

        List<Symbol> parameters = new ArrayList<>();

        this.auxLocalVariables = new ArrayList<>();
        this.methodName = jmmNode.getJmmChild(1).get("value");

        boolean hasVars = false;

        String currentMethod = "";

        String type = "Type";
        String identifier = "Identifier";
        String varDeclaration = "VarDeclaration";

        Type methodReturnType;

        for (int i=0; i<jmmNode.getNumChildren(); i=i+1){

            JmmNode child = jmmNode.getJmmChild(i);

            String childKind = child.getKind();

            boolean validParameter = Objects.equals(child.getKind(), identifier) || Objects.equals(child.getKind(), type) || Objects.equals(child.getKind(), varDeclaration);

            if (!validParameter){
                continue;
            }

            if (i == 0){
                JmmNode nextChild = jmmNode.getJmmChild(i+1);
                String last2Chars = child.get("t").length() > 2 ? child.get("t").substring(child.get("t").length() - 2) : child.get("t");
                boolean isArray = (Objects.equals(last2Chars, "[]"));
                String returnType = child.get("t");

                if (isArray){
                    returnType = child.get("t").substring(0,child.get("t").length() - 2);
                }

                methodReturnType = new Type(returnType, isArray);
                this.methods.add(nextChild.get("value"));
                methodTypes.put(nextChild.get("value"), methodReturnType);
                currentMethod = nextChild.get("value");
            } else if (childKind.equals(type)){
                JmmNode nextChild = jmmNode.getJmmChild(i+1);
                String last2Chars = child.get("t").length() > 2 ? child.get("t").substring(child.get("t").length() - 2) : child.get("t");
                boolean isArray = (Objects.equals(last2Chars, "[]"));
                String typeName = child.get("t");
                String symbolName = nextChild.get("value");

                Type mtype = new Type(typeName, isArray);
                Symbol symbol = new Symbol(mtype, symbolName);

                parameters.add(symbol);
            }
            else if (childKind.equals(varDeclaration)){
                hasVars = true;
                visit(child);
            }
        }

        this.methodParameters.put(currentMethod, parameters);

        if (!hasVars){
            this.localVariables.put(this.methodName, new ArrayList<>());
        }

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

        String methodCaller = jmmNode.getJmmParent().toString();


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

        boolean isArray = false;
        String substring = typeName.substring(Math.max(typeName.length() - 2, 0));
        String returnType = typeName;

        if (substring.equals("[]")){
            isArray = true;
            returnType = typeName.substring(0,typeName.length() - 2);
        }



        Type type = new Type(returnType, isArray);

        Symbol newSymbol = new Symbol(type, symbolName);

        if (methodCaller.equals("MethodDeclaration")){
            this.auxLocalVariables.add(newSymbol);
            this.localVariables.put(this.methodName, this.auxLocalVariables);
        }
        else {
            this.fields.add(newSymbol);
        }

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
            visit (child ,s2 + "\t");
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
