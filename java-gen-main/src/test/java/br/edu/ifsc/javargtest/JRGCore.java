package br.edu.ifsc.javargtest;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Provide;

/**
 *
 * @author lukra
 *
 */
public class JRGCore {

    private static final int FUEL_START = 10;

    private ClassTable mCT;

    private JRGBase mBase;

    private JRGOperator mOperator;

    //private Map<String, String> mCtx;
    private List<String> mValidNames;

    int mFuel;

    public JRGCore(ClassTable ct, JRGBase base) {
        mCT = ct;

        mBase = base;

        mOperator = new JRGOperator(mCT, mBase, this);

        //mCtx = new HashMap<String, String>();
        mValidNames = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l");

        mFuel = FUEL_START;
    }

  /*  @Provide
    public Arbitrary<Expression> genExpressionLambda(Map<String, String> ctx, Type t) {
        Arbitrary<Expression> e;
        List<Arbitrary<Expression>> cand = new ArrayList<>();
           try {
           // if (!t.isPrimitiveType()) {
               // System.out.println("subtype: " + t); //function
                    
              //  t = ReflectParserTranslator.reflectToParserType("int");//function
                
                t = ReflectParserTranslator.reflectToParserType(
                        Arbitraries.of(mCT.subTypes2(t.toString())).sample().toString());//function*/
                
                // o subtype recebe uma classe, e retorna classes. t é tipo exemplo int
                
                
                
              //  System.out.println("t:"+t);
           // }
     /*   } catch (ClassNotFoundException ex) {
            System.out.println("Erro obtendo subtipo!");
        }
         
        try {
            if (mFuel > 0) { // Permite a recursão até certo ponto
                mFuel--;

                if (t.toString().equals(PrimitiveType.booleanType().asString())) {
                   // System.out.println("0");
                    cand.add(
                            Arbitraries.oneOf(
                                    mOperator.genLogiExpression(ctx),
                                    mOperator.genRelaExpression(ctx)
                            )
                    );
                }

                // Candidatos de tipos primitivos
                if (t.isPrimitiveType()) {
                    cand.add(Arbitraries.oneOf(mBase.genPrimitiveType(t.asPrimitiveType())));
                }

                if (t.isPrimitiveType() && mBase.isNumericType(t)) {
                    cand.add(Arbitraries.oneOf(mOperator.genArithExpression(ctx, t)));
               //     System.out.println("2");
                }

                // Verifica se existem atributos candidatos
                if (!mCT.getCandidateFields(t.asString()).isEmpty()) {
               //     System.out.println("3");
                    cand.add(Arbitraries.oneOf(genAttributeAccess(ctx, t)));
                    

                }

                //Verifica se existem candidados methods
                if (!mCT.getCandidateMethods(t.asString()).isEmpty()) {
                    cand.add(Arbitraries.oneOf(genMethodInvokation(ctx, t)));
                    //essencial
                }
            }
        } catch (ClassNotFoundException ex1) {
            throw new RuntimeException("Error: class not found!");
        }
        // System.out.println("genExpression::end");
        return Arbitraries.oneOf(cand);
    }
*/
    @Provide
    public Arbitrary<Expression> genExpression(Map<String, String> ctx, Type t) {
        Arbitrary<Expression> e;
        List<Arbitrary<Expression>> cand = new ArrayList<>();

        try {
            if (mFuel > 0) { // Permite a recursão até certo ponto
                mFuel--;

                if (t.toString().equals(PrimitiveType.booleanType().asString())) {
                    cand.add(
                            Arbitraries.oneOf(
                                    mOperator.genLogiExpression(ctx),
                                    mOperator.genRelaExpression(ctx)
                            )
                    );
                }

                // Candidatos de tipos primitivos
                if (t.isPrimitiveType()) {
                    cand.add(
                            Arbitraries.oneOf(mBase.genPrimitiveType(t.asPrimitiveType()))
                    );
                }

                if (t.isPrimitiveType() && mBase.isNumericType(t)) {
                    cand.add(Arbitraries.oneOf(mOperator.genArithExpression(ctx, t)));
                }

                // Se não for tipo primitivo
                if (!t.isPrimitiveType()) {
                    //Candidatos de construtores
                    cand.add(Arbitraries.oneOf(genObjectCreation(ctx, t)));
                }

                // Verifica se existem atributos candidatos
                if (!mCT.getCandidateFields(t.asString()).isEmpty()) {
                    cand.add(Arbitraries.oneOf(genAttributeAccess(ctx, t)));
                }

                //Verifica se existem candidados methods
                if (!mCT.getCandidateMethods(t.asString()).isEmpty()) {
                    cand.add(Arbitraries.oneOf(genMethodInvokation(ctx, t)));
                }

                // Verifica se existem candidados cast
                if (!t.isPrimitiveType() && !mCT.subTypes2(t.asString()).isEmpty()) {
                    cand.add(Arbitraries.oneOf(genUpCast(ctx, t)));
                }

                // Verifica se existem candidados Var
                if (ctx.containsValue(t.asString())) {
                    cand.add(Arbitraries.oneOf(genVar(ctx, t)));
                }
            } else { // Não permite aprofundar a recursão
                if (t.isPrimitiveType()) {
                    cand.add(
                            Arbitraries.oneOf(mBase.genPrimitiveType(t.asPrimitiveType()))
                    );
                }

                if (!t.isPrimitiveType()) {
                    cand.add(Arbitraries.oneOf(genObjectCreation(ctx, t)));
                }

                if (ctx.containsValue(t.asString())) {
                    cand.add(Arbitraries.oneOf(genVar(ctx, t)));
                }
                //if (t.toString().equals(PrimitiveType.booleanType().asString())) {
                //    cand.add(Arbitraries.oneOf(mOperator.genLogiExpression(ctx),
                //    mOperator.genRelaExpression(ctx)));
                //}

                //if (t.isPrimitiveType() && mBase.isNumericType(t)){
                //   cand.add(Arbitraries.oneOf(mOperator.genArithExpression(ctx,t)));
                //}
            }
        } catch (ClassNotFoundException ex1) {
            throw new RuntimeException("Error: class not found!");
        }

        return Arbitraries.oneOf(cand);
    }

