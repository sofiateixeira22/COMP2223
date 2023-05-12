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
    private int indexIf = 0;
    private int indexWhile = 0;
    private int indexArray = 0;
    private JmmSemanticsResult jmmSemanticsResult;
    private StringBuilder operationBuilder = new StringBuilder();
    private List<String> imports = new ArrayList<>();
    private List<Symbol> localVariables = new ArrayList<>();
    private List<Symbol> methodParam = new ArrayList<>();
    private List<Symbol> fields = new ArrayList<>();

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        this.code = new StringBuilder();
        this.jmmSemanticsResult = jmmSemanticsResult;
        this.symbolTable = jmmSemanticsResult.getSymbolTable();

        importVisit();
        classDeclarationVisit();

        System.out.println("\n** Code **\n\n" + this.code);

        return new OllirResult(jmmSemanticsResult, code.toString(), new ArrayList<>());
    }

    public void importVisit() {
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
        this.code.append("\n");

        this.fields = this.symbolTable.getFields();

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
    }

    public void methodVisit() {
        for(var methodString: this.symbolTable.getMethods()) {
            indexSecondLevel+=1;

            if(methodString.equals("main")) this.code.append("\t.method public static " + methodString + "(");
            else this.code.append("\t.method public " + methodString + "(");

            methodParametersVisit(methodString);

            this.code.append(")");

            var method = this.jmmSemanticsResult.getRootNode().getJmmChild(this.indexFirstLevel-1).getJmmChild(this.indexSecondLevel-1);

            var returnType = method.getJmmChild(0).get("t");

            if(returnType.equals("int[]") || returnType.equals("boolean[]")) this.code.append(".array");

            if(returnType.equals("int") || returnType.equals("int[]")) this.code.append(".i32 ");
            else if (returnType.equals("boolean") || returnType.equals("boolean[]")) this.code.append(".bool ");
            else if(returnType.equals("void")) this.code.append(".V ");
            else this.code.append("." + returnType + " ");

            this.code.append("{\n");

            this.localVariables = this.symbolTable.getLocalVariables(methodString);

            this.indexIf = 0;
            this.indexWhile = 0;
            this.indexArray = 0;

            for(JmmNode jmmNode : method.getChildren()) {
                if(jmmNode.getKind().equals("Statement"))
                    statementVisit(jmmNode);
            }

            if(method.getJmmChild(method.getNumChildren()-1).getJmmChild(0).getJmmChild(0).getKind().equals("AdditiveOp")) {
                this.code.append(getReturnVar(returnType));
            } else {
                this.code.append("\n\t\tret");
                if(!returnType.equals("void")) {
                    var returnValue = getReturnVar(returnType);
                    if(returnType.equals("int[]") || returnType.equals("boolean[]")) this.code.append(".array");
                    if(returnType.equals("int") || returnType.equals("int[]")) this.code.append(".i32 " + returnValue + ";\n");
                    if(returnType.equals("boolean") || returnType.equals("boolean[]")) this.code.append(".bool " + returnValue + ";\n");
                } else
                    this.code.append(".V;\n");
            }

            this.code.append("\t}\n\n");
        }
    }

    public void methodParametersVisit(String methodString) {
        this.methodParam = this.symbolTable.getParameters(methodString);

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

    public void statementVisit(JmmNode jmmNode) {
        if(jmmNode.getJmmChild(0).getKind().equals("AssignmentOp"))
            assignmentVisit(jmmNode.getJmmChild(0));

        if(jmmNode.getJmmChild(0).getKind().equals("MethodCall"))
            methodCallVisit(jmmNode.getJmmChild(0));

        if(jmmNode.hasAttribute("stmt")) {
            if(jmmNode.get("stmt").equals("if")) ifVisit(jmmNode);
            if(jmmNode.get("stmt").equals("while")) whileVisit(jmmNode);
        }
    }

    public void assignmentVisit(JmmNode jmmNode) {
        var dest = jmmNode.getJmmChild(0).get("value");

        if(jmmNode.getJmmChild(1).hasAttribute("value")) {
            var value = jmmNode.getJmmChild(1).get("value");

            this.code.append("\t\t" + dest);
            var type = getType(dest);
            if(type.equals("int")) this.code.append(".i32 :=.i32 " + value + ".i32;\n");
            else if(type.equals("boolean")) this.code.append(".bool :=.bool " + value + ".bool;\n");
            else this.code.append("." + type + " :=." + type + " " + value + "." + type + ";\n");
        }

        if(jmmNode.getJmmChild(1).getKind().equals("AdditiveOp") || jmmNode.getJmmChild(1).getKind().equals("MultiplicativeOp")) {
            StringBuilder returnType = new StringBuilder();
            var type = getType(dest);
            if(type.equals("int")) returnType.append("int");
            var operation = operationVisit(jmmNode.getJmmChild(1), 0, returnType.toString(), new StringBuilder());
            this.code.append("\t\t" + dest + ".i32 :=.i32 " + operation + ".i32;\n");
        }

        if(jmmNode.getJmmChild(1).getKind().equals("LogicalOp")) {
            var type = getType(dest);
            this.code.append("\t\t" + dest);
            if(type.equals("int")) this.code.append(".i32 :=.i32 ");
            else if(type.equals("boolean")) this.code.append(".bool :=.bool ");
            else this.code.append("." + type + ":=." + type + " ");

            var node = jmmNode.getJmmChild(1);
            var op = node.get("op");

            if(node.getJmmChild(0).getKind().equals("Boolean"))
                this.code.append(node.getJmmChild(0).get("value") + ".bool ");
            this.code.append(op + ".bool ");
            if(node.getJmmChild(1).getKind().equals("Boolean"))
                this.code.append(node.getJmmChild(1).get("value") + ".bool");
            this.code.append(";\n");
        }

        if(jmmNode.getJmmChild(1).getKind().equals("RelationalOp")) {
            var type = getType(dest);
            this.code.append("\t\t" + dest);
            if(type.equals("int")) this.code.append(".i32 :=.i32 ");
            else if(type.equals("boolean")) this.code.append(".bool :=.bool ");
            else this.code.append("." + type + ":=." + type + " ");

            var node = jmmNode.getJmmChild(1);
            var op = node.get("op");

            if(node.getJmmChild(0).getKind().equals("Integer"))
                this.code.append(node.getJmmChild(0).get("value") + ".i32 ");
            this.code.append(op + ".bool ");
            if(node.getJmmChild(1).getKind().equals("Integer"))
                this.code.append(node.getJmmChild(1).get("value") + ".i32");
            this.code.append(";\n");
        }

        if(jmmNode.getJmmChild(1).getKind().equals("ArrayNew")) {
            this.indexArray+=1;
            arrayVisit(jmmNode.getJmmChild(1), dest);
        }
    }

    public String getType(String var) {
        String type = "";
        for(var localVar : this.localVariables) {
            if(localVar.getName().equals(var)) type = localVar.getType().getName();
        }
        for(var param : this.methodParam) {
            if(param.getName().equals(var)) type = param.getType().getName();
        }
        for(var field : this.fields) {
            if(field.getName().equals(var)) type = field.getType().getName();
        }
        return type;
    }

    public void methodCallVisit(JmmNode jmmNode) {
        var method = jmmNode.getJmmChild(1).get("value");
        var dest = jmmNode.getJmmChild(0).get("value");

        StringBuilder methodcall = new StringBuilder();

        var isVar = getType(dest);
        if(isVar.equals("") && this.imports.contains(dest)) {
            methodcall.append("\t\tinvokestatic(" + dest);
        } else {
            methodcall.append("\t\tinvokevirtual(" + dest);

            if(jmmNode.getJmmChild(0).getKind().equals("Integer"))
                methodcall.append(".i32");
            if(jmmNode.getJmmChild(0).getKind().equals("Boolean"))
                methodcall.append(".bool");
            else {
                var type = getType(dest);
                if(type.equals("int")) methodcall.append(".i32");
                else if(type.equals("boolean")) methodcall.append(".bool");
                else methodcall.append("." + type);
            }
        }
        if(jmmNode.getNumChildren() > 2) {
            methodcall.append(", \"" + method + "\", ");
            if(jmmNode.getNumChildren() > 3) {
                for(int i = 2; i < jmmNode.getNumChildren()-1; i++) {
                    methodcall.append(jmmNode.getJmmChild(i).get("value") + ".");
                    if(jmmNode.getJmmChild(i).getKind().equals("Integer"))
                        methodcall.append("i32");
                    else if(jmmNode.getJmmChild(i).getKind().equals("Boolean"))
                        methodcall.append("bool");
                    else {
                        var type = getType(jmmNode.getJmmChild(i).get("value"));
                        if(type.equals("int")) this.code.append("i32");
                        else if(type.equals("boolean")) this.code.append("bool");
                        else methodcall.append(type);
                    }
                    methodcall.append(", ");
                }
            }
            var node = jmmNode.getJmmChild(jmmNode.getNumChildren()-1);
            if(node.hasAttribute("value")) {
                methodcall.append(node.get("value") + ".");

                if(node.getKind().equals("Integer")) methodcall.append("i32");
                else if(node.getKind().equals("Boolean")) methodcall.append("bool");
                else {
                    var type = getType(node.get("value"));
                    if(type.equals("int")) methodcall.append("i32");
                    else if(type.equals("boolean")) methodcall.append("bool");
                    else methodcall.append(type);
                }
            } else if(node.getKind().equals("Length")) {
                this.indexArray += 1;
                methodcall.append(arrayVisit(node, node.getJmmChild(0).get("value")) + ".i32");
            }

            methodcall.append(").V;\n");
        } else {
            methodcall.append(", \"" + method + "\").V;\n");
        }
        this.code.append(methodcall);
    }

    public String operationVisit(JmmNode jmmNode, int index, String type, StringBuilder tmp) {
        var op = jmmNode.get("op");
        index+=1;

        var left = jmmNode.getJmmChild(0);
        if(left.hasAttribute("value")) {
            for(var localVar : this.localVariables) {
                if(localVar.getName().equals(left.get("value")))
                    if(localVar.getType().getName().equals("int")) this.operationBuilder.append(left.get("value") + ".i32");
            }
            if(left.getKind().equals("Integer")) this.operationBuilder.append(left.get("value") + ".i32");
        } else {
            operationVisit(left, index, type, tmp);
            this.operationBuilder.append(tmp + ".i32");
        }

        this.operationBuilder.append(" " + op + ".i32 ");

        var right = jmmNode.getJmmChild(1);
        if(right.hasAttribute("value")) {
            for(var localVar : this.localVariables) {
                if(localVar.getName().equals(right.get("value")))
                    if (localVar.getType().getName().equals("int")) this.operationBuilder.append(right.get("value") + ".i32");
            }
            if(right.getKind().equals("Integer")) this.operationBuilder.append(right.get("value") + ".i32");
        } else {
            operationVisit(right, index, type, tmp);
            this.operationBuilder.append(tmp + ".i32");
        }

        this.code.append("\t\tt" + index + ".i32 :=.i32 " + this.operationBuilder.toString() + ";\n");

        if(!tmp.isEmpty()) tmp.delete(0, tmp.length());

        tmp.append("t" + index);

        this.operationBuilder.delete(0, this.operationBuilder.length());

        return tmp.toString();
    }

    public void ifVisit(JmmNode jmmNode) {
        this.indexIf += 1;

        this.code.append("\t\tif (");

        var condition = jmmNode.getJmmChild(0);
        if(condition.getJmmChild(0).getKind().equals("RelationalOp")) {
            var op = condition.getJmmChild(0).get("op");
            var var1 = condition.getJmmChild(0).getJmmChild(0).get("value");
            var var2 = condition.getJmmChild(0).getJmmChild(1).get("value");

            this.code.append(var1 + ".");

            if(condition.getJmmChild(0).getJmmChild(0).getKind().equals("Integer"))
                this.code.append("i32");
            else {
                var var1_type = getType(var1);
                if(var1_type.equals("int")) this.code.append("i32 ");
                else if(var1_type.equals("boolean")) this.code.append("bool ");
                else this.code.append(var1_type);
            }

            this.code.append(op + ".bool ");

            this.code.append(var2 + ".");

            if(condition.getJmmChild(0).getJmmChild(1).getKind().equals("Integer"))
                this.code.append("i32");
            else {
                var var2_type = getType(var2);
                if(var2_type.equals("int")) this.code.append("i32 ");
                else if(var2_type.equals("boolean")) this.code.append("bool ");
                else this.code.append(var2_type);
            }
        } else if(condition.getJmmChild(0).getKind().equals("IdentifierExpr")) {
            var value = condition.getJmmChild(0).get("value");
            var type = getType(value);
            this.code.append(value + ".");
            if(type.equals("int")) this.code.append("i32");
            else if(type.equals("boolean")) this.code.append("bool");
            else this.code.append(type);
        }

        this.code.append(") goto ifbody_" + this.indexIf + ";\n");

        var else_stmt = jmmNode.getJmmChild(2);
        if(else_stmt.getJmmChild(0).getKind().equals("Statement")) statementVisit(else_stmt.getJmmChild(0));

        this.code.append("\t\tgoto endif_" + this.indexIf + ";\n");
        this.code.append("\t\tifbody_" + this.indexIf + ":\n");

        var if_stmt = jmmNode.getJmmChild(1);
        if(if_stmt.getJmmChild(0).getKind().equals("Statement")) statementVisit(if_stmt.getJmmChild(0));

        this.code.append("\t\tendif_" + this.indexIf + ":\n");
    }

    public void whileVisit(JmmNode jmmNode) {
        this.indexWhile += 1;

        this.code.append("\t\tif (");

        var condition = jmmNode.getJmmChild(0);

        if(condition.getJmmChild(0).getKind().equals("RelationalOp")) {
            var op = condition.getJmmChild(0).get("op");
            var var1 = condition.getJmmChild(0).getJmmChild(0).get("value");
            var var2 = condition.getJmmChild(0).getJmmChild(1).get("value");

            this.code.append(var1 + ".");

            if(condition.getJmmChild(0).getJmmChild(0).getKind().equals("Integer"))
                this.code.append("i32");
            else {
                var var1_type = getType(var1);
                if(var1_type.equals("int")) this.code.append("i32 ");
                else if(var1_type.equals("boolean")) this.code.append("bool ");
                else this.code.append(var1_type);
            }

            this.code.append(op + ".bool ");

            this.code.append(var2 + ".");

            if(condition.getJmmChild(0).getJmmChild(1).getKind().equals("Integer"))
                this.code.append("i32");
            else {
                var var2_type = getType(var2);
                if(var2_type.equals("int")) this.code.append("i32 ");
                else if(var2_type.equals("boolean")) this.code.append("bool ");
                else this.code.append(var2_type);
            }
        }

        this.code.append(") goto whilebody_" + this.indexWhile + ";\n");
        this.code.append("\t\tgoto endwhile_" + this.indexWhile + ";\n");

        this.code.append("\t\twhilebody_" + this.indexWhile + ":\n");

        var stmts_size = jmmNode.getJmmChild(1).getNumChildren();
        for(int i = 0; i < stmts_size; i++) {
            if(jmmNode.getJmmChild(1).getJmmChild(i).getKind().equals("Statement"))
                statementVisit(jmmNode.getJmmChild(1).getJmmChild(i));
        }
    }

    public String arrayVisit(JmmNode jmmNode, String dest) {
        if(jmmNode.getKind().equals("ArrayNew")) {

            var temp = "temp" + this.indexArray;
            String temp_type;

            if(jmmNode.getJmmChild(1).getKind().equals("Integer")) {
                this.code.append("\t\ttemp" + this.indexArray + ".i32 :=.i32 " + jmmNode.getJmmChild(1).get("value") + ".i32;\n");
                temp_type = "int";
            } else {
                var type = getType(jmmNode.getJmmChild(1).get("value"));
                temp_type = type;
                if(type.equals("int"))
                    this.code.append("\t\ttemp" + this.indexArray + ".i32 :=.i32 " + jmmNode.getJmmChild(1).get("value") + ".i32;\n");
                else if(type.equals("boolean"))
                    this.code.append("\t\ttemp" + this.indexArray + ".bool :=.bool " + jmmNode.getJmmChild(1).get("value") + ".bool;\n");
                else
                    this.code.append("\t\ttemp" + this.indexArray + "." + type + " :=." + type + " " + jmmNode.getJmmChild(1).get("value") + "." + type + ";\n");
            }

            var type = getType(dest);
            if(type.equals("int[]") || type.equals("int")) this.code.append("\t\t" + dest + ".array.i32 :=.array.i32 ");
            else if(type.equals("boolean[]") || type.equals("boolean")) this.code.append("\t\t" + dest + ".array.bool :=.array.bool ");
            else this.code.append("\t\t" + dest + ".array." + type + " :=.array." + type + " ");

            this.code.append("new(array, " + temp + ".");

            if(temp_type.equals("int")) this.code.append("i32");
            else if(temp_type.equals("boolean")) this.code.append("bool");
            else this.code.append(temp_type);

            this.code.append(").array");

            if(jmmNode.getJmmChild(0).get("t").equals("int"))
                this.code.append(".i32;\n");
            else if(jmmNode.getJmmChild(0).get("t").equals("boolean"))
                this.code.append(".bool;\n");
            else this.code.append("." + jmmNode.getJmmChild(0).get("t") + ";\n");

        } else if(jmmNode.getKind().equals("Length")) {
            var temp = "temp" + this.indexArray;

            this.code.append("\t\ttemp" + this.indexArray + ".i32 :=.i32 ");
            this.code.append("arraylength(" + dest + ".array");

            var type = getType(dest);
            if(type.equals("int") || type.equals("int[]")) this.code.append(".i32");
            else if(type.equals("boolean") || type.equals("boolean[]")) this.code.append(".bool");
            else this.code.append("." + type);

            this.code.append(").i32.i32;\n\n");

            return temp;
        }

        return null;
    }

    public String getReturnVar(String returnType) {
        //TODO: tests failing here
        var method = this.jmmSemanticsResult.getRootNode().getJmmChild(this.indexFirstLevel-1).getJmmChild(this.indexSecondLevel-1);
        StringBuilder returnVar = new StringBuilder();
        var returnValue = method.getJmmChild(method.getNumChildren()-1).getJmmChild(0).getJmmChild(0);

        if(returnValue.hasAttribute("value")) {
            returnVar.append(returnValue.get("value"));
            var type = getType(returnValue.get("value"));

            if(type.equals("int[]") || type.equals("boolean[]")) returnVar.append(".array");
            if(returnType.equals("int") || returnType.equals("int[]")) {
                returnVar.append(".i32");
            }
            if(returnType.equals("boolean") || returnType.equals("boolean[]")) {
                returnVar.append(".bool");
            }
        }
        if(returnValue.getKind().equals("AdditiveOp")) {
            var operation = operationVisit(returnValue, 0, returnType, new StringBuilder());
            returnVar.append("\t\tret.i32 " + operation + ".i32;\n");
        }
        return returnVar.toString();
    }
}
