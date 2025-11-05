# android路由改造


### ARouter不足

#### 路由注解不支持多个路由
官方解决方式是：
>PretreatmentService或者PathReplaceService中全局处理。

缺点是集中式替换，而不是模块内部自己处理。

#### 路由注解不支持正则表达式
虽然在官方全局拦截器中是可以处理的，
但这样就不够专注、不内聚。

#### 暂停或者恢复路由功能不支持
参考下列场景：
1. 深度链接打开目标页，app未启动要先打开首页再跳转目标页（先缓存，再跳转）。
2. app未启动时用户点击通知栏打开目标页，app未启动要先打开首页再跳转目标页（先缓存，再跳转）。
3. 跳转目标页面要手势密码验证，验证成功再跳转目标页。
4. 跳转页面需要登陆，登陆成功才能跳转。

#### 不支持局部降级
ARouter支持全局降级。

#### 特定路由找不到目标页，模块内部处理

#### 不支持从path中获取参数
本身是支持从query中获取参数的，但是不支持从path中获取参数，例如：weimai://test/module/tag/1111111，
想获取tag或者1111111的值需要自己处理。

#### 字段自动注入不支持可选字段
h5打开原生页面传递字段key是和ios保持统一的，但是当初定义有可能是另外一个字段，需要自己拦截处理。

#### 路由不支持公共静态方法
工具类想暴露给其他模块就比较麻烦，需要实现IProvider。
模块间启动如果参数复杂，需要大量添加key、value的代码。
如果不关心字段key，只需要传递值就好了。

#### 无法实现接口与实现类一对多关系
官方IProvider是一对一的关系。
如果想根据接口获取所有模块实现类官方不支持。

适用场景：
1. moduleLifecycle应用启动通知各个模块初始化。
2. 特定业务需要各自模块自己注册的，如模块内部注册h5交互。

### 如何解决以上问题

#### 注解添加secondarypath字段，支持多个路由，支持正则表达式


##### ARouter通过路由找到目标Activity原理

ARouter保存路由信息是在Warehouse中routes字段中，
routers是一个Map<String, RouteMeta>，
就是常说的路由表。

ARouter查找路由都是以path为key去这个map获取value，如果找不到，就会路由失败，onLost会触发。

##### RouterMeta的生成
RouteMeta是如何生成和添加进Warehouse中routes字段中的呢？

技术核心涉及到编译器注解，编译器会根据注解生成对应的类和方法。

RouteProcessor会处理@Route注解，可以获取注解中path字段和添加注解TypeElement，根据这些信息构建
RouteMeta对象。

##### RouterMeta的添加到路由表
接下来就是考虑如何将RouteMeta添加进map里。
ARouter为提升效率，增加懒加载和分组加载的概念。

```

public class ARouter$$Group$$chat implements IRouteGroup {
  @Override
 public void loadInto(Map<String, RouteMeta> atlas) {
    atlas.put("/chat/message/intimateservice", RouteMeta.build(RouteType.ACTIVITY, TransactionMessageActivity.class, "/chat/message/intimateservice", "chat", null, -1, -2147483648));
 }
}


```

只要获取ARouter$$Group$$chat调用loadInto(Warehouse.routes)，就实现了添加路由到路由表的功能。

ARouter是根据path中group来从map中获取ARouter$$Group$$chat.class然后实例化的。

ARouter$$Group$$chat的Class就是保存在Warehouse的groupsIndex中。



groupsIndex是group名字为key的Map<String, Class<? extends IRouteGroup>>。
（ARouter不能跨模块group同名的原因也在这里，会覆盖。）



ARouter初始化的时候会加载每个模块实现IRouteRoot的对象，然后调用

loadInto来添加IRouteGroup的class到groupsIndex。
这样初始化中就实现了路由IRouteGroup注册。



```

public class ARouter$$Root$$app implements IRouteRoot {
  @Override
 public void loadInto(Map<String, Class<? extends IRouteGroup>> routes) {
    routes.put("chat", ARouter$$Group$$chat.class);
 routes.put("doctor", ARouter$$Group$$doctor.class);

 }
}
```



总结来说，总的流程是这样的：

ARouter初始化 → 加载IRouteRoot → 添加IRouteGroup的class到groupsIndex。

当我们发起路由时会传递path → Warehouse中routes获取RouterMeta → 获取成功直接跳转。

获取失败 → 从path中获取group → 根据group查询groupsIndex获取IRouteGroup的class → 实例化 → 调用loadinto添加

to routes → 根据path从routes取RouterMeta → 获取成功直接跳转。





在不更改ARouter大框架如何支持配置多个路由？