    @Provide
    public Arbitrary<NodeList<Expression>> genExpressionList(
            Map<String, String> ctx,
            List<Type> types
    ) {
        JRGLog.showMessage(JRGLog.Severity.MSG_XDEBUG, "genExpressionList::inicio");

        JRGLog.showMessage(
                JRGLog.Severity.MSG_XDEBUG,
                "genExpressionList::types" + types.toString()
        );
        List<Expression> exs = types
                .stream()
                .map(t -> genExpression(ctx, t))
                .map(e -> e.sample())
                .collect(Collectors.toList());

        NodeList<Expression> nodes = new NodeList<>(exs);

        JRGLog.showMessage(JRGLog.Severity.MSG_XDEBUG, "genExpressionList::fim");

        return Arbitraries.just(nodes);
    }

    @Provide
    //public Arbitrary<ObjectCreationExpr> genObjectCreation(Type t) throws ClassNotFoundException {
    public Arbitrary<Expression> genObjectCreation(
            Map<String, String> ctx,
            Type t
    )
            throws ClassNotFoundException {
        JRGLog.showMessage(JRGLog.Severity.MSG_XDEBUG, "genObjectCreation::inicio");

        List<Constructor> constrs;
        //System.out.println("----"+t);
        constrs = mCT.getClassConstructors(t.asString());
        Arbitrary<Constructor> c = Arbitraries.of(constrs);

        Constructor constr = c.sample();

        JRGLog.showMessage(
                JRGLog.Severity.MSG_DEBUG,
                "genObjectCreation::constr : " + constr.toString()
        );

        Class[] params = constr.getParameterTypes();

        List<Class> ps = Arrays.asList(params);

        JRGLog.showMessage(
                JRGLog.Severity.MSG_DEBUG,
                "genObjectCreation::ps " + ps
        );

        List<Type> types = ps
                .stream()
                .map(
                        tname -> ReflectParserTranslator.reflectToParserType(tname.getName())
                )
                .collect(Collectors.toList());

        JRGLog.showMessage(
                JRGLog.Severity.MSG_DEBUG,
                "genObjectCreation::types " + "[" + types + "]"
        );

        JRGLog.showMessage(JRGLog.Severity.MSG_XDEBUG, "genObjectCreation::fim");

        return genExpressionList(ctx, types)
                .map(el -> new ObjectCreationExpr(null, t.asClassOrInterfaceType(), el));
    }

