Situation:
```java
public class TargetBase {
    private String baseField;
    
    public void method() {
        System.out.println("Base method");
    }
}

public class TargetClass extends TargetBase {
    private String privateField;
    public final String finalField;
    
    public TargetClass(String finalField) {
        this.finalField = finalField;
    }
    
    public void method() {
        this.privateField = "Foo";
    }
    
    private void privateMethod() {
    }
}

public class TargetClassUser {
    public void method() {
        TargetClass target = new TargetClass();
        target.method();
        System.out.println(target.finalField);
    }
}

public interface IDuck {
    void quack();
}

@Mixin(TargetBase.class)
public class TargetBaseMixin {
    @Shadow
    private String baseField;
    
    protected String getBaseField() {
        return baseField;
    }
}

@Mixin(TargetClass.class)
public class SomeAnnoyingMixin extends TargetBaseMixin implements IDuck {
    @Shadow
    private String privateField;
    @Shadow
    @Final
    @Mutable
    private String finalField;
    
    private String addedField;
    
    @Override
    public void quack() {
        System.out.println(this.privateField);
        this.finalField = "Quack";
        MyPluginClass.onQuack();
        System.out.println(this.getBaseField());
        this.privateMethod();
    }
}

@Mixin(TargetClass.class)
public class AnnoyingMixin2 extends TargetBase {
    @Inject(at = @At("HEAD"), method = "method")
    private void injectMethod(CallbackInfo ci) {
        super.method();
    }
}

public class MyPluginClass {
    public static void onQuack() {
        System.out.println("Quacked!");
    }
}
```

Expected outcome:
```java
public class TargetBase {
    private String baseField;
    
    public TargetBase() {
        PulpBootstrap.registerProxy(new $TargetBase$Proxy(this));
    }

    public void method() {
        System.out.println("Base method");
    }
}

public class $TargetBase$Proxy {
    protected final TargetBase $this;
    
    public $TargetBase$Proxy(TargetBase $this) {
        this.$this = $this;
    }
    
    public String getBaseField() {
        return <invokedynamic>[PulpBootstrap.getPrivateField([VarHandle("baseField", String.class)])](this.$this);
    }
}

public class TargetClass extends TargetBase {
    private String privateField;
    public final String finalField;

    public TargetClass(String finalField) {
        PulpBootstrap.registerProxy(new $TargetClass$Proxy(this));
        this.finalField = finalField;
    }

    public void method() {
        (($TargetClass$Proxy) PulpBootstrap.getProxy(this)).handler$injectMethod$zzz(new CallbackInfo());
        this.privateField = "Foo";
    }

    private void privateMethod() {
    }
}

public class $TargetClass$Proxy extends $TargetBase$Proxy {
    public $TargetClass$Proxy(TargetClass $this) {
        super($this);
    }
    
    public void handler$injectMethod$zzz(CallbackInfo ci) {
        <invokedynamic>[PulpBootstrap.invokeExact("method", )](this.$this);
    }
}
```
