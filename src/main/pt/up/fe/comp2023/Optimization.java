package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class Optimization implements JmmOptimization {
    private StringBuilder code;
    private SymbolTable symbolTable;
    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        this.code = new StringBuilder();
        this.symbolTable = jmmSemanticsResult.getSymbolTable();

        importVisit();
        classDeclarationVisit();

        System.out.println("\n** Code **\n\n" + this.code);

//        System.out.println("again " + symbolTable.getSuper());
//        System.out.println("hello");
//        System.out.println(jmmSemanticsResult.getRootNode()); //Gets Program
//        System.out.println(jmmSemanticsResult.getRootNode().toTree()); //Gets the tree
//        System.out.println(jmmSemanticsResult.getReports());
//        System.out.println(jmmSemanticsResult.getSymbolTable().print());
        return null;
    }

    public void importVisit() {
        System.out.println("** Import Visit **");

        for (var importString: this.symbolTable.getImports()) {
            this.code.append("import " + importString + ";\n");
        }
    }

    public void classDeclarationVisit() {
        System.out.println("** Class Declaration Visit **");

        this.code.append(this.symbolTable.getClassName() + " ");

        var superClass = this.symbolTable.getSuper();
        if (superClass != null) {
            this.code.append("extends " + superClass + " ");
        }
        this.code.append("{\n");

        varDeclarationVisit(); //global variables

        this.code.append(".construct " + this.symbolTable.getClassName() + "().V {\n");
        this.code.append("invokespecial(this, \"<init>\").V;\n");
        this.code.append("}\n");

        methodVisit();

        this.code.append("\n}");
    }

    public void varDeclarationVisit() {
        System.out.println("** Var Declaration Visit **");

        for (var fieldString: this.symbolTable.getFields()) {
            this.code.append(".field public " + fieldString.getName());

            if (fieldString.getType().getName().equals("int"))
                this.code.append(".i32");
            else if (fieldString.getType().getName().equals("boolean"))
                this.code.append(".bool");
            else this.code.append(".V");

            this.code.append(";\n");
        }
    }

    public void methodVisit() {
        System.out.println("** Method Visit **");

        for (var methodString: this.symbolTable.getMethods()) {
            if(methodString.equals("main"))
                this.code.append(".method public static " + methodString + "(");
            else
                this.code.append(".method public " + methodString + "(");

            methodParametersVisit(methodString);

            this.code.append(")");

            if(this.symbolTable.getReturnType(methodString).getName().equals("int"))
                this.code.append(".i32 ");
            else if (this.symbolTable.getReturnType(methodString).getName().equals("boolean"))
                this.code.append(".bool ");
            else this.code.append(".V ");

            this.code.append("{\n");

            //TODO: things inside method
            System.out.println(this.symbolTable.getFields());

            this.code.append("}\n");
        }
    }

    public void methodParametersVisit(String methodString) {
        System.out.println("** Method Parameters Visit **");

        for (var parameterString: this.symbolTable.getParameters(methodString)) {
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
}