    @Provide
    public Arbitrary<FieldAccessExpr> genAttributeAccess(
            Map<String, String> ctx,
            Type t
    )
            throws ClassNotFoundException {
        JRGLog.showMessage(
                JRGLog.Severity.MSG_XDEBUG,
                "genAttributeAccess::inicio"
        );

        Arbitrary<Field> f = genCandidatesField(t.asString());

        Field field = f.sample();

        JRGLog.showMessage(
                JRGLog.Severity.MSG_XDEBUG,
                "genAttributeAccess::field: " + field.getName()
        );

        JRGLog.showMessage(
                JRGLog.Severity.MSG_XDEBUG,
                "genAttributeAccess::Class: " + field.getDeclaringClass().getName()
        );

        Arbitrary<Expression> e = genExpression(
                ctx,
                ReflectParserTranslator.reflectToParserType(
                        field.getDeclaringClass().getName()
                )
        );

        JRGLog.showMessage(JRGLog.Severity.MSG_XDEBUG, "genAttributeAccess::fim");

        return e.map(obj -> new FieldAccessExpr(obj, field.getName()));
    }

    @Provide
    public Arbitrary<MethodCallExpr> genMethodInvokation(
            Map<String, String> ctx,
            Type t
    )
            throws ClassNotFoundException {
        JRGLog.showMessage(
                JRGLog.Severity.MSG_XDEBUG,
                "genMethodInvokation:" + ":inicio"
        );

        Arbitrary<Method> methods;

        JRGLog.showMessage(
                JRGLog.Severity.MSG_DEBUG,
                "genMethodInvokation:" + ":t = " + t.asString()
        );

        methods = genConcreteCandidatesMethods(t.asString());// AQUI MUDEI PRA MÉTODOS CONCRETOS!!! 
                                                            // o método gerado ERA function!!

        Method method = methods.sample();

        Class[] params = method.getParameterTypes();

        List<Class> ps = Arrays.asList(params);

        JRGLog.showMessage(
                JRGLog.Severity.MSG_DEBUG,
                "genObjectCreation:" + ":method " + method.toString()
        );

        Arbitrary<Expression> e = genExpression(
                ctx,
                ReflectParserTranslator.reflectToParserType(
                        method.getDeclaringClass().getName()
                )
        );

        List<Type> types = ps
                .stream()
                .map(
                        tname -> ReflectParserTranslator.reflectToParserType(tname.getName())
                )
                .collect(Collectors.toList());

        JRGLog.showMessage(JRGLog.Severity.MSG_XDEBUG, "genMethodInvokation::fim");

        return genExpressionList(ctx, types)
                .map(el -> new MethodCallExpr(e.sample(), method.getName(), el));
    }

