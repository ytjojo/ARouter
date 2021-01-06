```
    一个用于帮助 Android App 进行组件化改造的框架 —— 支持模块间的路由、通信、解耦
```

[English](https://github.com/alibaba/ARouter/blob/master/README.md)

##### [![Join the chat at https://gitter.im/alibaba/ARouter](https://badges.gitter.im/alibaba/ARouter.svg)](https://gitter.im/alibaba/ARouter?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

#### 最新版本

模块|arouter-api|arouter-compiler|arouter-register|arouter-idea-plugin
---|---|---|---|---
最新版本|[![Download](https://api.bintray.com/packages/zhi1ong/maven/arouter-api/images/download.svg)](https://bintray.com/zhi1ong/maven/arouter-api/_latestVersion)|[![Download](https://api.bintray.com/packages/zhi1ong/maven/arouter-compiler/images/download.svg)](https://bintray.com/zhi1ong/maven/arouter-compiler/_latestVersion)|[![Download](https://api.bintray.com/packages/zhi1ong/maven/arouter-register/images/download.svg)](https://bintray.com/zhi1ong/maven/arouter-register/_latestVersion)|[![as plugin](https://img.shields.io/jetbrains/plugin/d/11428-arouter-helper.svg)](https://plugins.jetbrains.com/plugin/11428-arouter-helper)

#### Demo展示

##### [Demo apk下载](https://github.com/alibaba/ARouter/blob/develop/demo/arouter-demo-1.5.2.apk)、[Demo Gif](https://raw.githubusercontent.com/alibaba/ARouter/master/demo/arouter-demo.gif)

#### 一、功能介绍
1. **支持直接解析标准URL进行跳转，并自动注入参数到目标页面中**
2. **支持多模块工程使用**
3. **支持添加多个拦截器，自定义拦截顺序**
4. **支持依赖注入，可单独作为依赖注入框架使用**
5. **支持InstantRun**
6. **支持MultiDex**(Google方案)
7. 映射关系按组分类、多级管理，按需初始化
8. 支持用户指定全局降级与局部降级策略
9. 页面、拦截器、服务等组件均自动注册到框架
10. 支持多种方式配置转场动画
11. 支持获取Fragment
12. 完全支持Kotlin以及混编(配置见文末 其他#5)
13. **支持第三方 App 加固**(使用 arouter-register 实现自动注册)
14. **支持生成路由文档**
15. **提供 IDE 插件便捷的关联路径和目标类**
16. 支持增量编译(开启文档生成后无法增量编译)
17. 支持动态注册路由信息

#### 二、典型应用
1. 从外部URL映射到内部页面，以及参数传递与解析
2. 跨模块页面跳转，模块间解耦
3. 拦截跳转过程，处理登陆、埋点等逻辑
4. 跨模块API调用，通过控制反转来做组件解耦

#### 三、基础功能
1. 添加依赖和配置
    ``` gradle
    android {
        defaultConfig {
            ...
            javaCompileOptions {
                annotationProcessorOptions {
                    arguments = [AROUTER_MODULE_NAME: project.getName()]
                }
            }
        }
    }

    dependencies {
        // 替换成最新版本, 需要注意的是api
        // 要与compiler匹配使用，均使用最新版可以保证兼容
        compile 'com.alibaba:arouter-api:x.x.x'
        annotationProcessor 'com.alibaba:arouter-compiler:x.x.x'
        ...
    }
    // 旧版本gradle插件(< 2.2)，可以使用apt插件，配置方法见文末'其他#4'
    // Kotlin配置参考文末'其他#5'
    ```

2. 添加注解
    ``` java
    // 在支持路由的页面上添加注解(必选)
    // 这里的路径需要注意的是至少需要有两级，/xx/xx
    @Route(path = "/test/activity")
    public class YourActivity extend Activity {
        ...
    }
    ```

3. 初始化SDK
    ``` java
    if (isDebug()) {           // 这两行必须写在init之前，否则这些配置在init过程中将无效
        ARouter.openLog();     // 打印日志
        ARouter.openDebug();   // 开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！线上版本需要关闭,否则有安全风险)
    }
    ARouter.init(mApplication); // 尽可能早，推荐在Application中初始化
    ```

4. 发起路由操作
    ``` java
    // 1. 应用内简单的跳转(通过URL跳转在'进阶用法'中)
    ARouter.getInstance().build("/test/activity").navigation();

    // 2. 跳转并携带参数
    ARouter.getInstance().build("/test/1")
                .withLong("key1", 666L)
                .withString("key3", "888")
                .withObject("key4", new Test("Jack", "Rose"))
                .navigation();
    ```

5. 添加混淆规则(如果使用了Proguard)
    ``` 
    -keep public class com.alibaba.android.arouter.routes.**{*;}
    -keep public class com.alibaba.android.arouter.facade.**{*;}
    -keep class * implements com.alibaba.android.arouter.facade.template.ISyringe{*;}

    # 如果使用了 byType 的方式获取 Service，需添加下面规则，保护接口
    -keep interface * implements com.alibaba.android.arouter.facade.template.IProvider

    # 如果使用了 单类注入，即不定义接口实现 IProvider，需添加下面规则，保护实现
    # -keep class * implements com.alibaba.android.arouter.facade.template.IProvider
    ```

6. 使用 Gradle 插件实现路由表的自动加载 (可选)
    ```gradle
    apply plugin: 'com.alibaba.arouter'

    buildscript {
        repositories {
            jcenter()
        }

        dependencies {
            classpath "com.alibaba:arouter-register:?"
        }
    }
    ```

    可选使用，通过 ARouter 提供的注册插件进行路由表的自动加载(power by [AutoRegister](https://github.com/luckybilly/AutoRegister))， 默认通过扫描 dex 的方式
    进行加载通过 gradle 插件进行自动注册可以缩短初始化时间解决应用加固导致无法直接访问
    dex 文件，初始化失败的问题，需要注意的是，该插件必须搭配 api 1.3.0 以上版本使用！

7. 使用 IDE 插件导航到目标类 (可选)

    在 Android Studio 插件市场中搜索 `ARouter Helper`, 或者直接下载文档上方 `最新版本` 中列出的 `arouter-idea-plugin` zip 安装包手动安装，安装后
    插件无任何设置，可以在跳转代码的行首找到一个图标 (![navigation](https://raw.githubusercontent.com/alibaba/ARouter/develop/arouter-idea-plugin/src/main/resources/icon/outline_my_location_black_18dp.png))
    点击该图标，即可跳转到标识了代码中路径的目标类

#### 四、进阶用法
1. 通过URL跳转
    ``` java
    // 新建一个Activity用于监听Scheme事件,之后直接把url传递给ARouter即可
    public class SchemeFilterActivity extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        ARouter.getInstance().build(uri).navigation();
        finish();
        }
    }
    ```

    AndroidManifest.xml
    ``` xml
    <activity android:name=".activity.SchemeFilterActivity">
        <!-- Scheme -->
        <intent-filter>
            <data
            android:host="m.aliyun.com"
            android:scheme="arouter"/>

            <action android:name="android.intent.action.VIEW"/>

            <category android:name="android.intent.category.DEFAULT"/>
            <category android:name="android.intent.category.BROWSABLE"/>
        </intent-filter>
    </activity>
    ```

2. 解析参数
    ``` java
    // 为每一个参数声明一个字段，并使用 @Autowired 标注
    // URL中不能传递Parcelable类型数据，通过ARouter api可以传递Parcelable对象
    @Route(path = "/test/activity")
    public class Test1Activity extends Activity {
        @Autowired
        public String name;
        @Autowired
        int age;
        
        // 通过name来映射URL中的不同参数
        //  alternate = {"sex"} 配置可代替参数 传递sex 也会赋值给boy字段
        // 可以配置多个代替参数
        @Autowired(name = "girl" ,alternate = {"sex"}) 
        boolean boy;
        
        // 支持解析自定义对象，URL中使用json传递
        @Autowired
        TestObj obj;      
        
        // 使用 withObject 传递 List 和 Map 的实现了
        // Serializable 接口的实现类(ArrayList/HashMap)
        // 的时候，接收该对象的地方不能标注具体的实现类类型
        // 应仅标注为 List 或 Map，否则会影响序列化中类型
        // 的判断, 其他类似情况需要同样处理        
        @Autowired
        List<TestObj> list;
        @Autowired
        Map<String, List<TestObj>> map;
        
        @Override
        protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ARouter.getInstance().inject(this);

        // ARouter会自动对字段进行赋值，无需主动获取
        Log.d("param", name + age + boy);
        }
    }


    // 如果需要传递自定义对象，新建一个类（并非自定义对象类），然后实现 SerializationService,并使用@Route注解标注(方便用户自行选择序列化方式)，例如：
    @Route(path = "/yourservicegroupname/json")
    public class JsonServiceImpl implements SerializationService {
        @Override
        public void init(Context context) {

        }

        @Override
        public <T> T json2Object(String text, Class<T> clazz) {
            return JSON.parseObject(text, clazz);
        }

        @Override
        public String object2Json(Object instance) {
            return JSON.toJSONString(instance);
        }
    }
    ```

3. 声明拦截器(拦截跳转过程，面向切面编程)
    ``` java
    // 比较经典的应用就是在跳转过程中处理登陆事件，这样就不需要在目标页重复做登陆检查
    // 拦截器会在跳转之间执行，多个拦截器会按优先级顺序依次执行
    @Interceptor(priority = 8, name = "测试用拦截器")
    public class TestInterceptor implements IInterceptor {
        @Override
        public void process(Postcard postcard, InterceptorCallback callback) {
        ...
        callback.onContinue(postcard);  // 处理完成，交还控制权
        // callback.onInterrupt(new RuntimeException("我觉得有点异常"));      // 觉得有问题，中断路由流程

        // 以上两种至少需要调用其中一种，否则不会继续路由
        }

        @Override
        public void init(Context context) {
        // 拦截器的初始化，会在sdk初始化的时候调用该方法，仅会调用一次
        }
    }
    ```

4. 处理跳转结果
    ``` java
    // 使用两个参数的navigation方法，可以获取单次跳转的结果
    ARouter.getInstance().build("/test/1").navigation(this, new NavigationCallback() {
        @Override
        public void onFound(Postcard postcard) {
        ...
        }

        @Override
        public void onLost(Postcard postcard) {
        ...
        }
    });
    ```

5. 自定义全局降级策略
    ``` java
    // 实现DegradeService接口，并加上一个Path内容任意的注解即可
    @Route(path = "/xxx/xxx")
    public class DegradeServiceImpl implements DegradeService {
    @Override
    public void onLost(Context context, Postcard postcard) {
        // do something.
    }

    @Override
    public void init(Context context) {

    }
    }
    ```

6. 为目标页面声明更多信息
    ``` java
    // 我们经常需要在目标页面中配置一些属性，比方说"是否需要登陆"之类的
    // 可以通过 Route 注解中的 extras 属性进行扩展，这个属性是一个 int值，换句话说，单个int有4字节，也就是32位，可以配置32个开关
    // 剩下的可以自行发挥，通过字节操作可以标识32个开关，通过开关标记目标页面的一些属性，在拦截器中可以拿到这个标记进行业务逻辑判断
    @Route(path = "/test/activity", extras = Consts.XXXX)
    ```

7. 通过依赖注入解耦:服务管理(一) 暴露服务
    ``` java
    // 声明接口,其他组件通过接口来调用服务
    public interface HelloService extends IProvider {
        String sayHello(String name);
    }

    // 实现接口
    @Route(path = "/yourservicegroupname/hello", name = "测试服务")
    public class HelloServiceImpl implements HelloService {

        @Override
        public String sayHello(String name) {
        return "hello, " + name;
        }

        @Override
        public void init(Context context) {

        }
    }
    ```

8. 通过依赖注入解耦:服务管理(二) 发现服务
    ``` java
    public class Test {
        @Autowired
        HelloService helloService;

        @Autowired(name = "/yourservicegroupname/hello")
        HelloService helloService2;

        HelloService helloService3;

        HelloService helloService4;

        public Test() {
        ARouter.getInstance().inject(this);
        }

        public void testService() {
        // 1. (推荐)使用依赖注入的方式发现服务,通过注解标注字段,即可使用，无需主动获取
        // Autowired注解中标注name之后，将会使用byName的方式注入对应的字段，不设置name属性，会默认使用byType的方式发现服务(当同一接口有多个实现的时候，必须使用byName的方式发现服务)
        helloService.sayHello("Vergil");
        helloService2.sayHello("Vergil");

        // 2. 使用依赖查找的方式发现服务，主动去发现服务并使用，下面两种方式分别是byName和byType
        helloService3 = ARouter.getInstance().navigation(HelloService.class);
        helloService4 = (HelloService) ARouter.getInstance().build("/yourservicegroupname/hello").navigation();
        helloService3.sayHello("Vergil");
        helloService4.sayHello("Vergil");
        }
    }
    ```

9. 预处理服务
    ``` java
    // 实现 PretreatmentService 接口，并加上一个Path内容任意的注解即可
    @Route(path = "/xxx/xxx")
    public class PretreatmentServiceImpl implements PretreatmentService {
        @Override
        public boolean onPretreatment(Context context, Postcard postcard) {
            // 跳转前预处理，如果需要自行处理跳转，该方法返回 false 即可
        }

        @Override
        public void init(Context context) {
    
        }
    }
    ```

10. 动态注册路由信息
适用于部分插件化架构的App以及需要动态注册路由信息的场景，可以通过 ARouter 提供的接口实现动态注册
路由信息，目标页面和服务可以不标注 @Route 注解，**注意：同一批次仅允许相同 group 的路由信息注册**
    ``` java
        ARouter.getInstance().addRouteGroup(new IRouteGroup() {
            @Override
            public void loadInto(Map<String, RouteMeta> atlas) {
                atlas.put("/dynamic/activity",      // path
                    RouteMeta.build(
                        RouteType.ACTIVITY,         // 路由信息
                        TestDynamicActivity.class,  // 目标的 Class
                        "/dynamic/activity",        // Path
                        "dynamic",                  // Group, 尽量保持和 path 的第一段相同
                        0,                          // 优先级，暂未使用
                        0                           // Extra，用于给页面打标
                    )
                );
            }
        });
    ```
 
#### 五、更多功能

1. 初始化中的其他设置
    ``` java
    ARouter.openLog(); // 开启日志
    ARouter.openDebug(); // 使用InstantRun的时候，需要打开该开关，上线之后关闭，否则有安全风险
    ARouter.printStackTrace(); // 打印日志的时候打印线程堆栈
    ```

2. 详细的API说明
    ``` java
    // 构建标准的路由请求
    ARouter.getInstance().build("/home/main").navigation();

    // 构建标准的路由请求，并指定分组
    ARouter.getInstance().build("/home/main", "ap").navigation();

    // 构建标准的路由请求，通过Uri直接解析
    Uri uri;
    ARouter.getInstance().build(uri).navigation();

    // 构建标准的路由请求，startActivityForResult
    // navigation的第一个参数必须是Activity，第二个参数则是RequestCode
    ARouter.getInstance().build("/home/main", "ap").navigation(this, 5);

    // 直接传递Bundle
    Bundle params = new Bundle();
    ARouter.getInstance()
        .build("/home/main")
        .with(params)
        .navigation();

    // 指定Flag
    ARouter.getInstance()
        .build("/home/main")
        .withFlags();
        .navigation();

    // 获取Fragment
    Fragment fragment = (Fragment) ARouter.getInstance().build("/test/fragment").navigation();
                        
    // 对象传递
    ARouter.getInstance()
        .withObject("key", new TestObj("Jack", "Rose"))
        .navigation();

    // 觉得接口不够多，可以直接拿出Bundle赋值
    ARouter.getInstance()
            .build("/home/main")
            .getExtra();

    // 转场动画(常规方式)
    ARouter.getInstance()
        .build("/test/activity2")
        .withTransition(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
        .navigation(this);

    // 转场动画(API16+)
    ActivityOptionsCompat compat = ActivityOptionsCompat.
        makeScaleUpAnimation(v, v.getWidth() / 2, v.getHeight() / 2, 0, 0);

    // ps. makeSceneTransitionAnimation 使用共享元素的时候，需要在navigation方法中传入当前Activity

    ARouter.getInstance()
        .build("/test/activity2")
        .withOptionsCompat(compat)
        .navigation();
            
    // 使用绿色通道(跳过所有的拦截器)
    ARouter.getInstance().build("/home/main").greenChannel().navigation();

    // 使用自己的日志工具打印日志
    ARouter.setLogger();

    // 使用自己提供的线程池
    ARouter.setExecutor();
    ```

3. 获取原始的URI
    ``` java
    String uriStr = getIntent().getStringExtra(ARouter.RAW_URI);
    ```

4. 重写跳转URL
    ``` java
    // 实现PathReplaceService接口，并加上一个Path内容任意的注解即可
    @Route(path = "/xxx/xxx") // 必须标明注解
    public class PathReplaceServiceImpl implements PathReplaceService {
        /**
        * For normal path.
        *
        * @param path raw path
        */
        String forString(String path) {
        return path;    // 按照一定的规则处理之后返回处理后的结果
        }

    /**
        * For uri type.
        *
        * @param uri raw uri
        */
    Uri forUri(Uri uri) {
        return url;    // 按照一定的规则处理之后返回处理后的结果
    }
    }
    ```

5. 生成路由文档
    ``` gradle
    // 更新 build.gradle, 添加参数 AROUTER_GENERATE_DOC = enable
    // 生成的文档路径 : build/generated/source/apt/(debug or release)/com/alibaba/android/arouter/docs/arouter-map-of-${moduleName}.json
    android {
        defaultConfig {
            ...
            javaCompileOptions {
                annotationProcessorOptions {
                    arguments = [AROUTER_MODULE_NAME: project.getName(), AROUTER_GENERATE_DOC: "enable"]
                }
            }
        }
    }
    ```

#### 六、其他

1. 路由中的分组概念

	- SDK中针对所有的路径(/test/1 /test/2)进行分组，分组只有在分组中的某一个路径第一次被访问的时候，该分组才会被初始化
	- 可以通过 @Route 注解主动指定分组，否则使用路径中第一段字符串(/*/)作为分组
	- 注意：一旦主动指定分组之后，应用内路由需要使用 ARouter.getInstance().build(path, group) 进行跳转，手动指定分组，否则无法找到
    ``` java
    @Route(path = "/test/1", group = "app")
    ```

2. 拦截器和服务的异同

	- 拦截器和服务所需要实现的接口不同，但是结构类似，都存在 init(Context context) 方法，但是两者的调用时机不同
	- 拦截器因为其特殊性，会被任何一次路由所触发，拦截器会在ARouter初始化的时候异步初始化，如果第一次路由的时候拦截器还没有初始化结束，路由会等待，直到初始化完成。
	- 服务没有该限制，某一服务可能在App整个生命周期中都不会用到，所以服务只有被调用的时候才会触发初始化操作

3. 旧版本gradle插件的配置方式
    ``` gradle
    apply plugin: 'com.neenbedankt.android-apt'

    buildscript {
        repositories {
        jcenter()
        }

        dependencies {
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.4'
        }
    }

    apt {
        arguments {
        AROUTER_MODULE_NAME project.getName();
        }
    }

    dependencies {
        compile 'com.alibaba:arouter-api:x.x.x'
        apt 'com.alibaba:arouter-compiler:x.x.x'
        ...
    }
    ```

4. Kotlin项目中的配置方式
    ```
    // 可以参考 module-kotlin 模块中的写法
    apply plugin: 'kotlin-kapt'

    kapt {
        arguments {
            arg("AROUTER_MODULE_NAME", project.getName())
        }
    }

    dependencies {
        compile 'com.alibaba:arouter-api:x.x.x'
        kapt 'com.alibaba:arouter-compiler:x.x.x'
        ...
    }
    ```

#### 七、Q&A

1. "W/ARouter::: ARouter::No postcard![ ]"

    这个Log正常的情况下也会打印出来，如果您的代码中没有实现DegradeService和PathReplaceService的话，因为ARouter本身的一些功能也依赖
    自己提供的Service管理功能，ARouter在跳转的时候会尝试寻找用户实现的PathReplaceService，用于对路径进行重写(可选功能)，所以如果您没有
    实现这个服务的话，也会抛出这个日志

    推荐在app中实现DegradeService、PathReplaceService

2. "W/ARouter::: ARouter::There is no route match the path [/xxx/xxx], in group [xxx][ ]"

    - 通常来说这种情况是没有找到目标页面，目标不存在
    - 如果这个页面是存在的，那么您可以按照下面的步骤进行排查
        1. 检查目标页面的注解是否配置正确，正确的注解形式应该是 (@Route(path="/test/test"), 如没有特殊需求，请勿指定group字段，废弃功能)
        2. 检查目标页面所在的模块的gradle脚本中是否依赖了 arouter-compiler sdk (需要注意的是，要使用apt依赖，而不是compile关键字依赖)
        3. 检查编译打包日志，是否出现了形如 ARouter::Compiler >>> xxxxx 的日志，日志中会打印出发现的路由目标
        4. 启动App的时候，开启debug、log(openDebug/openLog), 查看映射表是否已经被扫描出来，形如 D/ARouter::: LogisticsCenter has already been loaded, GroupIndex[4]，GroupIndex > 0

3. 开启InstantRun之后无法跳转(高版本Gradle插件下无法跳转)？
        
     因为开启InstantRun之后，很多类文件不会放在原本的dex中，需要单独去加载，ARouter默认不会去加载这些文件，因为安全原因，只有在开启了openDebug之后
     ARouter才回去加载InstantRun产生的文件，所以在以上的情况下，需要在init**之前**调用openDebug
 
4. TransformException:java.util.zip.ZipException: duplicate entry ....
 
     ARouter有按组加载的机制，关于分组可以参考 6-1 部分，ARouter允许一个module中存在多个分组，但是不允许多个module中存在相同的分组，会导致映射文件冲突

5. Kotlin类中的字段无法注入如何解决？
    
    首先，Kotlin中的字段是可以自动注入的，但是注入代码为了减少反射，使用的字段赋值的方式来注入的，Kotlin默认会生成set/get方法，并把属性设置为private
    所以只要保证Kotlin中字段可见性不是private即可，简单解决可以在字段上添加 @JvmField 

6. 通过URL跳转之后，在intent中拿不到参数如何解决？
    
    需要注意的是，如果不使用自动注入，那么可以不写 `ARouter.getInstance().inject(this)`，但是需要取值的字段仍然需要标上 `@Autowired` 注解，因为
    只有标上注解之后，ARouter才能知道以哪一种数据类型提取URL中的参数并放入Intent中，这样您才能在intent中获取到对应的参数
    
7. 新增页面之后，无法跳转？
    
    ARouter加载Dex中的映射文件会有一定耗时，所以ARouter会缓存映射文件，直到新版本升级(版本号或者versionCode变化)，而如果是开发版本(ARouter.openDebug())，
    ARouter 每次启动都会重新加载映射文件，开发阶段一定要打开 Debug 功能

#### 八、其他

1. 沟通和交流

    1. 钉钉交流群1
    
        ![qq](https://raw.githubusercontent.com/alibaba/ARouter/master/demo/dingding-group-1.png)

    2. QQ 交流群1
    
        ![qq](https://raw.githubusercontent.com/alibaba/ARouter/master/demo/qq-group-1.png)

    3. QQ 交流群2
        
        ![qq](https://raw.githubusercontent.com/alibaba/ARouter/master/demo/qq-group-2.png)
        
#### 九、新增功能

新增功能原则：
注解能解决问题的就不要用拦截器处理
能在局部处理就不要扩散到全局

1. 新增alternate字段，自动装载注解支持多个代替参数，
    参考Gson的@SerializedName注解alternate字段

    ```
    // 通过name来映射URL中的不同参数
    //  alternate = {"sex"} 配置可代替参数 传递sex 也会赋值给boy字段
    // 可以配置多个代替参数
    @Autowired(name = "girl" ,alternate = {"sex"}) 
    boolean boy;
    ```
2. 路由支持secondarypath，可以配置多个，支持优先级配置，值越大优先级越高

secondarypath可以配置一下四种
+ 仅有scheme 实际场景仅适用打开web链接 必须是Uri方式创建路由
+ 仅有path 比较常见 Uri和path创建路由都可以唤起
+ host+path 必须是Uri方式创建路由
+ scheme+host+path 比较严苛 必须是Uri方式创建路由

```
@Route(path = "/test/activity2",secondaryPathes = {"/test/activity2key"})
public class Test2Activity extends AppCompatActivity {
```

 ARouter.getInstance().build("/test/activity2key").navigation();
 也能打开Test2Activity
3. secondarypath支持正则表达式匹配，并从path获取字段值，从path中获取字段，需要用<>来包括字段key，还支持带scheme完整路径匹配
部分路径模糊匹配


```
@Route(path = "/test/activity4", priority = 100, secondaryPathes = {"/test/home/pro<name>/<extra>/<id>"})
public class Test4Activity extends AppCompatActivity {
```
可以从intent中获取name extra id三个字段。
4. secondarypath支持正则表达式将所有http和https开头的链接交给WebviewActvity,支持正则表达式优先级，值越大优先级越高

```
@Route(path = "/test/globlewebview", priority = 1, secondaryPathes = {"https://", "http://"})
public class WebViewActivity extends AppCompatActivity {
```
ARouter.getInstance().build(Uri.parse("http://m.aliyun.com/test/home/projack/mydata/12232322/")).navigation(this);
此链接会匹配WebviewActivity和Test4Activity，，只会打开Test4Activity，因为Test4Activity优先级高

5. 路由支持配置多个私有拦截器，多个按顺序执行
拦截器中可以暂停路由，在异步获取结果后再决定是否跳转
在私有拦截器可以替换参数，
判断条件等处理

```
@Route(path = "/test/activity1", interceptors = {TestPrivateInterceptor.class}, name = "测试用 Activity", secondaryPathes = {"/test/activity1secondary", "/test/activity1secondary2"})
public class Test1Activity extends BaseActivity {

```
6. 暂停 恢复 移除暂停 路由
常用于异步场景，比如需要登陆
需要手势密码验证
跳转动态从服务器获取状态来确定是否进行跳转
暂停路由可以在Arouter.build()后直接调用
可以在公共拦截器中处理
也可以私有拦截器中处理
在拦截器中需要异步处理的话，需要同步调用暂停或终止，根据异步结果再确定调用暂停后恢复，或暂停后移除
//暂停
postcard.pause("test1");
//终止
postcard.interrupt(throwable);
//暂停后恢复
postcard.resumePausePostCard(throwable);
//暂停后移除
//例如如果从服务器获取结果是不允许跳转，就直接移除暂停的路由。整个路由生命周期结束
ARouter.getInstance().removePause("test1");
6. 路由支持静态公共方法,和公共构造方法，可以有返回值
参数除Postcard可以提供的之外，还支持Postcard ，Context ，NavigationCallback，Uri，通过注解标记的IntentFlag，Action,RequestCode;

静态方法但是有返回值的时候一定要greenChannel()，不然拦截处理走异步，会提前返回null

```

   @Route(path = "/test/methodInvoker1", interceptors = {Test1Interceptor.class})
    public static void startWithAcitonFlag(Activity context, @Query("name") String userName, @Action("") String action, @Flags int flag, @RequestCode int requestCode) {
        Intent intent = new Intent(context, MethodInvokerActivity.class);
        intent.putExtra("name", userName);
        intent.setAction(action);
        if (flag != 0) {
            intent.setFlags(flag);
        }
        context.startActivityForResult(intent, requestCode);
    }
```
7 支持接口方式启动路由，这样参数就很明确，可以下沉到基础库

```
public interface ITestNavigator {


    @Action("testactivity")
    @RequestCode(300)
    @TargetPath("/test/activity1")
    Postcard navigateTest(Activity activity, @RequestCode int requestCode, @Query("map") Map<String, List<TestObj>> map, Uri uri);

    @TargetPath("/test/activity2key")
    Intent navigateTest2(Activity activity,@Query("key1") String mykey);

    @Action("testactivity")
    @RequestCode(300)
    @TargetPath("/test/activity1")
    void navigateTest(Activity activity, @RequestCode int requestCode, @Query("age") int age,@Query("name") String userName, int height, boolean boy, NavCallback navCallback);




    @Action("testactivity")
    @TargetPath("/test/activity1")
    void navigateTest(Activity activity, String name, Integer height, long high,@Query("boy") Boolean girl, Byte byteFlag, short shortFlag, @Query("age") int age, char ch, float fl, double dou, TestSerializable ser, TestParcelable pac, CharSequence charSequence, byte[] bytes, CharSequence[] charSequenceArray, char[] charArray, short[] sh, float[] floats, SparseArray<TestParcelable> sparseArrays, ArrayList<Integer> integerArrayList, ArrayList<CharSequence> charSequenceArrayList, ArrayList<String> shortArray, ArrayList<TestParcelable> parcelables, Map<String, List<TestObj>> map);

    public interface ItestStaticMethod{
        @TargetPath("/test/getmap")
        Map<String, List<TestObj>> getMap();
    }
}

```

会生成ARouter代码

```

/**
 * DO NOT EDIT THIS FILE!!! IT WAS GENERATED BY AROUTER. */
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

  @Override
  public void navigateTest(Activity activity, int requestCode, int age, String userName, int height,
      boolean boy, NavCallback navCallback) {
    Postcard postcard = ARouter.getInstance().build("/test/activity1");
    postcard.withInt("age", age);
    postcard.withString("name", userName);
    postcard.withInt("height", height);
    postcard.withBoolean("boy", boy);
    postcard.withAction("testactivity");
    postcard.navigation(activity, requestCode, navCallback);
  }

  @Override
  public void navigateTest(Activity activity, String name, Integer height, long high, Boolean girl,
      Byte byteFlag, short shortFlag, int age, char ch, float fl, double dou, TestSerializable ser,
      TestParcelable pac, CharSequence charSequence, byte[] bytes, CharSequence[] charSequenceArray,
      char[] charArray, short[] sh, float[] floats, SparseArray<TestParcelable> sparseArrays,
      ArrayList<Integer> integerArrayList, ArrayList<CharSequence> charSequenceArrayList,
      ArrayList<String> shortArray, ArrayList<TestParcelable> parcelables,
      Map<String, List<TestObj>> map) {
    Postcard postcard = ARouter.getInstance().build("/test/activity1");
    postcard.withString("name", name);
    postcard.withInt("height", height);
    postcard.withLong("high", high);
    postcard.withBoolean("boy", girl);
    postcard.withByte("byteFlag", byteFlag);
    postcard.withShort("shortFlag", shortFlag);
    postcard.withInt("age", age);
    postcard.withChar("ch", ch);
    postcard.withFloat("fl", fl);
    postcard.withDouble("dou", dou);
    postcard.withSerializable("ser", ser);
    postcard.withParcelable("pac", pac);
    postcard.withCharSequence("charSequence", charSequence);
    postcard.withByteArray("bytes", bytes);
    postcard.withCharSequenceArray("charSequenceArray", charSequenceArray);
    postcard.withCharArray("charArray", charArray);
    postcard.withShortArray("sh", sh);
    postcard.withFloatArray("floats", floats);
    postcard.withSparseParcelableArray("sparseArrays", sparseArrays);
    postcard.withIntegerArrayList("integerArrayList", integerArrayList);
    postcard.withCharSequenceArrayList("charSequenceArrayList", charSequenceArrayList);
    postcard.withStringArrayList("shortArray", shortArray);
    postcard.withParcelableArrayList("parcelables", parcelables);
    postcard.withObject("map", map);
    postcard.withAction("testactivity");
    postcard.navigation(activity, null);
  }
}
```

调用方式


```
 Intent intent = ARouter.getInstance().navigationWithTemplate(ITestNavigator.class).navigateTest2(this,"hello world");
                startActivity(intent);

```

8. 支持根据接口获取所有实现类

用途，模块化后，各个模块生命周期，各个模块处理路由降级处理

支持优先级 值越大优先级越高

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

如何调用

```
 //列表顺序是按优先级由高到低排列的
 List<IModuleLifecycle> list = ARouter.getInstance().getMultiImplements(IModuleLifecycle.class);
 for (IModuleLifecycle iModuleLifecycle : list) {
      iModuleLifecycle.onCreate();
 }

```
9 关于Intent 字段兼容
如果是Uri构建路由方式（一般是深度链接打开app），转交给ARouter，目标页字段有@Autowired注解，ARouter会自动转换类型
例如

```

 @Autowired
    String name;

    @Autowired
    int age;

    @Autowired(name = "boy")
    boolean girl;

```
ARouter.getInstance().build(Uri.parse("arouter://m.aliyun.com/test/activity3?name=alex&age=18&boy=true&high=180")).navigation(this);
name age girl字段都能正确获取数据值，
如果链接中拼接参数boy=1标示true boy=0标示false，
需要自定义IPrivateInterceptor，在拦截器中转换处理，同时@Router配置interceptors字段