@Route新增secondaryPathes字段，String数组类型

可以配置多个

RouteMeta也新增secondaryPathes字段

编译器注解生成代码如下



```

public class ARouter$$Group$$test implements IRouteGroup {
  @Override
 public void loadInto(Map<String, RouteMeta> atlas) {
atlas.put("/test/activity2", RouteMeta.build(RouteType.ACTIVITY, Test2Activity.class, "/test/activity2", "test", new java.util.HashMap<String, Integer>(){{put("key1", 8); }}, new String[]{"/test/activity2key", "test.com/test/activity2key"}, new Class[]{com.alibaba.android.ARouter.demo.module1.testactivity.privateInterceptor.TestPrivateInterceptor.class}, -1, -2147483648));
  }
}
```

将RouteMeta添加到路由表的时候会判断RouteMeta中secondaryPathes是否为空。

不为空，循环secondaryPathes中元素，如果以/开头，直接以元素为key、RouteMeta为value添加到路由表中。

否则构建DeepLinkUri放进pathMappings中。

DeepLinkUri中包含RouteMeta，会构建正则表达式。

ARouter发起路由有两种方式：一种是传递以/开头的path，另外一种是传递Uri。

path方式就是以path从路由表取RouteMeta，Uri的方式先取path，以path为key从路由表取RouteMeta。

如果取不到才会循环pathMappings中的元素，用正则表达式方式进行找到匹配的DeepLinkUri，放入优先级排序的集合中。

最后取优先级最高一个DeepLinkUri，从中取RouteMeta进行跳转。

总体流程是：构建Postcard → 查获RouteMeta（动态添加，path从路由表routes中匹配，正则从pathMappings元素匹配）→ RouteMeta中字段赋值给Postcard → 全局拦截器（greenChannel会跳过）→

私有拦截器（没有会跳过）→ 构建Intent → 跳转。

#### 从path中获取字段（query获取字段本身已经支持）

核心原理是：

一、根据定义路由地址构建正则表达式。

匹配<字段>，获取字段放入key列表中，替换<字段>为

