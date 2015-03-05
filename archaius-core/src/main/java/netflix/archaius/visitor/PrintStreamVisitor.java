package netflix.archaius.visitor;

import java.io.PrintStream;

import netflix.archaius.Config;

public class PrintStreamVisitor implements Config.Visitor {
    private final PrintStream stream;
    
    public PrintStreamVisitor(PrintStream stream) {
        this.stream = stream;
    }
    
    public PrintStreamVisitor() {
        this(System.out);
    }
    
    @Override
    public void visit(Config config, String key) {
        stream.println(key + " = " + config.getString(key));
    }
}
