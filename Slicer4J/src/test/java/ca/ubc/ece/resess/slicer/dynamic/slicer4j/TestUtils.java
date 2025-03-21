package ca.ubc.ece.resess.slicer.dynamic.slicer4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import ca.ubc.ece.resess.slicer.dynamic.core.graph.DynamicControlFlowGraph;
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.DynamicSlice;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementInstance;



public class TestUtils {
    static Path root = Paths.get(".").normalize().toAbsolutePath();
    static Path junitRunnerPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "SingleJUnitTestRunner.jar");
    static Path junitJar = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "junit-4.8.2.jar");
    protected static Slicer setupSlicing(Path root, String jarPath, Path outDir, Path sliceLogger) {
        Slicer slicer = new Slicer();
        slicer.setPathJar(jarPath);
        slicer.setOutDir(outDir.toString());
        slicer.setLoggerJar(sliceLogger.toString());
        
        slicer.setFileToParse(outDir + File.separator + "trace.log");
        slicer.setStubDroidPath(root.getParent().toString() + File.separator + "models" + File.separator + "summariesManual");
        slicer.setTaintWrapperPath(root.getParent().toString() + File.separator + "models" + File.separator + "EasyTaintWrapperSource.txt");
        return slicer;
    }
    
    protected static Map<StatementInstance, String> sliceAndGetDirectDepdendeincesMap(Slicer slicer, DynamicControlFlowGraph dcfg, Integer tracePositionToSliceFrom) {
        StatementInstance stmt = dcfg.mapNoUnits(tracePositionToSliceFrom);
        slicer.setDebug(true);
        DynamicSlice dynamicSlice = slicer.directStatementDependency(stmt, true, false);
        Map<StatementInstance, String> sliceDeps = dynamicSlice.getSliceDependenciesAsMap();
        System.out.println(sliceDeps);
        return sliceDeps;
    }

    protected static Set<String> sliceAndGetSourceLines(Slicer slicer, DynamicControlFlowGraph dcfg, Integer tracePositionToSliceFrom) {
        StatementInstance stmt = dcfg.mapNoUnits(tracePositionToSliceFrom);
        DynamicSlice dynamicSlice = slicer.slice(dcfg, true, false, false, false, stmt, new HashSet<>(), slicer.getWorkingSet());
        Set<String> sliceLines = dynamicSlice.getSliceAsSourceLineNumbers();
        System.out.println(sliceLines);
        return sliceLines;
    }

    protected static Set<String> sliceAndGetSourceLines(Slicer slicer, DynamicControlFlowGraph dcfg, List<Integer> tracePositions) {
        List<StatementInstance> stmts = dcfg.mapNoUnits(tracePositions);
        DynamicSlice dynamicSlice = slicer.slice(dcfg, true, false, false, false, stmts, new HashSet<>(), slicer.getWorkingSet());
        Set<String> sliceLines = dynamicSlice.getSliceAsSourceLineNumbers();
        System.out.println(sliceLines);
        return sliceLines;
    }

    public static void runInstrumentedJarFromTest(String pathToJar, String dependencyPath, String testClass, String testMethod, String outDir) throws IOException, InterruptedException {
        String cmd = "java -Xmx8g -cp \"" + junitRunnerPath + ":" + junitJar + ":" + pathToJar + ":" + dependencyPath + "/*\" SingleJUnitTestRunner " +
                testClass + "#" + testMethod;
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", cmd + "| grep \"SLICING\"");
        Process p = pb.start();
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(outDir + File.separator + "trace.log"));
        String readline;
        while ((readline = reader.readLine()) != null) {
            writer.write(readline);
            writer.write("\n");
        }
        writer.close();
        reader.close();
    }

    protected static void buildJar(Path testPath) throws IOException, InterruptedException {
        Process p = null;
        ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "package");
        pb.directory(testPath.toFile());
        p = pb.start();
        p.waitFor();
        System.out.println("Out stream: ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String readline;
        while ((readline = reader.readLine()) != null) {
            System.out.println(readline);
        }
        reader.close();
        System.out.println("Error stream: ");
        reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        while ((readline = reader.readLine()) != null) {
            System.out.println(readline);
        }
        reader.close();
    }
    
    protected static void cleanWorkingDirectory() throws IOException {
        Path root = Paths.get(".").normalize().toAbsolutePath();
        Path workingDirPath = Paths.get(root.getParent().toString(), "scripts" + File.separator + "testTempDir");
        if (workingDirPath.toFile().exists()) {
            Files.walk(workingDirPath)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }
    }

    static List<Integer> getTracePositionFromSourceLine(int sourceLineNo, String javaSourceFile, DynamicControlFlowGraph dcfg) {
        List<Integer> ret = new ArrayList<>();
        for( StatementInstance stmt : dcfg.getTraceList() ){
            if( stmt.getJavaSourceLineNo() == sourceLineNo && stmt.getJavaSourceFile().equals( javaSourceFile ) ){
                ret.add( stmt.getLineNo() );
            }
        }
        return ret;
    }
}