    @Provide
    public Arbitrary<MethodCallExpr> genLambdaInvokation(
            Map<String, String> ctx,
            Type t
    )
            throws ClassNotFoundException {
        JRGLog.showMessage(
                JRGLog.Severity.MSG_XDEBUG,
                "genLambdaInvokation:" + ":inicio"
        );

        Arbitrary<Method> methods;

        JRGLog.showMessage(
                JRGLog.Severity.MSG_DEBUG,
                "genLambdaInvokation:" + ":t = " + t.asString()
        );

        methods = genLambdaCandidatesMethods(t.asString());//// AQUI OS LAMBDA CANDIDATE METHODS
        Method method = methods.sample();
        
      
        
        
        //GAMBIARRA////////////////////////////////////////////////////////////////
        System.out.println("method valor:: "+method.toString());
        System.out.println("teste2: "+ method.getClass().toString());
        System.out.println("teste3: "+ method.getName());
        System.out.println("teste4:"+ t.asString());
        
        /*Class<?> functionalInterface = getFunctionalInterface(method);
        System.out.println("Nome da Interface Funcional: " + functionalInterface.getName());
*/
        
        int ultimo = method.toString().lastIndexOf('.');
        
        int penultimo = method.toString().lastIndexOf('.', ultimo - 1);
        
        String interfa = method.toString().substring(penultimo+1, ultimo);
       
        System.out.println("\n"+ interfa);
        
        //////////////////////////////////////////////////////////////////////////
        
        
        Class[] params = method.getParameterTypes();

        List<Class> ps = Arrays.asList(params);

        JRGLog.showMessage(
                JRGLog.Severity.MSG_DEBUG,
                "genObjectCreation:" + ":method " + method.toString()
        );

        Arbitrary<LambdaExpr> e = genLambdaExpr(ctx, ReflectParserTranslator.reflectToParserType(
                method.getDeclaringClass().getName())
        );
        
        
        List<Type> types = ps
                .stream()
                .map(
                        tname -> ReflectParserTranslator.reflectToParserType(tname.getName())
                )
                .collect(Collectors.toList());

        JRGLog.showMessage(JRGLog.Severity.MSG_XDEBUG, "genMethodInvokation::fim");

        return genExpressionList(ctx, types)
                .map(el -> new MethodCallExpr(e.sample(), method.getName(), el));
    }

    
    @Provide
    public Arbitrary<NameExpr> genVar(Map<String, String> ctx, Type t) {
        JRGLog.showMessage(JRGLog.Severity.MSG_XDEBUG, "genVar::inicio");
        System.out.println("tipo:"+ t);
        List<NameExpr> collect = ctx
                .entrySet()
                .stream()
                .filter(e -> e.getValue().equals(t.asString()))
                .map(x -> new NameExpr(x.getKey()))
                .collect(Collectors.toList());

        JRGLog.showMessage(JRGLog.Severity.MSG_XDEBUG, "genVar::fim");

        return Arbitraries.of(collect);
    }

    @Provide
    public Arbitrary<CastExpr> genUpCast(Map<String, String> ctx, Type t)
            throws ClassNotFoundException {
        List<Class> st = mCT.subTypes2(t.asString());

        Arbitrary<Class> sc = Arbitraries.of(st);

        Class c = sc.sample();

        Arbitrary<Expression> e = genExpression(
                ctx,
                ReflectParserTranslator.reflectToParserType(c.getName())
        );

        return e.map(
                obj
                -> new CastExpr(
                        ReflectParserTranslator.reflectToParserType(t.asString()),
                        obj
                )
        );
    }
    //mValidNames.remove(name); // 