([^/\\s@\\?#]+)。

如果无Scheme，加

\\S*

前缀，无path则在末尾追加

\\S*。

二、匹配：

用正则匹配获取Matcher，循环key列表，从Matcher.group(index+1)获取值放进

placeHolders

map集合中。

为了优化，做了缓存处理，会缓存scheme+host+path为唯一识别关键字。

不是每次都走正则匹配。



#### 字段注入支持多个可选字段



使用方式

```

@Autowired(name = "boy", required = true, alternate = {"sex"})
boolean girl;
```



会生成以添加注解的Activity或fragment名字+

$$Autowired的类，实现ISyringe接口
ARouter .inject(this);

会调用AutowiredServiceImpl的autowire方法根据类名找到class并实例化

activity$$Autowired,调用inject方法进行注入






```

public class Test4Activity$$ARouter$$Autowired implements ISyringe {
  private SerializationService serializationService;

 @Override
 public void inject(Object target) {
    serializationService = ARouter.getInstance().navigation(SerializationService.class);
 Test4Activity substitute = (Test4Activity)target;
 Bundle extras = substitute.getIntent().getExtras();
 if (null == extras) {
      return;
 }
if (ExtraUtils.containsKey(extras, "pac")) {
  substitute.pac = extras.getParcelable("pac" );
}

substitute.girl = extras.getBoolean("boy", substitute.girl);
substitute.girl = extras.getBoolean("sex", substitute.girl);

 }
}
```

会根据注解添加的每个key生成赋值代码，特殊类型的会判断是否包含key。

#### 路由到静态方法或者构造方法的实现

添加路由信息到路由表。

```

atlas.put("/test/dialog", RouteMeta.build(RouteType.METHOD, ARouter$$MethodInvoker$$modulejava.class, "/test/dialog", "test", null, null, null, -1, -2147483648));
```

一个模块的方法路由都会添加到继承了IMethodInvoker的类中，类名格式为

ARouter$$MethodInvoker$$+模块名。

 ```

public class ARouter$$MethodInvoker$$modulejava implements IMethodInvoker {
  @Override
 public Object invoke(Context context, Postcard postcard) {
    final String path = postcard.getPath();
 if("/test/dialog".equals(path)) {
      return new MyDialog(context, postcard.getExtras().getString("content"));
 } else if("/test/getintent".equals(path)) {
      return Test1Activity.getIntent(context, postcard.getExtras().getString("name"), postcard.getExtras().getInt("height"), postcard.getExtras().getLong("high"), postcard.getExtras().getBoolean("sex"), postcard.getExtras().getByte("byteFlag"), postcard.getExtras().getShort("shortFlag"), postcard.getExtras().getInt("age"), postcard.getExtras().getChar("ch"), postcard.getExtras().getFloat("fl"), postcard.getExtras().getDouble("dou"), (TestSerializable)postcard.getExtras().getSerializable("ser"), (TestParcelable)postcard.getExtras().getParcelable("pac"), postcard.getExtras().getCharSequence("charSequence"), postcard.getExtras().getByteArray("bytes"), postcard.getExtras().getCharSequenceArray("charSequenceArray"), postcard.getExtras().getCharArray("charArray"), postcard.getExtras().getShortArray("sh"), postcard.getExtras().getFloatArray("floats"), (SparseArray)postcard.getExtras().getSparseParcelableArray("sparseArrays"), postcard.getExtras().getIntegerArrayList("integerArrayList"), postcard.getExtras().getCharSequenceArrayList("charSequenceArrayList"), postcard.getExtras().getStringArrayList("stringArrayList"), (ArrayList)postcard.getExtras().getParcelableArrayList("parcelables"), (Map)postcard.getObject("map"));
 } else if("/test/getintentWithObj".equals(path)) {
      return Test1Activity.getIntent1(context, (TestSerializable)postcard.getExtras().getSerializable("ser"), (TestParcelable)postcard.getExtras().getParcelable("pac"), postcard.getExtras().getCharSequence("charSequence"), (SparseArray)postcard.getExtras().getSparseParcelableArray("sparseArrays"), postcard.getExtras().getIntegerArrayList("integerArrayList"), (ArrayList)postcard.getExtras().getParcelableArrayList("parcelables"), (Map)postcard.getObject("map"));
 } else if("/test/getmap".equals(path)) {
      return MyDialog.TestUtil.getMap();
 } else if("/test/methodInvoker".equals(path)) {
      MethodInvokerActivity.start(context, postcard.getExtras().getString("name"));
 return null;
 } else if("/test/methodInvoker1".equals(path)) {
      MethodInvokerActivity.startWithAcitonFlag((Activity)context, postcard.getExtras().getString("name"), postcard.getAction() == null ? "defaultAction" : postcard.getAction(), postcard.getFlags(), postcard.getRequestCode());
 return null;
 }
    return null;
 }
}


```

匹配方式比较简单粗暴，从RouteMeta获取ARouter$$MethodInvoker$$modulejava.class，实例化后调用invoke方法，在方法内部判断path分支。

调用构造方法或者静态方法，参数是从Postcard获取，字段默认参数名，可以用@Query改写。可以获取Uri、Action或requestCode。

因为方法路由支持全局拦截器和私有拦截器，调用目标构造方法或者有返回值的静态方法时候要注意。

#### 模版接口实现跳转不用关心具体参数key

```

public interface ITestNavigator {


    @Action("testactivity")
    @RequestCode(300)
    @TargetPath("/test/activity1")
    Postcard navigateTest(Activity activity, @RequestCode int requestCode, @Query("map") Map<String, List<TestObj>> map, Uri uri);

 @TargetPath("/test/activity2key")
    Intent navigateTest2(Activity activity,@Query("key1") String mykey);
}





public class ARouter$$ITestNavigatorImpl implements ITestNavigator {
  @Override
 public Postcard navigateTest(Activity activity, int requestCode, Map<String, List<TestObj>> map,
 Uri uri) {
    Postcard postcard = ARouter.getInstance().build("/test/activity1");
 postcard.withObject("map", map);
 postcard.withIntentData(uri);
 postcard.withAction("testactivity");
 return postcard;
 }

  @Override
 public Intent navigateTest2(Activity activity, String mykey) {
    Postcard postcard = ARouter.getInstance().build("/test/activity2key");
 postcard.withString("key1", mykey);
 postcard.setForIntent();
 return (android.content.Intent)postcard.navigation(activity, null);
 }
}
```

最终会放入接口class为key实现类为value的集合中。

```

public class ARouter$$Templates$$modulejava implements ITemplateGroup {
  @Override
 public void loadInto(Map<Class, Class> templates) {
    templates.put(ITestNavigator.ItestStaticMethod.class,ARouter$$ItestStaticMethodImpl.class);
 templates.put(ITestNavigator.class,ARouter$$ITestNavigatorImpl.class);
 }
}
```

调用方式：

```

ARouter.getInstance().navigationWithTemplate(ITestNavigator.class).navigateTest2(this,"hello world");
```

会从集合中查找ITestNavigator.class的实现类，并实例化；

#### @MultiImplement注解实现多个模块实现同一个接口

使用方式：

```

@MultiImplement(priority = 7, value = DegradeService.class)
public class TestDegrade implements DegradeService {
  public void init(Context paramContext) {}

  public boolean onLost(final Context context, Postcard paramPostcard) {
    Log.i("DegradeService", "priority = 7");
 if (paramPostcard.getPath().startsWith("/test")) {
      new Handler().postDelayed(new Runnable() {
            public void run() {
              Toast.makeText(context, "test not found", Toast.LENGTH_SHORT).show();
 }
          },4000L);
 return true;
 }
    return false;
 }
}
```

```

@MultiImplement(priority = 1, value = IModuleLifecycle.class)
public class AppModuleLifecycle implements IModuleLifecycle {
  public int getPrioriry() {
    return 1;
 }

  @Override
 public void onCreate() {

  }
}
```

调用方式：

```

List<IModuleLifecycle> list = ARouter.getInstance().getMultiImplements(IModuleLifecycle.class);

 for (IModuleLifecycle iModuleLifecycle : list) {
     iModuleLifecycle.onCreate();
 }

List<DegradeService> degradeSevices = ARouter.getInstance().getMultiImplements(DegradeService.class);
if (!CollectionUtils.isEmpty(degradeSevices)) {
    for (DegradeService degradeService : degradeSevices) {
        if (degradeService.onLost(postcard.getContext(), postcard)) {
            break;
 }
    }
}


```

所有信息保存在map集合中。

static Map<Class, PriorityList<RouteMeta>> multImplments = new HashMap<>();

```

public class ARouter$$MultiImplements$$modulejava implements IMultiImplementGroup {
  @Override
 public void loadInto(IMultiImplementRegister register) {
    register.add(DegradeService.class, RouteMeta.build(RouteType.MULTIIMPLEMENTS, ModuleDegrade.class , DegradeService.class, 100));
 register.add(DegradeService.class, RouteMeta.build(RouteType.MULTIIMPLEMENTS, AppDegrade.class , DegradeService.class, 5));
 register.add(DegradeService.class, RouteMeta.build(RouteType.MULTIIMPLEMENTS, TestDegrade.class , DegradeService.class, 7));
 register.add(IModuleLifecycle.class, RouteMeta.build(RouteType.MULTIIMPLEMENTS, AppModuleLifecycle.class , IModuleLifecycle.class, 1));
 register.add(IModuleLifecycle.class, RouteMeta.build(RouteType.MULTIIMPLEMENTS, ModuleJavaLifecycle.class , IModuleLifecycle.class, 20));
 register.add(IModuleLifecycle.class, RouteMeta.build(RouteType.MULTIIMPLEMENTS, MainModuleLifecycle.class , IModuleLifecycle.class, 4));
 }
}
```

 ```

@Route(path = "/ARouter/service/multiimplmentsregister")
public class MultiImplmentsRegister implements IMultiImplementRegister {
    @Override
 public void add(Class<?> keyClass, RouteMeta routeMeta) {
        if (!Warehouse.multImplments.containsKey(keyClass)) {
            Warehouse.multImplments.put(keyClass, new PriorityList());
 }
        ((PriorityList) Warehouse.multImplments.get(keyClass)).addItem(routeMeta, routeMeta.getPriority());
 }

    @Override
 public void init(Context context) {

    }
}

```

ARouter会缓存所有实例，保存在Map<Class, List> multImplmentsIntances。

#### 私有拦截器

私有拦截器可以使用在Activity、fragment、静态方法路由、构造方法路由。

但是带有返回值的静态方法路由或者构造方法路由使用有限制，不能在拦截器中异步操作，因为要同步返回对象。

使用方式如下，可以配置多个，多个按顺序执行：

```

@Route(path = "/test/methodInvoker", interceptors = {TestPrivateInterceptor.class})
public static void start(Context context, @Query("name") String userName) {
    Intent intent = new Intent(context, MethodInvokerActivity.class);
 intent.putExtra("name", userName);
 context.startActivity(intent);
}

```

拦截器的class会保存在RouteMeta对象中，最终传递给postcard。

在执行全局拦截器之后，跳转之前执行私有拦截器的初始化和调用操作。

```

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

```

#### 路由暂停恢复和废弃暂停路由
使用场景,校验参数合法性,登录校验,权限校验等。
可以拒绝跳转，也可以暂停跳转,等异步操作成功后，恢复跳转或者废弃暂停路由。

如何暂停路由,在私有拦截器中,调用pause

实现也比较简单，暂停路由是根据指定key缓存postcard，恢复路由跳是获取缓存的postcard，并恢复跳转,同时移除缓存的postcard。
