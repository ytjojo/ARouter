package com.alibaba.android.arouter.core;

import android.content.Context;
import android.net.Uri;

import com.alibaba.android.arouter.base.PriorityList;
import com.alibaba.android.arouter.exception.HandlerException;
import com.alibaba.android.arouter.exception.NoRouteFoundException;
import com.alibaba.android.arouter.facade.DeepLinkUri;
import com.alibaba.android.arouter.facade.InterceptorResult;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.enums.TypeKind;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import com.alibaba.android.arouter.facade.template.IDeepLinkMatcher;
import com.alibaba.android.arouter.facade.template.IInterceptorGroup;
import com.alibaba.android.arouter.facade.template.IMethodInvoker;
import com.alibaba.android.arouter.facade.template.IMultiImplementGroup;
import com.alibaba.android.arouter.facade.template.IMultiImplementRegister;
import com.alibaba.android.arouter.facade.template.IPrivateInterceptor;
import com.alibaba.android.arouter.facade.template.IProvider;
import com.alibaba.android.arouter.facade.template.IProviderGroup;
import com.alibaba.android.arouter.facade.template.IRouteGroup;
import com.alibaba.android.arouter.facade.template.IRouteMetaRegister;
import com.alibaba.android.arouter.facade.template.IRouteRoot;
import com.alibaba.android.arouter.facade.template.ITemplateGroup;
import com.alibaba.android.arouter.facade.util.ArrayUtils;
import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.android.arouter.utils.ClassUtils;
import com.alibaba.android.arouter.utils.CollectionUtils;
import com.alibaba.android.arouter.utils.Consts;
import com.alibaba.android.arouter.utils.MapUtils;
import com.alibaba.android.arouter.utils.PackageUtils;
import com.alibaba.android.arouter.utils.TextUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import static com.alibaba.android.arouter.launcher.ARouter.logger;
import static com.alibaba.android.arouter.utils.Consts.AROUTER_SP_CACHE_KEY;
import static com.alibaba.android.arouter.utils.Consts.AROUTER_SP_KEY_MAP;
import static com.alibaba.android.arouter.utils.Consts.DOT;
import static com.alibaba.android.arouter.utils.Consts.ROUTE_ROOT_PAKCAGE;
import static com.alibaba.android.arouter.utils.Consts.SDK_NAME;
import static com.alibaba.android.arouter.utils.Consts.SEPARATOR;
import static com.alibaba.android.arouter.utils.Consts.SUFFIX_INTERCEPTORS;
import static com.alibaba.android.arouter.utils.Consts.SUFFIX_PROVIDERS;
import static com.alibaba.android.arouter.utils.Consts.SUFFIX_ROOT;
import static com.alibaba.android.arouter.utils.Consts.TAG;

/**
 * LogisticsCenter contains all of the map.
 * <p>
 * 1. Creates instance when it is first used.
 * 2. Handler Multi-Module relationship map(*)
 * 3. Complex logic to solve duplicate group definition
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/23 15:02
 */
public class LogisticsCenter {
    private static Context mContext;
    static ThreadPoolExecutor executor;
    private static boolean registerByPlugin;

    /**
     * arouter-auto-register plugin will generate code inside this method
     * call this method to register all Routers, Interceptors and Providers
     */
    private static void loadRouterMap() {
        registerByPlugin = false;
        // auto generate register code by gradle plugin: arouter-auto-register
        // looks like below:
        // registerRouteRoot(new ARouter..Root..modulejava());
        // registerRouteRoot(new ARouter..Root..modulekotlin());
    }

    /**
     * register by class name
     * Sacrificing a bit of efficiency to solve
     * the problem that the main dex file size is too large
     */
    private static void register(String className) {
        if (!TextUtils.isEmpty(className)) {
            try {
                Class<?> clazz = Class.forName(className);
                Object obj = clazz.getConstructor().newInstance();
                if (obj instanceof IRouteRoot) {
                    registerRouteRoot((IRouteRoot) obj);
                } else if (obj instanceof IProviderGroup) {
                    registerProvider((IProviderGroup) obj);
                } else if (obj instanceof IInterceptorGroup) {
                    registerInterceptor((IInterceptorGroup) obj);
                } else if (obj instanceof IMultiImplementGroup) {
                    registerMultiImplements((IMultiImplementGroup) obj);
                } else if (obj instanceof ITemplateGroup) {
                    registerTemplate((ITemplateGroup) obj);
                } else {
                    logger.info(TAG, "register failed, class name: " + className
                            + " should implements one of IRouteRoot/IProviderGroup/IInterceptorGroup.");
                }
            } catch (Exception e) {
                logger.error(TAG, "register class error:" + className);
            }
        }
    }

