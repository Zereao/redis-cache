package cx.twinkle.rediscache.cache;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * @author twinkle
 * @version 2019/12/28 11:25
 */
public class SpelParser {
    private SpelExpressionParser spelExpressionParser;
    private BeanFactory beanFactory;

    public SpelParser(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.spelExpressionParser = new SpelExpressionParser();
    }

    public <T> T parse(String expression, Method method, Class<T> cls, Object... args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver(new BeanFactoryResolver(beanFactory));
        context.setVariable("method", method);
        context.setVariable("args", args);
        if (args != null && args.length > 0) {
            for (int i = 0, len = args.length; i < len; i++) {
                context.setVariable("p" + i, args[i]);
            }
        }
        Expression exp = spelExpressionParser.parseExpression(expression, ParserContext.TEMPLATE_EXPRESSION);
        return exp.getValue(context, cls);
    }
}
