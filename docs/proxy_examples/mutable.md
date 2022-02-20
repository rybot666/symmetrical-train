# `@Mutable`

## Input
```java
public class Target {
    private final int field;

    public Target(int value) {
        this.field = value;
    }

    public void printField() {
        System.out.println(this.field);
    }
}

@Mixin(Target.class)
public class ExampleMixin {
    @Shadow
    @Mutable
    private int field;
    
    @Inject(
        target = "printField",
        at = @At("HEAD")
    )
    private void inject(CallbackInfo ci) {
        this.field += 1;
    }
}

public class Main {
    public static void main(String[] args) {
        Target target = new Target(9);

        // expected output: 10
        target.printField();
        
        // expected output: 11
        target.printField();
    }
}
```

## Expected Generated Code
```java=
public class Target {
    private final int field;
    
    public Target(int value) {
        <invokedynamic>[PulpBootstrap.privateSetField([VarHandle($Target$Proxy.class, "field$definal", int.class)])](PulpBootstrap.getProxy(this), value)
    }
    
    public void printField() {
        (($Target$Proxy) PulpBootstrap.getProxy(this)).handler$inject$zzz(/* generate the callbackinfo */);

        System.out.println(<invokedynamic>[PulpBootstrap.privateGetField([VarHandle($Target$Proxy.class, "field$definal", int.class)])](PulpBootstrap.getProxy(this)));
    }
}

public class $Target$Proxy {
    private final Target $this;
    private int field$definal;
    
    public $Target$Proxy(Target $this) {
        this.$this = $this;
        
        // Copy the field from the instance into the definalised copy
        this.field$definal = <invokedynamic>[PulpBootstrap.privateGetField([VarHandle($Target$Proxy.class, "field", int.class)])](this.$this);
    }
    
    private handler$inject$zzz(CallbackInfo ci) {
        this.field$definal += 1;
    }
}

public class Main {
    public static void main(String[] args) {
        Target target = new Target(9);

        // expected output: 10
        target.printField();
        
        // expected output: 11
        target.printField();
    }
}
```