    /**
     * method for arouter-auto-register plugin to register Routers
     *
     * @param routeRoot IRouteRoot implementation class in the package: com.alibaba.android.arouter.core.routers
     */
    private static void registerRouteRoot(IRouteRoot routeRoot) {
        markRegisteredByPlugin();
        if (routeRoot != null) {
            routeRoot.loadInto(Warehouse.groupsIndex);
        }
    }

    /**
     * method for arouter-auto-register plugin to register Interceptors
     *
     * @param interceptorGroup IInterceptorGroup implementation class in the package: com.alibaba.android.arouter.core.routers
     */
    private static void registerInterceptor(IInterceptorGroup interceptorGroup) {
        markRegisteredByPlugin();
        if (interceptorGroup != null) {
            interceptorGroup.loadInto(Warehouse.interceptorsIndex);
        }
    }

    /**
     * method for arouter-auto-register plugin to register Providers
     *
     * @param providerGroup IProviderGroup implementation class in the package: com.alibaba.android.arouter.core.routers
     */
    private static void registerProvider(IProviderGroup providerGroup) {
        markRegisteredByPlugin();
        if (providerGroup != null) {
            providerGroup.loadInto(Warehouse.providersIndex);
        }
    }

    /**
     * mark already registered by arouter-auto-register plugin
     */
    private static void markRegisteredByPlugin() {
        if (!registerByPlugin) {
            registerByPlugin = true;
        }
    }

