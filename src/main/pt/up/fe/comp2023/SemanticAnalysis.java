package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class SemanticAnalysis implements JmmAnalysis {

    List<Report> reports = new ArrayList<>();

    public boolean checkAssignment(String typeA, String typeB){
        if (!typeB.equals(typeA)){
            Report report = new Report(ReportType.ERROR ,Stage.SEMANTIC, -1, "Object of type " + typeB + " cannot be assigned to object of type " + typeA);
            this.reports.add(report);
            return false;
        }
        return true;
    }

    public boolean checkOperation(JmmNode jmmNode){
        return true;
    }

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {

        var table = new SymbolTable(jmmParserResult);



        return new JmmSemanticsResult(jmmParserResult, table, new ArrayList<>());
    }
}
