package br.edu.ifsc.javargtest;

import br.edu.ifsc.javargtest.JRGLog.Severity;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.printer.DotPrinter;
import com.github.javaparser.printer.PrettyPrinter;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.Test;

/**
 *
 * @author samuel
 *
 */
public class MainTests {

    private static final String SKELETON_PATH
            = "src/main/java/br/edu/ifsc/javarg/MainClass.java";

    private CompilationUnit mSkeleton;

    private ClassTable mCT;

    private JRGBase mBase;

    private JRGCore mCore;

    private JRGStmt mStmt;

    private JRGOperator mOperator;

    private Map<String, String> mCtx;

    public MainTests() throws FileNotFoundException, IOException {
        mSkeleton = StaticJavaParser.parse(new File(SKELETON_PATH));

        dumpAST();

        //JRGLog.logLevel = Severity.MSG_XDEBUG; ////////////////////////////////
    }

    @BeforeTry
    public void createObjects() {
        mCT = new ClassTable(loadImports());

        mBase = new JRGBase(mCT);

        mCore = new JRGCore(mCT, mBase);

        mStmt = new JRGStmt(mCT, mBase, mCore);

        mOperator = new JRGOperator(mCT, mBase, mCore);

        mCtx = new HashMap<String, String>();
    }

    // Auxiliary methods
    private List<String> loadImports() {
        NodeList<ImportDeclaration> imports = mSkeleton.getImports();

        List<String> list = new ArrayList<>();

        Iterator<ImportDeclaration> it = imports.iterator();
        while (it.hasNext()) {
            ImportDeclaration i = it.next();
            list.add(i.getName().asString());
        }

        return list;
    }

    /*
   * 
   * Write AST - Arbitrary Sintax Tree to file
   * using FileWriter output filename is `ast.dot`
   * 
     */
    private void dumpAST() throws IOException {
        DotPrinter printer = new DotPrinter(true);

        try (
                 FileWriter fileWriter = new FileWriter("ast.dot");  PrintWriter printWriter = new PrintWriter(fileWriter)) {
            printWriter.print(printer.output(mSkeleton));
        }
    }

    private void imprimiDados(CompilationUnit Classe) throws IOException {
        //DotPrinter printer = new DotPrinter(true);

        PrettyPrinter printer = new PrettyPrinter();

        try (
                 FileWriter arq = new FileWriter("MainClass.java");  PrintWriter gravarArq = new PrintWriter(arq)) {
            //gravarArq.print(mSkeleton.toString());
            //gravarArq.print(mSkeleton.toString());
            gravarArq.print(printer.print(Classe));
        }
    }

    public void compila(String arquivo2) throws IOException {
        PrintWriter saida = new PrintWriter(new FileWriter("logCompilacao.txt"));

        int resultadoCompilacao = com.sun.tools.javac.Main.compile(
                new String[]{arquivo2},
                saida
        );
    }

    /**
     * ********************************
     *                                *
     * Tests start here * * ********************************
     */

    /*
   *
   * Generate a random primitive type all available primitive
   * types can be found at JRGBase.java `primitiveTypes()` which
   * then use 'net.jqwik.api.Arbitraries' to fetch all possible types
   *
     */
    // @Example
    boolean checkGenPrimitiveType() {
        Arbitrary<PrimitiveType.Primitive> t = mBase.primitiveTypes();

        Arbitrary<LiteralExpr> e = t.flatMap(
                tp -> mBase.genPrimitiveType(new PrimitiveType(tp))
        );

        System.out.println(
                "Expressão gerada (tipo primitivo): " + e.sample().toString()
        );

        return true;
    }

    /*
   *
   * Generate a random String literal with min_length = 1
   * and max_length = 5, ranging chars from 'a' to 'z'
   *
     */
    // @Example
    boolean checkGenPrimitiveString() {
        Arbitrary<LiteralExpr> s = mBase.genPrimitiveString();

        System.out.println("Frase gerada: " + s.sample());

        return true;
    }

 

    /*
   *
   * Generate a random Method from `JRGCore.java` ClassTable
   *
     */
    //@Property(tries = 10)
   // @Example
    boolean checkGenMethodInvokation() throws ClassNotFoundException {
        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenMethodInvokation" + "::inicio"
        );

        ClassOrInterfaceType c = new ClassOrInterfaceType();
        c.setName("br.edu.ifsc.javargexamples.A");

        //Arbitrary<MethodCallExpr> e = mCore.genMethodInvokation(c);
        
        Arbitrary<MethodCallExpr> e = mCore.genMethodInvokation(
                mCtx,
                ReflectParserTranslator.reflectToParserType("int")
        );