    /**
     * LogisticsCenter init, load all metas in memory. Demand initialization
     */
    public synchronized static void init(Context context, ThreadPoolExecutor tpe) throws HandlerException {
        mContext = context;
        executor = tpe;

        try {
            long startInit = System.currentTimeMillis();
            //load by plugin first
            loadRouterMap();
            if (registerByPlugin) {
                logger.info(TAG, "Load router map by arouter-auto-register plugin.");
            } else {
                Set<String> routerMap;

                // It will rebuild router map every times when debuggable.
                if (ARouter.debuggable() || PackageUtils.isNewVersion(context)) {
                    logger.info(TAG, "Run with debug mode or new install, rebuild router map.");
                    // These class was generated by arouter-compiler.
                    routerMap = ClassUtils.getFileNameByPackageName(mContext, ROUTE_ROOT_PAKCAGE);
                    if (!routerMap.isEmpty()) {
                        context.getSharedPreferences(AROUTER_SP_CACHE_KEY, Context.MODE_PRIVATE).edit().putStringSet(AROUTER_SP_KEY_MAP, routerMap).apply();
                    }

                    PackageUtils.updateVersion(context);    // Save new version name when router map update finishes.
                } else {
                    logger.info(TAG, "Load router map from cache.");
                    routerMap = new HashSet<>(context.getSharedPreferences(AROUTER_SP_CACHE_KEY, Context.MODE_PRIVATE).getStringSet(AROUTER_SP_KEY_MAP, new HashSet<String>()));
                }

                logger.info(TAG, "Find router map finished, map size = " + routerMap.size() + ", cost " + (System.currentTimeMillis() - startInit) + " ms.");
                startInit = System.currentTimeMillis();

                for (String className : routerMap) {
                    if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_ROOT)) {
                        // This one of root elements, load root.
                        ((IRouteRoot) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.groupsIndex);
                    } else if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_INTERCEPTORS)) {
                        // Load interceptorMeta
                        ((IInterceptorGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.interceptorsIndex);
                    } else if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_PROVIDERS)) {
                        // Load providerIndex
                        ((IProviderGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.providersIndex);
                    }
                }
            }

            logger.info(TAG, "Load root element finished, cost " + (System.currentTimeMillis() - startInit) + " ms.");

            if (Warehouse.groupsIndex.size() == 0) {
                logger.error(TAG, "No mapping files were found, check your configuration please!");
            }

            if (ARouter.debuggable()) {
                logger.debug(TAG, String.format(Locale.getDefault(), "LogisticsCenter has already been loaded, GroupIndex[%d], InterceptorIndex[%d], ProviderIndex[%d]", Warehouse.groupsIndex.size(), Warehouse.interceptorsIndex.size(), Warehouse.providersIndex.size()));
            }
        } catch (Exception e) {
            throw new HandlerException(TAG + "ARouter init logistics center exception! [" + e.getMessage() + "]");
        }
    }


    /**
     * Completion the postcard by route metas
     *
     * @param postcard Incomplete postcard, should complete by this method.
     */
    public synchronized static void completion(Postcard postcard) {
        if (null == postcard) {
            throw new NoRouteFoundException(TAG + "No postcard!");
        }
        if (postcard.getType() != null && postcard.getDestination() != null) {
            return;
        }

        RouteMeta routeMeta = findRouteMeta(postcard);
        if (null == routeMeta) {
            throw new NoRouteFoundException(TAG + "There is no route match the path [" + postcard.getPath() + "], in group [" + postcard.getGroup() + "]");
        } else {
            postcard.setDestination(routeMeta.getDestination());
            postcard.setType(routeMeta.getType());
            postcard.setPriority(routeMeta.getPriority());
            postcard.setExtra(routeMeta.getExtra());
            postcard.setInterceptors(routeMeta.getInterceptors());

            Uri rawUri = postcard.getUri();
            if (null != rawUri) {   // Try to set params into bundle.
                Map<String, String> resultMap = TextUtils.splitQueryParameters(rawUri);
                Map<String, Integer> paramsType = routeMeta.getParamsType();

                if (MapUtils.isNotEmpty(paramsType)) {
                    // Set value by its type, just for params which annotation by @Param
                    for (Map.Entry<String, Integer> params : paramsType.entrySet()) {
                        setValue(postcard,
                                params.getValue(),
                                params.getKey(),
                                resultMap.get(params.getKey()));
                    }

                    // Save params name which need auto inject.
                    postcard.getExtras().putStringArray(ARouter.AUTO_INJECT, paramsType.keySet().toArray(new String[]{}));
                }

                // Save raw uri
                postcard.withString(ARouter.RAW_URI, rawUri.toString());
            }

            switch (routeMeta.getType()) {
                case PROVIDER:  // if the route is provider, should find its instance
                    // Its provider, so it must implement IProvider
                    Class<? extends IProvider> providerMeta = (Class<? extends IProvider>) routeMeta.getDestination();
                    IProvider instance = Warehouse.providers.get(providerMeta);
                    if (null == instance) { // There's no instance of this provider
                        IProvider provider;
                        try {
                            provider = providerMeta.getConstructor().newInstance();
                            provider.init(mContext);
                            Warehouse.providers.put(providerMeta, provider);
                            instance = provider;
                        } catch (Exception e) {
                            throw new HandlerException("Init provider failed! " + e.getMessage());
                        }
                    }
                    postcard.setProvider(instance);
                    postcard.greenChannel();    // Provider should skip all of interceptors
                    break;
                case METHOD:
                    break;
                case FRAGMENT:
                    postcard.greenChannel();    // Fragment needn't interceptors
                default:
                    break;
            }
        }
    }

    /**
     * Set value by known type
     *
     * @param postcard postcard
     * @param typeDef  type
     * @param key      key
     * @param value    value
     */
    private static void setValue(Postcard postcard, Integer typeDef, String key, String value) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
            return;
        }

        try {
            if (null != typeDef) {
                if (typeDef == TypeKind.BOOLEAN.ordinal()) {
                    postcard.withBoolean(key, Boolean.parseBoolean(value));
                } else if (typeDef == TypeKind.BYTE.ordinal()) {
                    postcard.withByte(key, Byte.parseByte(value));
                } else if (typeDef == TypeKind.SHORT.ordinal()) {
                    postcard.withShort(key, Short.parseShort(value));
                } else if (typeDef == TypeKind.INT.ordinal()) {
                    postcard.withInt(key, Integer.parseInt(value));
                } else if (typeDef == TypeKind.LONG.ordinal()) {
                    postcard.withLong(key, Long.parseLong(value));
                } else if (typeDef == TypeKind.FLOAT.ordinal()) {
                    postcard.withFloat(key, Float.parseFloat(value));
                } else if (typeDef == TypeKind.DOUBLE.ordinal()) {
                    postcard.withDouble(key, Double.parseDouble(value));
                } else if (typeDef == TypeKind.STRING.ordinal()) {
                    postcard.withString(key, value);
                } else if (typeDef == TypeKind.PARCELABLE.ordinal()) {
                    // TODO : How to description parcelable value with string?
                } else if (typeDef == TypeKind.OBJECT.ordinal()) {
                    postcard.withString(key, value);
                } else {    // Compatible compiler sdk 1.0.3, in that version, the string type = 18
                    postcard.withString(key, value);
                }
            } else {
                postcard.withString(key, value);
            }
        } catch (Throwable ex) {
            logger.warning(Consts.TAG, "LogisticsCenter setValue failed! " + ex.getMessage());
        }
    }

    /**
     * Suspend business, clear cache.
     */
    public static void suspend() {
        Warehouse.clear();
    }

    public synchronized static void addRouteGroupDynamic(String groupName, IRouteGroup group) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (Warehouse.groupsIndex.containsKey(groupName)) {
            // If this group is included, but it has not been loaded
            // load this group first, because dynamic route has high priority.
            Warehouse.groupsIndex.get(groupName).getConstructor().newInstance().loadInto(Warehouse.routes);
            Warehouse.groupsIndex.remove(groupName);
        }

        // cover old group.
        if (null != group) {
            group.loadInto(Warehouse.routes);
        }
    }


    public static void addRouteMeta(String path, RouteMeta routemeta) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("path must star with /");
        }
        Warehouse.routes.put(path, routemeta);
        if (!ArrayUtils.isEmpty(routemeta.getSecondaryPathes()))
            for (String str : routemeta.getSecondaryPathes()) {
                if (!str.startsWith("/") || TextUtils.containChars(str, "<{[.(:")) {
                    Warehouse.pathMappings.put(str, new DeepLinkUri(str, routemeta));
                } else {
                    Warehouse.routes.put(str, routemeta);
                }
            }
    }

    /**
     * Build postcard by serviceName
     *
     * @param interfaceClass interfaceClass
     * @return postcard
     */
    public static Postcard buildProvider(Class interfaceClass) {
        RouteMeta routeMeta = Warehouse.providersIndex.get(interfaceClass);
        return (routeMeta == null) ? null : new Postcard(routeMeta.getPath(), routeMeta.getGroup(), routeMeta.getDestination());
    }

    public static <T> T buildTemplateImpl(Class<T> templateClass) {
        try {
            return (T) Warehouse.templates.get(templateClass).getConstructor().newInstance();
        } catch (Exception e) {
            ARouter.logger.error("ARouter::", "Fetch templateClass instance error, " + TextUtils.formatStackTrace(e.getStackTrace()));
        }
        return null;
    }

    private static RouteMeta findRouteMeta(Postcard postcard) {
        RouteMeta routeMeta = null;
        routeMeta = findBykey(postcard);
        if (routeMeta != null) {
            return routeMeta;
        }
        if (loadByGroup(postcard.getGroup())) {
            routeMeta = findBykey(postcard);
            if (routeMeta != null) {
                return routeMeta;
            }
        }
        if (!Warehouse.groupsIndex.isEmpty()) {
            loadAllGroup();
        }
        routeMeta = findBykey(postcard);
        if (routeMeta != null) {
            return routeMeta;
        }
        List<IDeepLinkMatcher> deepLinkMatchers = ARouter.getInstance().getMultiImplements(IDeepLinkMatcher.class);
        if (!CollectionUtils.isEmpty(deepLinkMatchers)) {
            for (IDeepLinkMatcher deepLinkMatcher : deepLinkMatchers) {
                routeMeta = deepLinkMatcher.findMatch(postcard.getUri(), Warehouse.pathMappings, Warehouse.routes);
                if (routeMeta != null) {
                    return routeMeta;
                }
            }
        }
        return findByRegex(postcard);
    }

    private static RouteMeta findBykey(Postcard postcard) {
        return Warehouse.routes.get(postcard.getPath());
    }

    private static RouteMeta findByRegex(Postcard postcard) {
        if (!Warehouse.pathMappings.isEmpty() && postcard.getUri() != null) {
            PriorityList<DeepLinkUri> priorityList = new PriorityList();
            for (Map.Entry<String, DeepLinkUri> entry : Warehouse.pathMappings.entrySet()) {
                if (entry.getValue().isMatcher(postcard.getUri().toString())) {
                    DeepLinkUri deepLinkUri = entry.getValue();
                    priorityList.addItem(deepLinkUri, deepLinkUri.getPriority());
                }
            }
            if (!priorityList.isEmpty()) {
                DeepLinkUri deepLinkUri = (DeepLinkUri) priorityList.get(0);
                injectPlaceHolders(postcard, deepLinkUri, postcard.getUri().toString());
                return deepLinkUri.getRouteMeta();
            }
        }
        return null;
    }


    public static IMethodInvoker getIInvokeMethod(Class<IMethodInvoker> clazz) {
        IMethodInvoker iMethodInvoker = Warehouse.methodInvoker.get(clazz);
        if (iMethodInvoker == null)
            try {
                iMethodInvoker = clazz.getConstructor(new Class[0]).newInstance(new Object[0]);
                Warehouse.methodInvoker.put(clazz, iMethodInvoker);
                return iMethodInvoker;
            } catch (Exception exception) {
                throw new HandlerException("Init multiImplements failed! " + exception.getMessage());
            }
        return iMethodInvoker;
    }

    public static <T> List<T> getMultiImplements(Class<? extends T> clazz) {
        List<T> list = (List<T>) Warehouse.multImplmentsIntances.get(clazz);
        if (list == null) {
            PriorityList<T> priorityList = new PriorityList();
            List<RouteMeta> routeMetas = Warehouse.multImplments.get(clazz);
            if (!CollectionUtils.isEmpty(routeMetas)) {
                for (RouteMeta meta : routeMetas) {
                    try {
                        priorityList.addItem((T) meta.getDestination().getConstructor().newInstance(), meta.getPriority());
                    } catch (Exception e) {
                        throw new HandlerException("Init multiImplements failed! " + e.getMessage());
                    }
                }
            }
            Warehouse.multImplmentsIntances.put(clazz, priorityList);
            list = priorityList;
        }

        ArrayList<T> arrayList = new ArrayList<>();
        if (!list.isEmpty()) {
            arrayList.addAll(list);
            return arrayList;
        }
        return arrayList;
    }

    public static void createPrivateInterceptors(Postcard postcard) {
        if (postcard.getInterceptors() != null) {
            ArrayList<IPrivateInterceptor> arrayList = postcard.getPrivateInterceptors();
            if (CollectionUtils.isEmpty(arrayList)) {
                arrayList = new ArrayList();
                for (Class<IPrivateInterceptor> clazz : postcard.getInterceptors()) {
                    if (IPrivateInterceptor.class.isAssignableFrom(clazz)) {
                        try {
                            arrayList.add((IPrivateInterceptor) clazz.getConstructor().newInstance());
                        } catch (Exception exception) {
                            ARouter.logger.error("ARouter::", "Fetch IPrivateInterceptor instance error, " + TextUtils.formatStackTrace(exception.getStackTrace()));
                        }
                    }
                }
                postcard.setPrivateInterceptors(arrayList);
            }
        }
    }

    public static InterceptorResult doPrivateInterceptions(Postcard postcard) {
        createPrivateInterceptors(postcard);
        ArrayList<IPrivateInterceptor> privateInterceptors = postcard.getPrivateInterceptors();
        if (!CollectionUtils.isEmpty(privateInterceptors)) {
            int begin = postcard.getPauseCause() == null ? -1 : privateInterceptors.indexOf(postcard.getPauseCause()) + 1;

            for (int i = 0; i < privateInterceptors.size(); i++) {
                IPrivateInterceptor iPrivateInterceptor = privateInterceptors.get(i);
                if (i < begin) {
                    continue;
                }
                iPrivateInterceptor.process(postcard.getContext(), postcard);
                if (ARouter.getInstance().isPaused(postcard)) {
                    postcard.setPauseCause(iPrivateInterceptor);
                    return InterceptorResult.PAUSE;
                } else if (postcard.getTag() != null) {
                    return InterceptorResult.INTERRUPT;
                } else {
                    postcard.setPauseCause(null);
                }
            }
            return InterceptorResult.CONTINUE;
        }
        return InterceptorResult.CONTINUE;
    }


    private static void injectPlaceHolders(Postcard postcard, DeepLinkUri deepLinkUri, String url) {
        Map<String, String> placeHolderValues = deepLinkUri.getPlaceHolderValues(url);
        if (CollectionUtils.isEmpty(placeHolderValues)) {
            return;
        }
        Map<String, Integer> paramsType = deepLinkUri.getRouteMeta().getParamsType();
        if (MapUtils.isNotEmpty(paramsType)) {
            for (Map.Entry<String, Integer> entry : paramsType.entrySet()) {
                final String key = entry.getKey();
                setValue(postcard, entry.getValue(), key, placeHolderValues.get(key));
            }
            ArrayList placeHolderKeys = new ArrayList();
            placeHolderKeys.addAll(placeHolderValues.keySet());
            postcard.getExtras().putStringArrayList(ARouter.AUTO_INJECT_PLACEHOLDERS, placeHolderKeys);
        }
    }

    private static void loadAllGroup() {
        if (Warehouse.groupsIndex.isEmpty()) {
            return;
        }
        final Map<String, Class<? extends IRouteGroup>> allGroup = Warehouse.groupsIndex;
        HashMap<String, RouteMeta> allRouteMeta = new HashMap<>();
        for (Map.Entry<String, Class<? extends IRouteGroup>> entry : allGroup.entrySet()) {
            try {
                ((IRouteGroup) ((Class<IRouteGroup>) entry.getValue()).getConstructor().newInstance()).loadInto(allRouteMeta);
            } catch (Exception exception) {
                throw new HandlerException("ARouter::Fatal exception when loading group meta. [" + exception.getMessage() + "]");
            }
        }
        Warehouse.groupsIndex.clear();

        List<IRouteMetaRegister> registers = ARouter.getInstance().getMultiImplements(IRouteMetaRegister.class);
        if (!CollectionUtils.isEmpty(registers)) {
            for (IRouteMetaRegister register : registers) {
                register.loadInto(allRouteMeta);
            }
        }

        for (Map.Entry<String, RouteMeta> entry : allRouteMeta.entrySet()) {
            addRouteMeta(entry.getKey(), entry.getValue());
        }

    }

    private static boolean loadByGroup(String group) {
        if (TextUtils.isEmpty(group)) {
            return false;
        }
        Class<? extends IRouteGroup> groupClazz = Warehouse.groupsIndex.get(group);
        if (groupClazz == null)
            return false;
        try {
            IRouteGroup iRouteGroup = groupClazz.getConstructor().newInstance();
            HashMap<String, RouteMeta> groupRoutes = new HashMap();
            iRouteGroup.loadInto(groupRoutes);
            Warehouse.groupsIndex.remove(group);

            for (Map.Entry<String, RouteMeta> entry : groupRoutes.entrySet()) {
                String path = entry.getKey();
                RouteMeta routeMeta = entry.getValue();
                addRouteMeta(path, routeMeta);
            }
            return true;
        } catch (Exception exception) {
            throw new HandlerException(exception.toString());
        }
    }

    public static void putRoute(String path, RouteMeta routemeta) {
        Warehouse.routes.put(path, routemeta);
    }

    private static void registerMultiImplements(IMultiImplementGroup multiImplementsGroup) {
        markRegisteredByPlugin();
        if (multiImplementsGroup != null) {
            multiImplementsGroup.loadInto(ARouter.getInstance().navigation(IMultiImplementRegister.class));
        }
    }

    private static void registerTemplate(ITemplateGroup templateGroup) {
        markRegisteredByPlugin();
        if (templateGroup != null) {
            templateGroup.loadInto(Warehouse.templates);
        }
    }

    public static boolean hasInterceptors() {
        return MapUtils.isNotEmpty(Warehouse.interceptorsIndex);
    }
}