    @Provide
    public Arbitrary<LambdaExpr> genLambdaExpr(Map<String, String> ctx, Type t)
            throws ClassNotFoundException {
        System.out.println("core lambdaExpr: "+ t);
        List<Method> absMethods = mCT.getInterfaceAbstractMethods(t.asString());//recebe os métodos abstratos

        Method sign = absMethods.get(0); // se é funcional só tem um
        
        List<Class> types = Arrays.asList(sign.getParameterTypes());//tipos dométodo
        
        NodeList<Parameter> params = new NodeList<>();
        
        for (Class type : types) {
            String name = Arbitraries.of(mValidNames).sample();// gera um nome
            ctx.put(name, type.getName());
            params.add(new Parameter(
                    ReflectParserTranslator.reflectToParserType(
                            type.getName()), name));
        }
        Arbitrary<Expression> e;

        try {
            //e = genExpressionLambda(ctx, ReflectParserTranslator.reflectToParserType(sign.getReturnType().toString()));
          System.out.println("TESTE LAMBDA EXPR: ctx "+ ctx+" t: "+ReflectParserTranslator.reflectToParserType(sign.getReturnType().getName()));
            e = genExpression(ctx,
                    ReflectParserTranslator.reflectToParserType(
                            sign.getReturnType().getName()
                    ));//recebe o contexto e o tipo de retorno do método abstrato
        } catch (Exception ex) {
            System.out.println("genLambdaExpr::error ==> " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }
        return e.map(obj -> new LambdaExpr(params, obj));
    }

  
    @Provide
    public Arbitrary<Method> genConcreteCandidatesMethods(String type)
            throws ClassNotFoundException {
        List<Method> candidatesMethods;

        candidatesMethods = mCT.getConcreteCandidateMethods(type);
        //System.out.println("gen: " + type);

        return Arbitraries.of(candidatesMethods);
    }

    public Arbitrary<Method> genLambdaCandidatesMethods(String type)
            throws ClassNotFoundException {
        List<Method> candidatesMethods;

        candidatesMethods = mCT.getLambdaCandidateMethods(type);
        //System.out.println("gen: " + type);

        return Arbitraries.of(candidatesMethods);
    }

    @Provide
    public Arbitrary<Method> genCandidatesMethods(String type)
            throws ClassNotFoundException {
        List<Method> candidatesMethods;

        candidatesMethods = mCT.getCandidateMethods(type);

        return Arbitraries.of(candidatesMethods);
    }

    @Provide
    public Arbitrary<Field> genCandidatesField(String type)
            throws ClassNotFoundException {
        List<Field> candidatesField;

        candidatesField = mCT.getCandidateFields(type);

        return Arbitraries.of(candidatesField);
    }

    @Provide
    public Arbitrary<Constructor> genCandidatesConstructors(String type)
            throws ClassNotFoundException {
        List<Constructor> candidatesConstructors;

        candidatesConstructors = mCT.getCandidateConstructors(type);

        return Arbitraries.of(candidatesConstructors);
    }

    @Provide
    public Arbitrary<Class> genCandidateUpCast(String type)
            throws ClassNotFoundException {
        List<Class> upCast;

        upCast = mCT.subTypes2(type);

        return Arbitraries.of(upCast);
    }

    @Provide
    public Arbitrary<Expression> genExpressionOperator(
            Map<String, String> ctx,
            Type t
    ) throws ClassNotFoundException {
        Arbitrary<Expression> e;

        List<Arbitrary<Expression>> cand = new ArrayList<>();

        if (t.toString().equals(PrimitiveType.booleanType().asString())) {
            cand.add(
                    Arbitraries.oneOf(
                            mOperator.genLogiExpression(ctx),
                            mOperator.genRelaExpression(ctx)
                    )
            );
        }
        if (t.isPrimitiveType() && mBase.isNumericType(t)) {
            cand.add(Arbitraries.oneOf(mOperator.genArithExpression(ctx, t)));
        }
        return Arbitraries.oneOf(cand);
    }

    @Provide
    public Arbitrary<Expression> genExpressionOperatorFor(
            Map<String, String> ctx,
            Type t,
            VariableDeclarator e,
            Arbitrary<LiteralExpr> ex
    ) throws ClassNotFoundException {
        List<Arbitrary<Expression>> cand = new ArrayList<>();

        if (t.toString().equals(PrimitiveType.intType().asString())) {
            cand.add(
                    Arbitraries.oneOf(mOperator.genRelacionalBooleanFor(ctx, e, ex))
            );
        }

        return Arbitraries.oneOf(cand);
    }

    public Arbitrary<Expression> genExpressionMatchOperatorFor(
            Map<String, String> ctx,
            Type t,
            VariableDeclarator e,
            Arbitrary<LiteralExpr> ex
    ) throws ClassNotFoundException {
        List<Arbitrary<Expression>> cand = new ArrayList<>();

        if (t.toString().equals(PrimitiveType.intType().asString())) {
            cand.add(Arbitraries.oneOf(mOperator.genArithBooleanFor(ctx, e, ex)));
        }

        return Arbitraries.oneOf(cand);
    }
}
