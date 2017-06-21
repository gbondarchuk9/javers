package org.javers.common.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bartosz walacik
 */
class JaversGetterFactory {
    private final Class getterSource;

    JaversGetterFactory(Class getterSource) {
        this.getterSource = getterSource;
    }

    /**
     * List all class getters, including inherited and private.
     */
    List<JaversGetter> getAllGetters() {
        List<JaversGetter> getters = new ArrayList<>();
        TypeResolvingContext context = new TypeResolvingContext();

        Class clazz = getterSource;
        while (clazz != null && clazz != Object.class) {
            context.addTypeSubstitutions(clazz);
            Arrays.stream(clazz.getDeclaredMethods())
                    .filter(method -> isGetter(method) && !method.isBridge())
                    .forEach(getterMethod -> {
                        final List<JaversGetter> overridden = getters.stream()
                                .filter((existing) -> isOverridden(getterMethod, existing.getRawMember()))
                                .collect(Collectors.toList());
                        final boolean looksLikeId = overridden.stream().anyMatch(JaversMember::looksLikeId);
                        getters.removeAll(overridden);
                        getters.add(createJaversGetter(getterMethod, context, looksLikeId));
                    });
            clazz = clazz.getSuperclass();
        }

        return getters;
    }

    private static boolean isGetter(Method rawMethod) {
        return hasGetOrIsPrefix(rawMethod) &&
                hasNoParamters(rawMethod) &&
                returnsSomething(rawMethod) &&
                isNotStatic(rawMethod) &&
                isNotNative(rawMethod);
    }

    private static boolean hasGetOrIsPrefix(Method rawMethod) {
        return rawMethod.getName().startsWith("get") ||
                rawMethod.getName().startsWith("is");
    }

    private static boolean hasNoParamters(Method rawMethod) {
        return rawMethod.getParameterTypes().length == 0;
    }

    private static boolean returnsSomething(Method rawMethod) {
        return rawMethod.getGenericReturnType() != void.class;
    }

    private static boolean isNotStatic(Method rawMethod) {
        return !Modifier.isStatic(rawMethod.getModifiers());
    }

    private static boolean isNotNative(Method rawMethod) {
        return !Modifier.isNative(rawMethod.getModifiers());
    }

    private static boolean isOverridden(final Method parent, final Method toCheck) {
        return isSubClass(parent, toCheck) &&
                sameMethodName(parent, toCheck) &&
                returnTypeCovariant(parent, toCheck) &&
                sameArguments(parent, toCheck);
    }

    private static boolean isSubClass(final Method parent, final Method toCheck) {
        return parent.getDeclaringClass().isAssignableFrom(toCheck.getDeclaringClass());
    }

    private static boolean sameMethodName(final Method parent, final Method toCheck) {
        return parent.getName().equals(toCheck.getName());
    }

    private static boolean returnTypeCovariant(final Method parent, final Method toCheck) {
        return parent.getReturnType().isAssignableFrom(toCheck.getReturnType());
    }

    private static boolean sameArguments(final Method parent, final Method toCheck) {
        return Arrays.equals(parent.getParameterTypes(), toCheck.getParameterTypes());
    }

    private JaversGetter createJaversGetter(Method getterMethod, TypeResolvingContext context, boolean looksLikeId) {
        Type actualReturnType = context.getSubstitution(getterMethod.getGenericReturnType());
        return new JaversGetter(getterMethod, actualReturnType, looksLikeId);
    }
}
