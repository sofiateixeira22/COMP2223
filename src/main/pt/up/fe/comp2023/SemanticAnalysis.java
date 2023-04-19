package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class SemanticAnalysis implements JmmAnalysis {

    List<Report> reports = new ArrayList<>();


    public void traverseTree(JmmNode jmmNode){

        if (Objects.equals(jmmNode.toString(), "AssignmentOp")){

        }
        else {
            for (JmmNode child : jmmNode.getChildren()){
                traverseTree(child);
            }
        }

    }

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {

        var table = new SymbolTable(jmmParserResult);

        //System.out.println(jmmParserResult.getRootNode().toTree());

        //System.out.println(table.print());

        traverseTree(jmmParserResult.getRootNode());

        return new JmmSemanticsResult(jmmParserResult, table, new ArrayList<>());
    }
}
