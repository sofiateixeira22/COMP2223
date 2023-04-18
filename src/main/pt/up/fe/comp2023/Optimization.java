package pt.up.fe.comp2023;

import org.specs.comp.ollir.*;
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

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        this.code = new StringBuilder();
        this.jmmSemanticsResult = jmmSemanticsResult;
        this.symbolTable = jmmSemanticsResult.getSymbolTable();
        System.out.println(this.symbolTable.print());

        importVisit();
        classDeclarationVisit();

        System.out.println("\n** Code **\n\n" + this.code);

        System.out.println(jmmSemanticsResult.getRootNode().toTree()); //Gets the tree

        return new OllirResult(jmmSemanticsResult, code.toString(), new ArrayList<>());
    }

    public void importVisit() {
        System.out.println("** Import Visit **");

        for(var importString: this.symbolTable.getImports()) {
            indexFirstLevel+=1;
            this.code.append("import " + importString + ";\n");
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

        this.code.append("\t.construct " + this.symbolTable.getClassName() + "().V {\n");
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

            System.out.println(fieldString);

            if(fieldString.getType().isArray()) {
                this.code.append(".array");
                if(fieldString.getType().getName().equals("int[]"))
                    this.code.append(".i32");
                else if (fieldString.getType().getName().equals("boolean[]"))
                    this.code.append(".bool");
                else if (fieldString.getType().getName().equals("String[]"))
                    this.code.append(".String");
            } else {
                if(fieldString.getType().getName().equals("int"))
                    this.code.append(".i32");
                else if(fieldString.getType().getName().equals("boolean"))
                    this.code.append(".bool");
                else if(fieldString.getType().getName().equals("String"))
                    this.code.append(".String");
                else this.code.append(".V");
            }


            this.code.append(";\n");
        }
        //this.code.append("\n");
    }

    public void methodVisit() {
        System.out.println("** Method Visit **");

        for(var methodString: this.symbolTable.getMethods()) {
            indexSecondLevel+=1;
            if(methodString.equals("main"))
                this.code.append("\t.method public static " + methodString + "(");
            else
                this.code.append("\t.method public " + methodString + "(");

            methodParametersVisit(methodString);

            this.code.append(")");

            var returnType = this.symbolTable.getReturnType(methodString).getName();

            if(returnType.equals("int"))
                this.code.append(".i32 ");
            else if (returnType.equals("boolean"))
                this.code.append(".bool ");
            else this.code.append(".V ");

            this.code.append("{\n");

            var localVariables = this.symbolTable.getLocalVariables(methodString);

            var method = this.jmmSemanticsResult.getRootNode().getJmmChild(this.indexFirstLevel-1).getJmmChild(this.indexSecondLevel-1);

            for(JmmNode jmmNode : method.getChildren()) {
                if(jmmNode.getKind().equals("Statement")) {
                    var statement = statementVisit(jmmNode);
                    for(var localVar : localVariables) {
                        if(!statement.isEmpty() && statement.get(0).equals("Assignment")) {
                            if(localVar.getName().equals(statement.get(1))) {
                                if(localVar.getType().getName().equals("int")) {
                                    this.code.append("\t\t" + statement.get(1) + ".i32 :=.i32 " + statement.get(2) + ".i32;\n");
                                }
                                if(localVar.getType().getName().equals("bool")) {
                                    this.code.append("\t\t" + statement.get(1) + ".bool :=.bool " + statement.get(2) + ".bool;\n");
                                }
                            }
                        }
                        if(!statement.isEmpty() && statement.get(0).equals("MethodInvocation")) {
                            if(localVar.getName().equals(statement.get(3))) {
                                if(localVar.getType().getName().equals("int")) {
                                    this.code.append("\t\tinvokestatic(" + statement.get(2) + ", \"" + statement.get(1) + "\", " + statement.get(3) + ".i32).V;\n");
                                }
                                if(localVar.getType().getName().equals("boolean")) {
                                    this.code.append("\t\tinvokestatic(" + statement.get(2) + ", \"" + statement.get(1) + "\", " + statement.get(3) + ".bool).V;\n");
                                }
                            }
                        }
                    }
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

            if(parameterString.getType().isArray()) {
                this.code.append(".array");
                if(parameterString.getType().getName().equals("int[]"))
                    this.code.append(".i32");
                else if (parameterString.getType().getName().equals("boolean[]"))
                    this.code.append(".bool");
                else if (parameterString.getType().getName().equals("String[]"))
                    this.code.append(".String");
            }

            if(parameterString.getType().getName().equals("int"))
                this.code.append(".i32");
            else if (parameterString.getType().getName().equals("boolean"))
                this.code.append(".bool");
            else if (parameterString.getType().getName().equals("String"))
                this.code.append(".String");

            var size = this.symbolTable.getParameters(methodString).size();
            if(size > 1 && parameterString != this.symbolTable.getParameters(methodString).get(size-1))
                code.append(", ");

        }

    }

    public List statementVisit(JmmNode jmmNode) {
        if(jmmNode.getJmmChild(0).getKind().equals("AssignmentOp")) {
            return assignmentVisit(jmmNode.getJmmChild(0));
        }
        if(jmmNode.getJmmChild(0).getKind().equals("MethodInvocation")) {
            return methodInvocationVisit(jmmNode.getJmmChild(0));
        }
        return new ArrayList<>();
    }

    public List assignmentVisit(JmmNode jmmNode) {
        List<String> assignment = new ArrayList<>();
        assignment.add("Assignment");
        assignment.add(jmmNode.getJmmChild(0).get("value"));
        assignment.add(jmmNode.getJmmChild(1).get("value"));

        return assignment;
    }

    public List methodInvocationVisit(JmmNode jmmNode) {
        List<String> methodInvocation = new ArrayList<>();
        methodInvocation.add("MethodInvocation");
        methodInvocation.add(jmmNode.get("value"));
        methodInvocation.add(jmmNode.getJmmChild(0).get("value"));
        methodInvocation.add(jmmNode.getJmmChild(1).get("value"));

        return methodInvocation;
    }

    public List operationVisit(JmmNode jmmNode, List<Symbol> localVariables) {

        List<String> operation = new ArrayList<>();
        var op = jmmNode.get("op");

        var left = jmmNode.getJmmChild(0).get("value");
        for(var localVar : localVariables) {
            if(localVar.getName().equals(left)) {
                if(localVar.getType().getName().equals("int"))
                    operation.add(left + ".i32 ");
            }
        }
        operation.add(op + ".i32 ");

        if(jmmNode.getJmmChild(1).hasAttribute("value")) {
            var right = jmmNode.getJmmChild(1).get("value");
            for(var localVar : localVariables) {
                if(localVar.getName().equals(right)) {
                    if(localVar.getType().getName().equals("int"))
                        operation.add(right + ".i32");
                }
            }
        } else {
            operationVisit(jmmNode.getJmmChild(1), localVariables);
        }

        return operation;
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
            var operation = operationVisit(method.getJmmChild(method.getNumChildren()-1), localVariables);
            if(returnType.equals("int")) {
                this.code.append("\t\tt0.i32 :=.i32 ");
            }
            for(var value : operation) {
                this.code.append(value);
            }
            this.code.append(";\n");
            returnVar.append("t0.i32");
        }
        return returnVar.toString();
    }
}
