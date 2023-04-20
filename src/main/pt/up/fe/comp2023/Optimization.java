package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.*;

public class Optimization implements JmmOptimization {
    private StringBuilder code;
    private SymbolTable symbolTable;
    private int indexFirstLevel = 0;
    private int indexSecondLevel = 0;
    private JmmSemanticsResult jmmSemanticsResult;
    private StringBuilder operationBuilder = new StringBuilder();
    private List<String> imports = new ArrayList<>();

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        this.code = new StringBuilder();
        this.jmmSemanticsResult = jmmSemanticsResult;
        this.symbolTable = jmmSemanticsResult.getSymbolTable();

        importVisit();
        classDeclarationVisit();

        System.out.println("\n** Code **\n\n" + this.code);

        System.out.println(jmmSemanticsResult.getRootNode().toTree()); //Gets the tree

        return new OllirResult(jmmSemanticsResult, code.toString(), new ArrayList<>());
    }

    public void importVisit() {
        System.out.println("** Import Visit **");

        var root = jmmSemanticsResult.getRootNode();

        int numImports = 0;
        for(var child : root.getChildren())
            if(child.getKind().equals("ImportDeclaration")) numImports+=1;

        for(var i = 0; i < numImports; i++) {
            indexFirstLevel+=1;
            this.code.append("import ");
            if(root.getJmmChild(i).getNumChildren() > 1) {
                var size = root.getJmmChild(i).getNumChildren();
                for (int j = 0; j < size-1; j++) {
                    this.code.append(root.getJmmChild(i).getJmmChild(j).get("value") + ".");
                }
                this.code.append(root.getJmmChild(i).getJmmChild(size-1).get("value") + ";\n");
                imports.add(root.getJmmChild(i).getJmmChild(size-1).get("value"));
            } else {
                this.code.append(root.getJmmChild(i).getJmmChild(0).get("value") + ";\n");
                imports.add(root.getJmmChild(i).getJmmChild(0).get("value"));
            }
        }

        this.code.append("\n");
    }

    public void classDeclarationVisit() {
        System.out.println("** Class Declaration Visit **");
        indexFirstLevel +=1;

        this.code.append(this.symbolTable.getClassName() + " ");
        indexSecondLevel+=1;

        var superClass = this.symbolTable.getSuper();
        if(superClass != null) {
            indexSecondLevel+=1;
            this.code.append("extends " + superClass + " ");
        }
        this.code.append("{\n");

        varDeclarationVisit(); //global variables

        this.code.append("\n\t.construct " + this.symbolTable.getClassName() + "().V {\n");
        this.code.append("\t\tinvokespecial(this, \"<init>\").V;\n");
        this.code.append("\t}\n\n");

        methodVisit();

        this.code.append("}");
    }

    public void varDeclarationVisit() {
        System.out.println("** Var Declaration Visit **");

        this.code.append("\n");
        for(var fieldString: this.symbolTable.getFields()) {
            indexSecondLevel+=1;
            this.code.append("\t.field public " + fieldString.getName());

            var type = fieldString.getType();
            if(type.isArray()) this.code.append(".array");
            if(type.getName().equals("int[]") || type.getName().equals("int")) this.code.append(".i32");
            if(type.getName().equals("boolean[]") || type.getName().equals("boolean")) this.code.append(".bool");
            if(type.getName().equals(("String[]")) || type.getName().equals("String")) this.code.append(".String");

            this.code.append(";\n");
        }
        //this.code.append("\n");
    }

    public void methodVisit() {
        System.out.println("** Method Visit **");

        for(var methodString: this.symbolTable.getMethods()) {
            indexSecondLevel+=1;

            if(methodString.equals("main")) this.code.append("\t.method public static " + methodString + "(");
            else this.code.append("\t.method public " + methodString + "(");

            methodParametersVisit(methodString);

            this.code.append(")");

            var returnType = this.symbolTable.getReturnType(methodString).getName();

            if(returnType.equals("int")) this.code.append(".i32 ");
            else if (returnType.equals("boolean")) this.code.append(".bool ");
            else if(returnType.equals("void")) this.code.append(".V ");
            else this.code.append("." + returnType + " ");

            this.code.append("{\n");

            var localVariables = this.symbolTable.getLocalVariables(methodString);
            System.out.println("hello: " + localVariables);

            var method = this.jmmSemanticsResult.getRootNode().getJmmChild(this.indexFirstLevel-1).getJmmChild(this.indexSecondLevel-1);

            for(JmmNode jmmNode : method.getChildren()) {
                if(jmmNode.getKind().equals("Statement")) {
                    statementVisit(jmmNode, localVariables);
                }
            }

            if(!returnType.equals("void")) {
                var returnValue = getReturnVar(returnType, localVariables);
                if(returnType.equals("int")) this.code.append("\n\t\tret.i32 " + returnValue + ";\n");
                if(returnType.equals("boolean")) this.code.append("\n\t\tret.bool " + returnValue + ";\n");
            }

        this.code.append("\t}\n\n");
        }
    }

    public void methodParametersVisit(String methodString) {
        System.out.println("** Method Parameters Visit **");

        for(var parameterString: this.symbolTable.getParameters(methodString)) {
            this.code.append(parameterString.getName());

            var type = parameterString.getType();
            if(type.isArray()) this.code.append(".array");
            if(type.getName().equals("int[]") || type.getName().equals("int")) this.code.append(".i32");
            else if(type.getName().equals("boolean[]") || type.getName().equals("boolean")) this.code.append(".bool");
            else if(type.getName().equals(("String[]")) || type.getName().equals("String")) this.code.append(".String");
            else this.code.append("." + type.getName());

            var size = this.symbolTable.getParameters(methodString).size();
            if(size > 1 && parameterString != this.symbolTable.getParameters(methodString).get(size-1))
                this.code.append(", ");

        }

    }

    public void statementVisit(JmmNode jmmNode, List<Symbol> localVariables) {
        if(jmmNode.getJmmChild(0).getKind().equals("AssignmentOp")) {
            assignmentVisit(jmmNode.getJmmChild(0), localVariables);
        }
        if(jmmNode.getJmmChild(0).getKind().equals("MethodCall")) {
            methodCallVisit(jmmNode.getJmmChild(0), localVariables);
        }
    }

    public void assignmentVisit(JmmNode jmmNode, List<Symbol> localVariables) {
        var dest = jmmNode.getJmmChild(0).get("value");

        if(jmmNode.getJmmChild(1).hasAttribute("value")) {
            var value = jmmNode.getJmmChild(1).get("value");

            for(var localVar: localVariables)
                if(localVar.getName().equals(dest)) {
                    if(localVar.getType().getName().equals("int")) this.code.append("\t\t" + dest + ".i32 :=.i32 " + value + ".i32;\n");
                    if(localVar.getType().getName().equals("bool")) this.code.append("\t\t" + dest + ".bool :=.bool " + value + ".bool;\n");
                }
        }

        if(jmmNode.getJmmChild(1).getKind().equals("AdditiveOp") || jmmNode.getJmmChild(1).getKind().equals("MultiplicativeOp")) {
            StringBuilder returnType = new StringBuilder();
            for(var localVar: localVariables)
                if(localVar.getName().equals(dest))
                    if(localVar.getType().getName().equals("int")) {
                        returnType.append("int");
                        var operation = operationVisit(jmmNode.getJmmChild(1), localVariables, 0, returnType.toString(), new StringBuilder());
                        this.code.append("\t\t" + dest + ".i32 :=.i32 " + operation + ".i32;\n");
                    }
        }
    }

    public String getType(List<Symbol> localVariables, String var) {
        String type = "";
        for(var localVar : localVariables) {
            if(localVar.getName().equals(var)) type = localVar.getType().getName();
        }
        return type;
    }

    public void methodCallVisit(JmmNode jmmNode, List<Symbol> localVariables) {
        var method = jmmNode.getJmmChild(1).get("value");
        var dest = jmmNode.getJmmChild(0).get("value");
        System.out.println("method: " + method);
        System.out.println("dest: " + dest);
        System.out.println(jmmNode.getChildren());

        //if children are dest, method and at least one param
        if(jmmNode.getNumChildren() > 2) {
            Boolean isVar = false;
            for(var localVar: localVariables) {
                if(localVar.getName().equals(dest)) isVar=true;
            }
            if(!isVar && this.imports.contains(dest)) {
                //não é var e está nos imports -> invoke static
                this.code.append("\t\tinvokestatic(" + dest);
            } else {
                //é var -> invoke virtual
                this.code.append("\t\tinvokevirtual(" + dest);
                var type = getType(localVariables, dest);
                if(type.equals("int")) this.code.append(".i32");
                else if(type.equals("boolean")) this.code.append(".bool");
                else this.code.append("." + type);
            }

            this.code.append(", \"" + method + "\", ");
            if(jmmNode.getNumChildren() > 3) {
                for(int i = 2; i < jmmNode.getNumChildren()-1; i++) {
                    this.code.append(jmmNode.getJmmChild(i).get("value") + ".");
                    var type = getType(localVariables, jmmNode.getJmmChild(i).get("value"));
                    if(type.equals("int")) this.code.append("i32");
                    else if(type.equals("boolean")) this.code.append("bool");
                    else this.code.append(type);
                    this.code.append(", ");
                }
            }
            this.code.append(jmmNode.getJmmChild(jmmNode.getNumChildren()-1).get("value") + ".");
            var type = getType(localVariables, jmmNode.getJmmChild(jmmNode.getNumChildren()-1).get("value"));
            if(type.equals("int")) this.code.append("i32");
            else if(type.equals("boolean")) this.code.append("bool");
            else this.code.append(type);
            this.code.append(").V;\n");
        } else {
            var type = getType(localVariables, dest);
            this.code.append("\t\tinvokevirtual(" + dest + ".");
            if(type.equals("int")) this.code.append("i32");
            else if(type.equals("boolean")) this.code.append("bool");
            else this.code.append(type);
            this.code.append(", \"" + method + "\").V;\n");
        }
    }

    public String operationVisit(JmmNode jmmNode, List<Symbol> localVariables, int index, String type, StringBuilder tmp) {
        var op = jmmNode.get("op");
        index+=1;

        //left is recursive
        var left = jmmNode.getJmmChild(0);
        if(left.hasAttribute("value")) {
            for(var localVar : localVariables) {
                if(localVar.getName().equals(left.get("value")))
                    if(localVar.getType().getName().equals("int")) this.operationBuilder.append(left.get("value") + ".i32");
            }
            if(left.getKind().equals("Integer")) this.operationBuilder.append(left.get("value") + ".i32");
        } else {
            operationVisit(left, localVariables, index, type, tmp);
            this.operationBuilder.append(tmp + ".i32");
        }

        this.operationBuilder.append(" " + op + ".i32 ");

        //right
        var right = jmmNode.getJmmChild(1);
        if(right.hasAttribute("value")) {
            for(var localVar : localVariables) {
                if(localVar.getName().equals(right.get("value")))
                    if (localVar.getType().getName().equals("int")) this.operationBuilder.append(right.get("value") + ".i32");
            }
            if(right.getKind().equals("Integer")) this.operationBuilder.append(right.get("value") + ".i32");
        } else {
            operationVisit(right, localVariables, index, type, tmp);
            this.operationBuilder.append(tmp + ".i32");
        }

        if(type.equals("int")) this.code.append("\t\tt" + index + ".i32 :=.i32 " + this.operationBuilder.toString() + ";\n");

        if(!tmp.isEmpty()) tmp.delete(0, tmp.length());

        tmp.append("t" + index);

        this.operationBuilder.delete(0, this.operationBuilder.length());

        return tmp.toString();
    }

    public String getReturnVar(String returnType, List<Symbol> localVariables) {
        var method = this.jmmSemanticsResult.getRootNode().getJmmChild(this.indexFirstLevel-1).getJmmChild(this.indexSecondLevel-1);
        StringBuilder returnVar = new StringBuilder();
        if(method.getJmmChild(method.getNumChildren()-1).hasAttribute("value")) {
                returnVar.append(method.getJmmChild(method.getNumChildren()-1).get("value"));
            if(returnType.equals("int")) {
                returnVar.append(".i32");
            }
            if(returnType.equals("boolean")) {
                returnVar.append(".bool");
            }
        }
        //System.out.println(method.getJmmChild(method.getNumChildren()-1));
        if(method.getJmmChild(method.getNumChildren()-1).getKind().equals("AdditiveOp")) {
            var operation = operationVisit(method.getJmmChild(method.getNumChildren()-1), localVariables, 0, returnType, new StringBuilder());
            returnVar.append(operation + ".i32");
        }
        return returnVar.toString();
    }
}