        if (e != null) {
            System.out.println("Method gerado: " + e.sample().toString());
        } else {
            JRGLog.showMessage(
                    Severity.MSG_ERROR,
                    "Não foi possível gerar " + "criação do método"
            );
        }

        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenMethodInvokation" + "::fim"
        );

        return true;
    }

    //@Example
    @Property(tries = 10)

    boolean checkGenLambdaInvokation() throws ClassNotFoundException {
        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenLambdaInvokation" + "::inicio"
        );

        ClassOrInterfaceType c = new ClassOrInterfaceType();
        c.setName("java.util.function.DoubleToIntFunction");//interface
        if (mCT.isFunctionalInterface(c.asString())) {
           // System.out.println("antes do genLambdaInvokation");

            Arbitrary<MethodCallExpr> e = mCore.genLambdaInvokation(
                    mCtx,
                    ReflectParserTranslator.reflectToParserType("int") // int????????
            );
           // System.out.println("depois do genLambdaInvokation");

            if (e != null) {
                System.out.println("\n \n \nLAMBDA GERADO:: " + e.sample().toString());
            } else {
                JRGLog.showMessage(
                        Severity.MSG_ERROR,
                        "Não foi possível gerar " + "criação do lambda"
                );
            }
        }
        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenLambdaInvokation" + "::fim"
        );

        return true;
    }

    
        /*
   *
   * Get all superTypes from subsequent class calls
   * from ClassTable given a Class path as a parameter
   *
   * In fact, should be called `checkSuperTypes()`
   *
     */
    // @Example
    boolean checkSubTypes2() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkSubTypes" + "::inicio");

        List<Class> b = mCT.subTypes2("br.edu.ifsc.javargexamples.B");
        //List<Class> b = mCT.subTypes2("java.util.function.DoubleToIntFunction");
        b.forEach(
                i -> {
                    System.out.println("subTypes: " + i.toString());
                }
        );

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkSubTypes" + "::final");

        return true;
    }
    
    
    /*
   *
   * Picks a random Method from a list of avaiable methods
   * from `JRGCore.java` using the given type "int" as a parameter
   *
     */
    //@Property(tries=10)
    //@Example
    boolean checkGenCandidatesMethods() throws ClassNotFoundException {
        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenCandidatesMethods" + "::inicio"
        );

        Arbitrary<Method> b = mCore.genCandidatesMethods("int");

        System.out.println("Candidatos Methods: " + b.sample());

        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenCandidatesMethods" + "::fim"
        );

        return true;
    }

    //@Example
    //@Property(tries = 10)
    boolean checkGenConcreteCandidatesMethods() throws ClassNotFoundException {
        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenConcreteCandidatesMethods" + "::inicio"
        );

        Arbitrary<Method> b = mCore.genConcreteCandidatesMethods("int");

        System.out.println("Concrete Candidates Methods: " + b.sample());

        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenConcreteCandidatesMethods" + "::fim"
        );

        return true;
    }

    //@Property(tries = 10) //SÓ EXISTE UM MÉTODO LAMBDA DISPONÍVEL NO MOMENTO... pq?
    //@Example
    boolean checkGenLambdaCandidateMethods() throws ClassNotFoundException {
        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenLambdaCandidateMethods" + "::inicio"
        );

        Arbitrary<Method> b = mCore.genLambdaCandidatesMethods("int");

        System.out.println("Candidatos métodos Lambda: " + b.sample());

        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenLambdaCandidateMethods:" + ":fim"
        );

        return true;
    }

    /*
   * 
   * Generate Lambda expressions from `JRGCore.java`
   * 
     */
     //@Example
    // @Property(tries = 10)
    boolean checkGenLambdaExpr() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenLambdaExpr::inicio");

        ClassOrInterfaceType fi = new ClassOrInterfaceType();

        fi.setName("java.util.function.DoubleToIntFunction");

        Arbitrary<LambdaExpr> e = mCore.genLambdaExpr(mCtx, fi);

        System.out.println("checkGenLambdaExpr: Expr gerada: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenLambdaExpr::fim");

        return true;
    }

    // @Example
    boolean checkIsFunctionalInterface() throws ClassNotFoundException {
        if (mCT.isFunctionalInterface("java.util.function.DoubleToIntFunction")) {
            System.out.println("It is a functional interface");
        } else {
            System.out.println("It is not a functional interface");
        }

        return true;
    }
   /*
   *
   * Generate a new Class or Interface type from `JRGCore.java`
   *
     */
   //  @Example
    boolean checkGenObjectCreation() throws ClassNotFoundException {
        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenObjectCreation" + "::inicio"
        );

        ClassOrInterfaceType c = new ClassOrInterfaceType();

        c.setName("br.edu.ifsc.javargexamples.A");// mudei de B para A!!


        //Arbitrary<ObjectCreationExpr> e = mCore.genObjectCreation(c);
        Arbitrary<Expression> e = mCore.genObjectCreation(mCtx, c);

        if (e != null) {
            System.out.println("ObjectCreation gerado: " + e.sample().toString());
        } else {
            JRGLog.showMessage(
                    Severity.MSG_ERROR,
                    "Não foi possível gerar " + "criação de objeto"
            );
        }

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenObjectCreation::fim");

        return true;
    }
    /*
   *
   * Picks a random Field/Attribute from a list of avaiable fields/attributes
   * from `JRGCore.java` using the given type "int" as a parameter
   *
     */
    // @Example
    //@Property(tries = 10)
    boolean checkGenCandidatesFields() throws ClassNotFoundException {
        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenCandidatesFields" + "::inicio"
        );

        Arbitrary<Field> b = mCore.genCandidatesField("int");

        System.out.println("Candidatos Fields: " + b.sample());

        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenCandidatesFields:" + ":fim"
        );

        return true;
    }

    /*
   *
   * Picks a random Constructor from a list of avaiable constructors
   * from `JRGCore.java` using the given type class as a parameter
   *
     */
    // @Example
    boolean checkGenCandidatesConstructors() throws ClassNotFoundException {
        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenCandidatesConstructors" + "::inicio"
        );

        Arbitrary<Constructor> b = mCore.genCandidatesConstructors(
                "br.edu." + "ifsc.javargexamples.B"
        );

        System.out.println("Candidatos Constructors: " + b.sample());

        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenCandidatesConstructors" + "::fim"
        );

        return true;
    }

    /*
   *
   * Generate a selection of random expressions using attributes and literal integers
   *
     */
    //@Property(tries = 1)
    boolean checkGenExpression() {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenExpression::inicio");

        try {
            Arbitrary<Expression> e = mCore.genExpression(
                    mCtx,
                    ReflectParserTranslator.reflectToParserType("int")
            );
            System.out.println("Expressão gerada: " + e.sample());
        } catch (Exception ex) {
            System.out.println("Erro: " + ex.getMessage());
            return false;
        }

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenExpression::fim");

        return true;
    }

    /*
   *
   * Generate a statement for accessing an attribute of type 'int'
   * from `JRGCore.java` using 'tname: int' as parameter
   *
     */
    // @Example
    boolean checkGenAttributeAccess() throws ClassNotFoundException {
        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenAtributteAcess" + "::inicio"
        );

        Arbitrary<FieldAccessExpr> e = mCore.genAttributeAccess(
                mCtx,
                ReflectParserTranslator.reflectToParserType("int")
        );

        System.out.println("Acesso gerado: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenExpression::fim");

        return true;
    }

    /*
   *
   * Generate a Cast expression for convertion
   * from `JRGCore.java` using a Class as parameter
   *
     */
    // @Example
    boolean checkGenUpCast() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenUpCast" + "::inicio");

        Arbitrary<CastExpr> e = mCore.genUpCast(
                mCtx,
                ReflectParserTranslator.reflectToParserType(
                        "br.edu.ifsc." + "javargexamples.Aextend"
                )
        );

        System.out.println("CheckGenUpCast: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenUpCast" + "::final");

        return true;
    }

    /*
   *
   * !ERROR "Jwqik empty set of values"
   *
     */
    // @Example
    boolean checkGenVar() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenVar" + "::inicio");

        Arbitrary<NameExpr> e = mCore.genVar(
                mCtx,
                ReflectParserTranslator.reflectToParserType("int")
        );

        System.out.println("checkGenVar: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenVar" + "::final");
        return true;
    }

    /*
   *
   * Get all super() from subsequents class inheritance calls
   * from ClassTable given a Class path as a parameter
   *
     */
     //@Example
    boolean checkSuperTypes() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkSuperTypes" + "::inicio");

        List<Class> b = mCT.superTypes(
                "br.edu.ifsc." + "javargexamples.AextendExtend"
        );

        b.forEach(
                i -> {
                    System.out.println("SuperTypes: " + i);
                }
        );

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkSuperTypes" + "::final");

        return true;
    }

    /*
   *
   * Get the subTypes from a given class object
   * from ClassTable given a Class path as a parameter
   *
     */
    // @Example
    //@Property(tries = 5)

    boolean checkSubTypes() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkSubTypes" + "::inicio");

        List<Class> b = mCT.subTypes("br.edu.ifsc." + "javargexamples.A");

        b.forEach(
                i -> {
                    System.out.println("subTypes: " + i.toString());
                }
        );

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkSubTypes" + "::final");

        return true;
    }



    /*
   *
   * Get the candidates for up-casting
   * from `JRGCore.java` using the class object as a parameter
   *
     */
    // @Example
    boolean checkGenCandidateUpCast() throws ClassNotFoundException {
        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenCandidateUpCast" + "::inicio"
        );

        Arbitrary<Class> b = mCore.genCandidateUpCast(
                "br.edu.ifsc." + "javargexamples.A"
        );

        System.out.println("Candidatos UpCast: " + b.sample().getName());

        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenCandidateUpCast" + "::final"
        );

        return true;
    }

    /*
   *
   * Generate a BlockStmt containing a random program
   * from `JRGStmt.java` using the imports from `MainClass.java`
   * the code is generated from a list variables up to conditional statements
   *
   * # A further improvement would be to write this BlockStmt to a file instead
   * of writing to console everytime the test is ran
   *
     */
    //@Example
    //@Property(tries=3)
    boolean checkGenBlockStmt() throws ClassNotFoundException, IOException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenBlockStmt::inicio");

        Arbitrary<BlockStmt> e = mStmt.genBlockStmt(mCtx);

        System.out.println("BlockStmt: " + e.sample());

        ClassOrInterfaceDeclaration classe = mSkeleton
                .getClassByName("MainClass")
                .get();

        List<MethodDeclaration> ms = classe.getMethods();

        MethodDeclaration m = ms.get(0);

        m.setBody(e.sample());

        imprimiDados(mSkeleton);

        compila("MainClass.java");

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenBlockStmt::fim");

        return true;
    }

    /*
   *
   * Generate a variety of variable declarations and assignments
   * using arbitrary data types and a valid string for the variable
   * label from `JRGStmt.java`
   *
     */
    // @Property(tries = 100)
    boolean checkGenVarDeclAssign() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenVarDeclaration::inicio");

        Arbitrary<VariableDeclarationExpr> e = mStmt.genVarDeclAssign(mCtx);

        System.out.println("checkGengenVarDeclaration: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenVarDeclaration::fim");

        return true;
    }

    /*
   *
   * Generate a variety of ONLY variable declarations using arbitrary
   * data types and a valid string for the variable label from `JRGStmt.java`
   *
     */
    // @Property(tries = 100)
    boolean checkGenVarDecl() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenVarDeclaration::inicio");

        Arbitrary<VariableDeclarationExpr> e = mStmt.genVarDecl(mCtx);

        System.out.println("checkGengenVarDeclaration: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenVarDeclaration::fim");

        return true;
    }

    /*
   *
   * Generate If and Else statements from `JRGStmt.java`
   *
     */
    // @Example
    boolean checkGenIfStmt() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenIfStmt::inicio");

        Arbitrary<IfStmt> e = mStmt.genIfStmt(mCtx);

        System.out.println("checkGenIfStmt: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenIfStmt::fim");

        return true;
    }

    /*
   *
   * !ERROR - It's using a binaryExpr and looping conditional for some reason
   * Idk if it is supposed to be like this
   *
     */
    // @Example
    boolean checkWhileStmt() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkWhileStmt::inicio");

        Arbitrary<WhileStmt> e = mStmt.genWhileStmt(mCtx);

        System.out.println("checkWhileStmt: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkWhileStmt::fim");

        return true;
    }

    /*
   *
   * Generate conditional statements and a MainClass as well as functions
   * inside it with statements within itself from `JRGStmt.java`
   *
     */
    // @Example
    boolean checkGenStatement() throws ClassNotFoundException, IOException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenStatement::inicio");

        Arbitrary<Statement> e = mStmt.genStatement(mCtx);

        System.out.println("checkGenStatement: " + e.sample());

        System.out.println(mSkeleton.getClassByName("MainClass"));

        ClassOrInterfaceDeclaration classe = mSkeleton
                .getClassByName("MainClass")
                .get();

        classe.addMethod(
                "main",
                Modifier.publicModifier().getKeyword(),
                Modifier.Keyword.STATIC
        );
        //mSkeleton.addInterface(e.sample().toString());

        classe.addInitializer().addAndGetStatement(e.sample());

        //imprimiDados(mSkeleton.addClass(classe.toString()));
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenStatement::fim");

        return true;
    }

    /*
   *
   * Generate a Logical statement from `JRGStmt.java`
   *
     */
    // @Example
    boolean checkGenExpressionStmt() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenExpressionStmt::inicio");

        Arbitrary<ExpressionStmt> e = mStmt.genExpressionStmt(mCtx);

        System.out.println("checkGenExpressionStmt: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenExpressionStmt::fim");

        return true;
    }

    /*
   *
   * Generate a Logical Expressions from `JRGOperator.java`
   *
     */
    //@Example
    // @Property(tries = 10)
    boolean checkGenLogiExpression() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenLogiExpression::inicio");

        Arbitrary<BinaryExpr> e = mOperator.genLogiExpression(mCtx);

        System.out.println("checkGenLogiExpression: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenLogiExpression::fim");

        return true;
    }

    /*
   *
   * Generate a Relational Expressions from `JRGOperator.java`
   * Using comparision signs as <, ==, >= for example
   *
     */
    // @Example
    // @Property(tries = 10)
    boolean checkGenRelaExpression() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenRelaExpression::inicio");

        Arbitrary<BinaryExpr> e = mOperator.genRelaExpression(mCtx);

        System.out.println("checkGenRelaExpression: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenRelaExpression::fim");

        return true;
    }

    /*
   *
   * Generate a Arithmetic Expressions from `JRGOperator.java`
   * Using %, ==, +, -, * between two or more statements for example
   *
     */
    // @Example
    // @Property(tries = 10)
    boolean checkGenArithExpression() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenArithExpression::inicio");

        Arbitrary<BinaryExpr> e = mOperator.genArithExpression(
                mCtx,
                ReflectParserTranslator.reflectToParserType("int")
        );

        System.out.println("checkGenArithExpression: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenArithExpression::fim");

        return true;
    }

    /*
   * 
   * Generate statements in a array format from `JRGStmt.java`
   * 
     */
    //@Example
    boolean checkGenStatementList() {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenStatementList::inicio");

        Arbitrary<NodeList<Statement>> e = mStmt.genStatementList(mCtx);

        System.out.println("checkGenStatementList: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenStatementList::fim");

        return true;
    }

    /*
   * 
   * Generate statements for variable declaration
   * From `JRGStmt.java`
   * 
     */
    // @Property(tries = 10)
    boolean checkGenVarDeclarationStmt() throws ClassNotFoundException {
        JRGLog.showMessage(
                Severity.MSG_XDEBUG,
                "checkGenVarDeclarationStmt::inicio"
        );

        Arbitrary<ExpressionStmt> e = mStmt.genVarDeclarationStmt(mCtx);

        System.out.println("checkGenVarDeclarationStmt: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenVarDeclarationStmt::fim");

        return true;
    }

    /*
   * 
   * !ERROR - empty set of values
   * 
     */
    // @Example
    boolean checkGenVarAssingStmt() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenVarAssingStmt::inicio");

        Arbitrary<VariableDeclarationExpr> e = mStmt.genVarAssingStmt(mCtx);

        System.out.println("checkGenVarAssingStmt: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenVarAssingStmt::fim");

        return true;
    }

    /*
   * 
   * !ERROR - empty set of values
   * 
     */
    // @Example
    boolean checkGenTypeAssingStmt() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenTypeAssingStmt::inicio");

        Arbitrary<AssignExpr> e = mStmt.genTypeAssingStmt(mCtx);

        System.out.println("checkGenTypeAssingStmt: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenTypeAssingStmt::fim");

        return true;
    }

    /*
   * 
   * Generate For Loopings expressions with statements within
   * the loop using `JRGStmt.java`
   * 
     */
    // @Example
    // @Property(tries=4)
    boolean checkGenFor() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenTypeAssingStmt::inicio");

        Arbitrary<ForStmt> e = mStmt.genForStmt(mCtx);
        //mStmt.genForStmt(mCtx);
        System.out.println("checkGenTypeAssingStmt: " + e.sample());

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenTypeAssingStmt::fim");

        return true;
    }

    /*
   * 
   * !IDK = Generate a selection of variable declarations and assignments
   * 
     */
    // @Example
    boolean checkGenList() throws ClassNotFoundException {
        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenTypeAssingStmt::inicio");

        List<Statement> e = mStmt.genList(mCtx);
        //mStmt.genForStmt(mCtx);
        System.out.println("checkGenTypeAssingStmt: " + e.get(0));

        JRGLog.showMessage(Severity.MSG_XDEBUG, "checkGenTypeAssingStmt::fim");

        return true;
    }
}
