package cucumber.runtime.snippets;

import gherkin.I18n;
import gherkin.formatter.model.Step;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for generating snippets.
 * <p/>
 * Subclasses can access common values:
 * <ul>
 * <li>{0} : Keyword</li>
 * <li>{1} : Regexp</li>
 * <li>{2} : Function name</li>
 * <li>{3} : Arguments</li>
 * <li>{4} : Hint comment</li>
 * </ul>
 */
public abstract class SnippetGenerator {
    private static final ArgumentPattern[] DEFAULT_ARGUMENT_PATTERNS = new ArgumentPattern[]{
            new ArgumentPattern(Pattern.compile("\"([^\"]*)\""), String.class),
            new ArgumentPattern(Pattern.compile("(\\d+)"), Integer.TYPE)
    };
    private static final Pattern GROUP_PATTERN = Pattern.compile("\\(");
    private static final String HINT = "Express the Regexp above with the code you wish you had";
    private static final Character SUBST = '_';

    private final Step step;
    private final String namedGroupStart;
    private final String namedGroupEnd;

    /**
     * Constructor for languages that do not support named capture groups, such as Java.
     *
     * @param step the step to generate snippet for.
     */
    protected SnippetGenerator(Step step) {
        this(step, null, null);
    }

    /**
     * Constructor for langauges that support named capture groups, such ash Ioke.
     *
     * @param step            the step to generate snippet for.
     * @param namedGroupStart beginning of named group, for example "{arg".
     * @param namedGroupEnd   end of named group, for example "}".
     */
    protected SnippetGenerator(Step step, String namedGroupStart, String namedGroupEnd) {
        this.step = step;
        this.namedGroupStart = namedGroupStart;
        this.namedGroupEnd = namedGroupEnd;
    }

    public String getSnippet() {
        return MessageFormat.format(template(), I18n.codeKeywordFor(step.getKeyword()), patternFor(step.getName()), functionName(step.getName()), arguments(argumentTypes(step.getName())), HINT);
    }

    protected abstract String template();

    protected abstract String arguments(List<Class<?>> argumentTypes);

    protected String patternFor(String stepName) {
        String pattern = stepName;
        for (ArgumentPattern argumentPattern : argumentPatterns()) {
            pattern = argumentPattern.replaceMatchesWithGroups(pattern);
        }
        if (namedGroupStart != null) {
            pattern = withNamedGroups(pattern);
        }

        return "^" + pattern + "$";
    }

    private String functionName(String name) {
        String functionName = name;
        for (ArgumentPattern argumentPattern : argumentPatterns()) {
            functionName = argumentPattern.replaceMatchesWithSpace(functionName);
        }
        functionName = sanitizeFunctionName(functionName);
        return functionName;
    }

    protected String sanitizeFunctionName(String functionName) {
        StringBuilder sanitized = new StringBuilder();
        sanitized.append(Character.isJavaIdentifierStart(functionName.charAt(0)) ? functionName.charAt(0) : SUBST);
        for (int i = 1; i < functionName.length(); i++) {
            if (Character.isJavaIdentifierPart(functionName.charAt(i))) {
                sanitized.append(functionName.charAt(i));
            } else if (sanitized.charAt(sanitized.length() - 1) != SUBST && i != functionName.length() - 1) {
                sanitized.append(SUBST);
            }
        }
        return sanitized.toString();
    }

    private String withNamedGroups(String snippetPattern) {
        Matcher m = GROUP_PATTERN.matcher(snippetPattern);

        StringBuffer sb = new StringBuffer();
        int n = 1;
        while (m.find()) {
            m.appendReplacement(sb, "(" + namedGroupStart + n++ + namedGroupEnd);
        }
        m.appendTail(sb);

        return sb.toString();
    }


    private List<Class<?>> argumentTypes(String name) {
        List<Class<?>> argTypes = new ArrayList<Class<?>>();
        Matcher[] matchers = new Matcher[argumentPatterns().length];
        for (int i = 0; i < argumentPatterns().length; i++) {
            matchers[i] = argumentPatterns()[i].pattern().matcher(name);
        }
        int pos = 0;
        while (true) {
            for (int i = 0; i < matchers.length; i++) {
                Matcher m = matchers[i].region(pos, name.length());
                if (m.lookingAt()) {
                    Class<?> typeForSignature = argumentPatterns()[i].type();
                    argTypes.add(typeForSignature);
                }
            }
            if (pos++ == name.length()) {
                break;
            }
        }
        return argTypes;
    }

    protected ArgumentPattern[] argumentPatterns() {
        return DEFAULT_ARGUMENT_PATTERNS;
    }

    protected String untypedArguments(List<Class<?>> argumentTypes) {
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < argumentTypes.size(); n++) {
            if (n > 1) {
                sb.append(", ");
            }
            sb.append("arg").append(n + 1);
        }
        return sb.toString();
    }
}